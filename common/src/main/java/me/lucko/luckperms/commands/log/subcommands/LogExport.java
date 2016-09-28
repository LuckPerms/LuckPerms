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

package me.lucko.luckperms.commands.log.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.commands.*;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.data.Log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class LogExport extends SubCommand<Log> {
    public LogExport() {
        super("export", "Export the log to a file", Permission.LOG_EXPORT, Predicate.not(1),
                Arg.list(Arg.create("file", true, "the name of the file"))
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Log log, List<String> args, String label) {
        File f = new File(plugin.getMainDir(), args.get(0));
        if (f.exists()) {
            Message.LOG_EXPORT_ALREADY_EXISTS.send(sender, f.getAbsolutePath());
            return CommandResult.INVALID_ARGS;
        }

        if (log.getContent().isEmpty()) {
            Message.LOG_EXPORT_EMPTY.send(sender);
            return CommandResult.STATE_ERROR;
        }

        try {
            f.createNewFile();
        } catch (IOException e) {
            Message.LOG_EXPORT_FAILURE.send(sender);
            e.printStackTrace();
            return CommandResult.FAILURE;
        }

        if (!Files.isWritable(f.toPath())) {
            Message.LOG_EXPORT_NOT_WRITABLE.send(sender, f.getAbsolutePath());
            return CommandResult.FAILURE;
        }

        List<String> data = new ArrayList<>();

        StringBuilder b = new StringBuilder();
        for (LogEntry e : log.getContent()) {
            b.setLength(0);
            b.append("/luckperms ");

            if (e.getType() == 'U') {
                b.append("user ").append(e.getActed().toString()).append(" ").append(e.getAction());
            }

            group:
            if (e.getType() == 'G') {
                if (e.getAction().equalsIgnoreCase("create")) {
                    b.append("creategroup ").append(e.getActedName());
                    break group;
                }

                if (e.getAction().equalsIgnoreCase("delete")) {
                    b.append("deletegroup ").append(e.getActedName());
                    break group;
                }

                b.append("group ").append(e.getActedName()).append(" ").append(e.getAction());
            }

            track:
            if (e.getType() == 'T') {
                if (e.getAction().equalsIgnoreCase("create")) {
                    b.append("createtrack ").append(e.getActedName());
                    break track;
                }

                if (e.getAction().equalsIgnoreCase("delete")) {
                    b.append("deletetrack ").append(e.getActedName());
                    break track;
                }

                b.append("track ").append(e.getActedName()).append(" ").append(e.getAction());
            }

            data.add(b.toString());
        }


        try {
            Files.write(f.toPath(), data, Charset.defaultCharset());
            Message.LOG_EXPORT_SUCCESS.send(sender, f.getAbsolutePath());
            return CommandResult.SUCCESS;
        } catch (IOException e) {
            e.printStackTrace();
            Message.LOG_EXPORT_FAILURE.send(sender);
            return CommandResult.FAILURE;
        }
    }
}
