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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import com.google.common.base.Splitter;

import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import io.github.mkremins.fanciful.FancyMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Class to handle import operations
 */
@RequiredArgsConstructor
public class Importer {
    private final CommandManager commandManager;

    private boolean running = false;
    private Sender executor = null;
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
        this.executor = executor;
        this.commands = commands.stream()
                .map(s -> s.startsWith("/") ? s.substring(1) : s)
                .map(s -> s.startsWith("perms ") ? s.substring(6) : s)
                .map(s -> s.startsWith("luckperms ") ? s.substring(10) : s)
                .collect(Collectors.toList());

        cmdResult = new HashMap<>();

        run();
    }

    private void cleanup() {
        executor = null;
        commands = null;
        cmdResult = null;
        lastMsg = 0;
        executing = -1;
        running = false;
    }

    public void run() {
        long startTime = System.currentTimeMillis();
        Message.IMPORT_START.send(executor);
        final Sender fake = new FakeSender(this);

        int index = 1;
        for (String command : commands) {
            if (lastMsg < (System.currentTimeMillis() / 1000) - 5) {
                lastMsg = System.currentTimeMillis() / 1000;

                sendProgress(index);
            }

            executing = index;
            try {
                CommandResult result = commandManager.onCommand(fake, "perms", Splitter.on(' ').splitToList(command)).get();
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
            Message.IMPORT_END_COMPLETE.send(executor, seconds);
        } else if (errors == 1) {
            Message.IMPORT_END_COMPLETE_ERR_SIN.send(executor, seconds, errors);
        } else {
            Message.IMPORT_END_COMPLETE_ERR.send(executor, seconds, errors);
        }

        int errIndex = 1;
        for (Map.Entry<Integer, Result> e : cmdResult.entrySet()) {
            if (e.getValue().getResult() != null && !e.getValue().getResult().asBoolean()) {
                Message.IMPORT_END_ERROR_HEADER.send(executor, errIndex, e.getKey(), e.getValue().getCommand(), e.getValue().getResult().toString());
                for (String s : e.getValue().getOutput()) {
                    Message.IMPORT_END_ERROR_CONTENT.send(executor, s);
                }
                Message.IMPORT_END_ERROR_FOOTER.send(executor);
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
            Message.IMPORT_PROGRESS_SIN.send(executor, percent, executing, commands.size(), errors);
        } else {
            Message.IMPORT_PROGRESS.send(executor, percent, executing, commands.size(), errors);
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
    }

    private static class Result {
        @Getter
        private final List<String> output = new ArrayList<>();
        @Getter
        @Setter
        private String command;
        @Getter
        @Setter
        private CommandResult result = CommandResult.FAILURE;

        private Result(String command) {
            this.command = command;
        }
    }

}
