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

import lombok.Getter;
import lombok.Setter;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.utils.Cycle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Handles import operations
 */
public class Importer implements Runnable {

    private final CommandManager commandManager;
    private final Set<Sender> notify;
    private final List<String> commands;
    private final List<ImportCommand> toExecute;

    public Importer(CommandManager commandManager, Sender executor, List<String> commands) {
        this.commandManager = commandManager;

        if (executor.isConsole()) {
            this.notify = ImmutableSet.of(executor);
        } else {
            this.notify = ImmutableSet.of(executor, commandManager.getPlugin().getConsoleSender());
        }
        this.commands = commands.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> !s.startsWith("#"))
                .filter(s -> !s.startsWith("//"))
                .map(s -> s.startsWith("/luckperms ") ? s.substring("/luckperms ".length()) : s)
                .map(s -> s.startsWith("/lp ") ? s.substring("/lp ".length()) : s)
                .collect(Collectors.toList());
        this.toExecute = new ArrayList<>();
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        notify.forEach(s -> Message.IMPORT_START.send(s));

        // form instances for all commands, and register them
        int index = 1;
        for (String command : commands) {
            ImportCommand cmd = new ImportCommand(commandManager, index, command);
            toExecute.add(cmd);

            if (cmd.getCommand().startsWith("creategroup ") || cmd.getCommand().startsWith("createtrack ")) {
                cmd.process(); // process immediately
            }

            index++;
        }

        // divide commands up into pools
        Cycle<List<ImportCommand>> commandPools = new Cycle<>(CommandUtils.nInstances(128, ArrayList::new));

        String lastTarget = null;
        for (ImportCommand cmd : toExecute) {
            // if the last target isn't the same, skip to a new pool
            if (lastTarget == null || !lastTarget.equals(cmd.getTarget())) {
                commandPools.next();
            }

            commandPools.current().add(cmd);
            lastTarget = cmd.getTarget();
        }

        // A set of futures, which are really just the threads we need to wait for.
        Set<CompletableFuture<Void>> futures = new HashSet<>();

        AtomicInteger processedCount = new AtomicInteger(0);

        // iterate through each user sublist.
        for (List<ImportCommand> subList : commandPools.getBacking()) {

            // register and start a new thread to process the sublist
            futures.add(CompletableFuture.runAsync(() -> {

                // iterate through each user in the sublist, and grab their data.
                for (ImportCommand cmd : subList) {
                    cmd.process();
                    processedCount.incrementAndGet();
                }
            }, commandManager.getPlugin().getScheduler().async()));
        }

        // all of the threads have been scheduled now and are running. we just need to wait for them all to complete
        CompletableFuture<Void> overallFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));

        while (true) {
            try {
                overallFuture.get(10, TimeUnit.SECONDS);
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


        long endTime = System.currentTimeMillis();
        double seconds = (endTime - startTime) / 1000;

        int errors = (int) toExecute.stream().filter(v -> !v.getResult().asBoolean()).count();

        switch (errors) {
            case 0:
                notify.forEach(s -> Message.IMPORT_END_COMPLETE.send(s, seconds));
                break;
            case 1:
                notify.forEach(s -> Message.IMPORT_END_COMPLETE_ERR_SIN.send(s, seconds, errors));
                break;
            default:
                notify.forEach(s -> Message.IMPORT_END_COMPLETE_ERR.send(s, seconds, errors));
                break;
        }

        AtomicInteger errIndex = new AtomicInteger(1);
        for (ImportCommand e : toExecute) {
            if (e.getResult() != null && !e.getResult().asBoolean()) {
                notify.forEach(s -> {
                    Message.IMPORT_END_ERROR_HEADER.send(s, errIndex.get(), e.getId(), e.getCommand(), e.getResult().toString());
                    for (String out : e.getOutput()) {
                        Message.IMPORT_END_ERROR_CONTENT.send(s, out);
                    }
                    Message.IMPORT_END_ERROR_FOOTER.send(s);
                });

                errIndex.incrementAndGet();
            }
        }
    }

    private void sendProgress(int processedCount) {
        int percent = (processedCount * 100) / commands.size();
        int errors = (int) toExecute.stream().filter(v -> v.isCompleted() && !v.getResult().asBoolean()).count();

        if (errors == 1) {
            notify.forEach(s -> Message.IMPORT_PROGRESS_SIN.send(s, percent, processedCount, commands.size(), errors));
        } else {
            notify.forEach(s -> Message.IMPORT_PROGRESS.send(s, percent, processedCount, commands.size(), errors));
        }
    }

    @Getter
    private static class ImportCommand extends DummySender {
        private static final Splitter ARGUMENT_SPLITTER = Splitter.on(CommandManager.COMMAND_SEPARATOR_PATTERN).omitEmptyStrings();
        private static final Splitter SPACE_SPLITTER = Splitter.on(" ");

        private final CommandManager commandManager;
        private final int id;
        private final String command;

        private final String target;

        @Setter
        private boolean completed = false;

        private final List<String> output = new ArrayList<>();

        @Setter
        private CommandResult result = CommandResult.FAILURE;

        ImportCommand(CommandManager commandManager, int id, String command) {
            super(commandManager.getPlugin());
            this.commandManager = commandManager;
            this.id = id;
            this.command = command;
            this.target = determineTarget(command);
        }

        @Override
        protected void consumeMessage(String s) {
            output.add(s);
        }

        public void process() {
            if (isCompleted()) {
                return;
            }

            try {
                List<String> args = CommandManager.stripQuotes(ARGUMENT_SPLITTER.splitToList(getCommand()));
                CommandResult result = commandManager.onCommand(this, "lp", args, Runnable::run).get();
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

    }

}
