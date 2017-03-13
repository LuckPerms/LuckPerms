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

package me.lucko.luckperms.common.commands.impl.misc;

import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SingleCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.data.Exporter;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExportCommand extends SingleCommand {
    private AtomicBoolean running = new AtomicBoolean(false);

    public ExportCommand() {
        super("Export", "Export data to a file", "/%s export <file>", Permission.EXPORT, Predicates.not(1),
                Arg.list(
                        Arg.create("file", true, "the file to export to")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        if (running.get()) {
            Message.EXPORT_ALREADY_RUNNING.send(sender);
            return CommandResult.STATE_ERROR;
        }

        File f = new File(plugin.getDataDirectory(), args.get(0));
        if (f.exists()) {
            Message.LOG_EXPORT_ALREADY_EXISTS.send(sender, f.getAbsolutePath());
            return CommandResult.INVALID_ARGS;
        }

        Path path = f.toPath();

        try {
            f.createNewFile();
        } catch (IOException e) {
            Message.LOG_EXPORT_FAILURE.send(sender);
            e.printStackTrace();
            return CommandResult.FAILURE;
        }

        if (!Files.isWritable(path)) {
            Message.LOG_EXPORT_NOT_WRITABLE.send(sender, f.getAbsolutePath());
            return CommandResult.FAILURE;
        }

        if (!running.compareAndSet(false, true)) {
            Message.EXPORT_ALREADY_RUNNING.send(sender);
            return CommandResult.STATE_ERROR;
        }

        Exporter exporter = new Exporter(plugin, sender, path);

        // Run the exporter in its own thread.
        plugin.doAsync(() -> {
            try {
                exporter.run();
            } finally {
                running.set(false);
            }
        });

        return CommandResult.SUCCESS;
    }

}
