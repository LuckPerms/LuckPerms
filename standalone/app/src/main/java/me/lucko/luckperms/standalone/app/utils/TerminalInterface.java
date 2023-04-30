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

package me.lucko.luckperms.standalone.app.utils;

import me.lucko.luckperms.standalone.app.LuckPermsApplication;
import me.lucko.luckperms.standalone.app.integration.CommandExecutor;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;

import java.util.List;

/**
 * The terminal/console-style interface presented to the user.
 */
public class TerminalInterface extends SimpleTerminalConsole {
    private final LuckPermsApplication application;
    private final CommandExecutor commandExecutor;

    public TerminalInterface(LuckPermsApplication application, CommandExecutor commandExecutor) {
        this.application = application;
        this.commandExecutor = commandExecutor;
    }

    @Override
    protected LineReader buildReader(LineReaderBuilder builder) {
        return super.buildReader(builder
                .appName("LuckPerms")
                .completer(this::completeCommand)
        );
    }

    @Override
    protected boolean isRunning() {
        return this.application.runningState().get();
    }

    @Override
    protected void shutdown() {
        this.application.requestShutdown();
    }

    @Override
    public void runCommand(String command) {
        command = stripSlashLp(command);

        if (command.equals("stop") || command.equals("exit")) {
            this.application.requestShutdown();
            return;
        }

        this.commandExecutor.execute(command);
    }

    private void completeCommand(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String cmdLine = stripSlashLp(line.line());

        for (String suggestion : this.commandExecutor.tabComplete(cmdLine)) {
            candidates.add(new Candidate(suggestion));
        }
    }

    private static String stripSlashLp(String command) {
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (command.startsWith("lp ")) {
            command = command.substring(3);
        }
        return command;
    }

}
