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

package me.lucko.luckperms.common.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes commands consecutively
 */
@RequiredArgsConstructor
public class ConsecutiveExecutor implements Runnable {
    private final CommandManager manager;

    private final List<QueuedCommand> commands = new ArrayList<>();

    public void queueCommand(QueuedCommand command) {
        synchronized (commands) {
            commands.add(command);
        }
    }

    @Override
    public void run() {
        final List<QueuedCommand> toExecute = new ArrayList<>();
        synchronized (commands) {
            toExecute.addAll(commands);
            commands.clear();
        }

        if (toExecute.isEmpty()) {
            return;
        }

        for (QueuedCommand command : toExecute) {
            manager.onCommand(command.getSender(), "perms", command.getArgs());
        }

    }

    @Getter
    @AllArgsConstructor
    public static class QueuedCommand {
        private final Sender sender;
        private final List<String> args;
    }

}
