/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.common.backup;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.sender.DummySender;
import me.lucko.luckperms.common.sender.Sender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Handles import operations
 */
public class LegacyImporter implements Runnable {

    private final CommandManager commandManager;
    private final Set<Sender> notify;
    private final List<String> commandList;
    private final List<ImportCommand> commands;

    public LegacyImporter(CommandManager commandManager, Sender executor, List<String> commands) {
        this.commandManager = commandManager;

        if (executor.isConsole()) {
            this.notify = ImmutableSet.of(executor);
        } else {
            this.notify = ImmutableSet.of(executor, commandManager.getPlugin().getConsoleSender());
        }
        this.commandList = commands.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> !s.startsWith("#"))
                .filter(s -> !s.startsWith("//"))
                .map(s -> s.startsWith("/luckperms ") ? s.substring("/luckperms ".length()) : s)
                .map(s -> s.startsWith("/lp ") ? s.substring("/lp ".length()) : s)
                .collect(Collectors.toList());
        this.commands = new ArrayList<>();
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        this.notify.forEach(s -> Message.IMPORT_START.send(s));

        // start an update task in the background - we'll #join this later
        CompletableFuture<Void> updateTask = CompletableFuture.runAsync(() -> this.commandManager.getPlugin().getSyncTaskBuffer().requestDirectly());

        this.notify.forEach(s -> Message.IMPORT_INFO.send(s, "Processing commands..."));

        // form instances for all commands, and register them
        int index = 1;
        for (String command : this.commandList) {
            ImportCommand cmd = new ImportCommand(this.commandManager, index, command);
            this.commands.add(cmd);

            if (cmd.getCommand().startsWith("creategroup ") || cmd.getCommand().startsWith("createtrack ")) {
                cmd.process(); // process immediately
            }

            index++;
        }

        // split data up into sections for each holder
        // holder id --> commands
        ListMultimap<String, ImportCommand> sections = MultimapBuilder.linkedHashKeys().arrayListValues().build();
        for (ImportCommand cmd : this.commands) {
            sections.put(Strings.nullToEmpty(cmd.getTarget()), cmd);
        }

        this.notify.forEach(s -> Message.IMPORT_INFO.send(s, "Waiting for initial update task to complete..."));

        // join the update task future before scheduling command executions
        updateTask.join();

        this.notify.forEach(s -> Message.IMPORT_INFO.send(s, "Setting up command executor..."));

        // create a threadpool for the processing
        ExecutorService executor = Executors.newFixedThreadPool(128, new ThreadFactoryBuilder().setNameFormat("luckperms-importer-%d").build());

        // A set of futures, which are really just the processes we need to wait for.
        Set<CompletableFuture<Void>> futures = new HashSet<>();

        AtomicInteger processedCount = new AtomicInteger(0);

        // iterate through each user sublist.
        for (Collection<ImportCommand> subList : sections.asMap().values()) {

            // register and start a new thread to process the sublist
            futures.add(CompletableFuture.completedFuture(subList).thenAcceptAsync(sl -> {

                // iterate through each user in the sublist, and grab their data.
                for (ImportCommand cmd : sl) {
                    cmd.process();
                    processedCount.incrementAndGet();
                }
            }, executor));
        }

        // all of the threads have been scheduled now and are running. we just need to wait for them all to complete
        CompletableFuture<Void> overallFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        this.notify.forEach(s -> Message.IMPORT_INFO.send(s, "All commands have been processed and scheduled - now waiting for the execution to complete."));

        while (true) {
            try {
                overallFuture.get(2, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException e) {
                // abnormal error - just break
                e.printStackTrace();
                break;
            } catch (TimeoutException e) {
                // still executing - send a progress report and continue waiting
                sendProgress(processedCount.get());
                continue;
            }

            // process is complete
            break;
        }

        executor.shutdown();

        long endTime = System.currentTimeMillis();
        double seconds = (endTime - startTime) / 1000.0;

        int errors = (int) this.commands.stream().filter(v -> v.getResult().wasFailure()).count();

        switch (errors) {
            case 0:
                this.notify.forEach(s -> Message.IMPORT_END_COMPLETE.send(s, seconds));
                break;
            case 1:
                this.notify.forEach(s -> Message.IMPORT_END_COMPLETE_ERR_SIN.send(s, seconds, errors));
                break;
            default:
                this.notify.forEach(s -> Message.IMPORT_END_COMPLETE_ERR.send(s, seconds, errors));
                break;
        }

        AtomicInteger errIndex = new AtomicInteger(1);
        for (ImportCommand e : this.commands) {
            if (e.getResult() != null && e.getResult().wasFailure()) {
                for (Sender s : this.notify) {
                    Message.IMPORT_END_ERROR_HEADER.send(s, errIndex.get(), e.getId(), e.getCommand(), e.getResult().toString());
                    e.getOutput().forEach(out -> Message.IMPORT_END_ERROR_CONTENT.send(s, out));
                    Message.IMPORT_END_ERROR_FOOTER.send(s);
                }

                errIndex.incrementAndGet();
            }
        }
    }

    private void sendProgress(int processedCount) {
        int percent = (processedCount * 100) / this.commandList.size();
        int errors = (int) this.commands.stream().filter(v -> v.isCompleted() && v.getResult().wasFailure()).count();

        if (errors == 1) {
            this.notify.forEach(s -> Message.IMPORT_PROGRESS_SIN.send(s, percent, processedCount, this.commands.size(), errors));
        } else {
            this.notify.forEach(s -> Message.IMPORT_PROGRESS.send(s, percent, processedCount, this.commands.size(), errors));
        }
    }

    private static class ImportCommand extends DummySender {
        private static final Splitter ARGUMENT_SPLITTER = Splitter.on(CommandManager.COMMAND_SEPARATOR_PATTERN).omitEmptyStrings();
        private static final Splitter SPACE_SPLITTER = Splitter.on(" ");

        private final CommandManager commandManager;
        private final int id;
        private final String command;

        private final String target;

        private boolean completed = false;

        private final List<String> output = new ArrayList<>();

        private CommandResult result = CommandResult.FAILURE;

        ImportCommand(CommandManager commandManager, int id, String command) {
            super(commandManager.getPlugin(), Sender.IMPORT_UUID, Sender.IMPORT_NAME);
            this.commandManager = commandManager;
            this.id = id;
            this.command = command;
            this.target = determineTarget(command);
        }

        @Override
        protected void consumeMessage(String s) {
            this.output.add(s);
        }

        public void process() {
            if (isCompleted()) {
                return;
            }

            try {
                List<String> args = CommandManager.stripQuotes(ARGUMENT_SPLITTER.splitToList(getCommand()));
                CommandResult result = this.commandManager.onCommand(this, "lp", args, Runnable::run).get();
                setResult(result);
            } catch (Exception e) {
                setResult(CommandResult.FAILURE);
                e.printStackTrace();
            }

            setCompleted(true);
        }

        private static String determineTarget(String command) {
            if (command.startsWith("user ") && command.length() > "user ".length()) {
                String subCmd = command.substring("user ".length());
                if (!subCmd.contains(" ")) {
                    return null;
                }

                String targetUser = SPACE_SPLITTER.split(subCmd).iterator().next();
                return "u:" + targetUser;
            }

            if (command.startsWith("group ") && command.length() > "group ".length()) {
                String subCmd = command.substring("group ".length());
                if (!subCmd.contains(" ")) {
                    return null;
                }

                String targetGroup = SPACE_SPLITTER.split(subCmd).iterator().next();
                return "g:" + targetGroup;
            }

            if (command.startsWith("track ") && command.length() > "track ".length()) {
                String subCmd = command.substring("track ".length());
                if (!subCmd.contains(" ")) {
                    return null;
                }

                String targetTrack = SPACE_SPLITTER.split(subCmd).iterator().next();
                return "t:" + targetTrack;
            }

            if (command.startsWith("creategroup ") && command.length() > "creategroup ".length()) {
                String targetGroup = command.substring("creategroup ".length());
                return "g:" + targetGroup;
            }

            if (command.startsWith("createtrack ") && command.length() > "createtrack ".length()) {
                String targetTrack = command.substring("createtrack ".length());
                return "t:" + targetTrack;
            }

            return null;
        }

        public int getId() {
            return this.id;
        }

        public String getCommand() {
            return this.command;
        }

        public String getTarget() {
            return this.target;
        }

        public boolean isCompleted() {
            return this.completed;
        }

        public List<String> getOutput() {
            return this.output;
        }

        public CommandResult getResult() {
            return this.result;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public void setResult(CommandResult result) {
            this.result = result;
        }
    }

}
