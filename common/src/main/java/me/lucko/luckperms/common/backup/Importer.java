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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.utils.DateUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Handles import operations
 */
public class Importer implements Runnable {
    private static final int PROGRESS_REPORT_SECONDS = 10;

    private final CommandManager commandManager;
    private final Set<Sender> notify;
    private final List<String> commands;
    private final Map<Integer, Result> cmdResult;
    private final ImporterSender fake;

    private long lastMsg = 0;
    private int executing = -1;

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
                .map(s -> s.startsWith("/") ? s.substring("/".length()) : s)
                .map(s -> s.startsWith("perms ") ? s.substring("perms ".length()) : s)
                .map(s -> s.startsWith("luckperms ") ? s.substring("luckperms ".length()) : s)
                .collect(Collectors.toList());

        this.cmdResult = new HashMap<>();
        this.fake = new ImporterSender(commandManager.getPlugin(), this::logMessage);
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        notify.forEach(s -> Message.IMPORT_START.send(s));

        int index = 1;
        for (String command : commands) {
            long time = DateUtil.unixSecondsNow();
            if (lastMsg < (time - PROGRESS_REPORT_SECONDS)) {
                lastMsg = time;

                sendProgress(index);
            }

            executing = index;
            try {
                CommandResult result = commandManager.onCommand(
                        fake,
                        "lp",
                        Util.stripQuotes(Splitter.on(CommandManager.COMMAND_SEPARATOR_PATTERN).omitEmptyStrings().splitToList(command))
                ).get();
                getResult(index, command).setResult(result);

            } catch (Exception e) {
                getResult(index, command).setResult(CommandResult.FAILURE);
                e.printStackTrace();
            }
            index++;
        }

        long endTime = System.currentTimeMillis();
        double seconds = (endTime - startTime) / 1000;

        int errors = (int) cmdResult.values().stream().filter(v -> v.getResult() != null && !v.getResult().asBoolean()).count();

        if (errors == 0) {
            notify.forEach(s -> Message.IMPORT_END_COMPLETE.send(s, seconds));
        } else if (errors == 1) {
            notify.forEach(s -> Message.IMPORT_END_COMPLETE_ERR_SIN.send(s, seconds, errors));
        } else {
            notify.forEach(s -> Message.IMPORT_END_COMPLETE_ERR.send(s, seconds, errors));
        }

        AtomicInteger errIndex = new AtomicInteger(1);
        for (Map.Entry<Integer, Result> e : cmdResult.entrySet()) {
            if (e.getValue().getResult() != null && !e.getValue().getResult().asBoolean()) {
                notify.forEach(s -> {
                    Message.IMPORT_END_ERROR_HEADER.send(s, errIndex.get(), e.getKey(), e.getValue().getCommand(), e.getValue().getResult().toString());
                    for (String out : e.getValue().getOutput()) {
                        Message.IMPORT_END_ERROR_CONTENT.send(s, out);
                    }
                    Message.IMPORT_END_ERROR_FOOTER.send(s);
                });

                errIndex.incrementAndGet();
            }
        }
    }

    private void sendProgress(int executing) {
        int percent = (executing * 100) / commands.size();
        int errors = (int) cmdResult.values().stream().filter(v -> v.getResult() != null && !v.getResult().asBoolean()).count();

        if (errors == 1) {
            notify.forEach(s -> Message.IMPORT_PROGRESS_SIN.send(s, percent, executing, commands.size(), errors));
        } else {
            notify.forEach(s -> Message.IMPORT_PROGRESS.send(s, percent, executing, commands.size(), errors));
        }
    }

    private Result getResult(int executing, String command) {
        return cmdResult.compute(executing, (i, r) -> {
            if (r == null) {
                r = new Result(command);
            } else {
                if (!command.equals("") && r.getCommand().equals("")) {
                    r.setCommand(command);
                }
            }

            return r;
        });
    }

    private void logMessage(String msg) {
        if (executing != -1) {
            getResult(executing, "").getOutput().add(Util.stripColor(msg));
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
