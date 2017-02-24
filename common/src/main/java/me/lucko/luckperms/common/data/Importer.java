/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.common.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Patterns;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import io.github.mkremins.fanciful.FancyMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Class to handle import operations
 */
@RequiredArgsConstructor
public class Importer {
    private final CommandManager commandManager;

    private boolean running = false;
    private Set<Sender> notify = ImmutableSet.of();
    private List<String> commands = null;
    private Map<Integer, Result> cmdResult = null;

    private long lastMsg = 0;
    private int executing = -1;

    public synchronized boolean startRun() {
        if (running) {
            return false;
        }

        running = true;
        return true;
    }

    public void start(Sender executor, List<String> commands) {
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
                .map(s -> s.startsWith("/") ? s.substring("/".length()) : s)
                .map(s -> s.startsWith("perms ") ? s.substring("perms ".length()) : s)
                .map(s -> s.startsWith("luckperms ") ? s.substring("luckperms ".length()) : s)
                .collect(Collectors.toList());

        cmdResult = new HashMap<>();

        run();
    }

    private void cleanup() {
        notify = ImmutableSet.of();
        commands = null;
        cmdResult = null;
        lastMsg = 0;
        executing = -1;
        running = false;
    }

    public void run() {
        long startTime = System.currentTimeMillis();
        notify.forEach(s -> Message.IMPORT_START.send(s));
        final Sender fake = new FakeSender(this);

        int index = 1;
        for (String command : commands) {
            if (lastMsg < (System.currentTimeMillis() / 1000) - 5) {
                lastMsg = System.currentTimeMillis() / 1000;

                sendProgress(index);
            }

            executing = index;
            try {
                CommandResult result = commandManager.onCommand(
                        fake,
                        "lp",
                        Util.stripQuotes(Splitter.on(Patterns.COMMAND_SEPARATOR).omitEmptyStrings().splitToList(command))
                ).get();
                getResult(index, command).setResult(result);

            } catch (Exception e) {
                getResult(index, command).setResult(CommandResult.FAILURE);
                e.printStackTrace();
            }
            index++;
        }

        long endTime = System.currentTimeMillis();
        double seconds = (endTime - startTime) / 1000.0;

        int errors = 0;
        for (Map.Entry<Integer, Result> e : cmdResult.entrySet()) {
            if (e.getValue().getResult() != null && !e.getValue().getResult().asBoolean()) {
                errors++;
            }
        }

        if (errors == 0) {
            for (Sender s : notify) {
                Message.IMPORT_END_COMPLETE.send(s, seconds);
            }
        } else if (errors == 1) {
            for (Sender s : notify) {
                Message.IMPORT_END_COMPLETE_ERR_SIN.send(s, seconds, errors);
            }
        } else {
            for (Sender s : notify) {
                Message.IMPORT_END_COMPLETE_ERR.send(s, seconds, errors);
            }
        }

        int errIndex = 1;
        for (Map.Entry<Integer, Result> e : cmdResult.entrySet()) {
            if (e.getValue().getResult() != null && !e.getValue().getResult().asBoolean()) {
                for (Sender s : notify) {
                    Message.IMPORT_END_ERROR_HEADER.send(s, errIndex, e.getKey(), e.getValue().getCommand(), e.getValue().getResult().toString());
                    for (String out : e.getValue().getOutput()) {
                        Message.IMPORT_END_ERROR_CONTENT.send(s, out);
                    }
                    Message.IMPORT_END_ERROR_FOOTER.send(s);
                }

                errIndex++;
            }
        }

        cleanup();
    }

    private void sendProgress(int executing) {
        int percent = (executing * 100) / commands.size();
        int errors = 0;

        for (Map.Entry<Integer, Result> e : cmdResult.entrySet()) {
            if (e.getValue().getResult() != null && !e.getValue().getResult().asBoolean()) {
                errors++;
            }
        }

        if (errors == 1) {
            for (Sender s : notify) {
                Message.IMPORT_PROGRESS_SIN.send(s, percent, executing, commands.size(), errors);
            }
        } else {
            for (Sender s : notify) {
                Message.IMPORT_PROGRESS.send(s, percent, executing, commands.size(), errors);
            }
        }
    }

    private Result getResult(int executing, String command) {
        if (!cmdResult.containsKey(executing)) {
            cmdResult.put(executing, new Result(command));
        }

        Result existing = cmdResult.get(executing);

        if (!command.equals("") && existing.getCommand().equals("")) {
            existing.setCommand(command);
        }

        return existing;
    }

    private void logMessage(String msg) {
        if (executing != -1) {
            getResult(executing, "").getOutput().add(Util.stripColor(msg));
        }
    }

    private static class FakeSender implements Sender {
        private final Importer instance;

        private FakeSender(Importer instance) {
            this.instance = instance;
        }

        @Override
        public LuckPermsPlugin getPlatform() {
            return instance.commandManager.getPlugin();
        }

        @Override
        public String getName() {
            return Constants.IMPORT_NAME;
        }

        @Override
        public UUID getUuid() {
            return Constants.IMPORT_UUID;
        }

        @Override
        public void sendMessage(String s) {
            instance.logMessage(s);
        }

        @Override
        public void sendMessage(FancyMessage message) {
            instance.logMessage(message.toOldMessageFormat());
        }

        @Override
        public boolean hasPermission(Permission permission) {
            return true;
        }

        @Override
        public boolean isConsole() {
            return true;
        }
    }

    @Getter
    @Setter
    private static class Result {

        @Setter(AccessLevel.NONE)
        private final List<String> output = new ArrayList<>();

        private String command;
        private CommandResult result = CommandResult.FAILURE;

        private Result(String command) {
            this.command = command;
        }
    }

}
