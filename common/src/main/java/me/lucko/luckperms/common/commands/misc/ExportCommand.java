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

package me.lucko.luckperms.common.commands.misc;

import me.lucko.luckperms.common.backup.Exporter;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExportCommand extends SingleCommand {
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ExportCommand() {
        super(CommandSpec.EXPORT, "Export", CommandPermission.EXPORT, Predicates.notInRange(1, 2));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        if (this.running.get()) {
            Message.EXPORT_ALREADY_RUNNING.send(sender);
            return CommandResult.STATE_ERROR;
        }

        boolean includeUsers = !args.remove("--without-users");
        boolean includeGroups = !args.remove("--without-groups");
        boolean upload = args.remove("--upload");

        Exporter exporter;
        if (upload) {
            if (!this.running.compareAndSet(false, true)) {
                Message.EXPORT_ALREADY_RUNNING.send(sender);
                return CommandResult.STATE_ERROR;
            }

            exporter = new Exporter.WebUpload(plugin, sender, includeUsers, includeGroups, label);
        } else {
            Path dataDirectory = plugin.getBootstrap().getDataDirectory();
            Path path = dataDirectory.resolve(args.get(0) + ".json.gz");

            if (!path.getParent().equals(dataDirectory)) {
                Message.FILE_NOT_WITHIN_DIRECTORY.send(sender, path.toString());
                return CommandResult.INVALID_ARGS;
            }

            if (Files.exists(path)) {
                Message.EXPORT_FILE_ALREADY_EXISTS.send(sender, path.toString());
                return CommandResult.INVALID_ARGS;
            }

            try {
                Files.createFile(path);
            } catch (IOException e) {
                Message.EXPORT_FILE_FAILURE.send(sender);
                e.printStackTrace();
                return CommandResult.FAILURE;
            }

            if (!Files.isWritable(path)) {
                Message.EXPORT_FILE_NOT_WRITABLE.send(sender, path.toString());
                return CommandResult.FAILURE;
            }

            if (!this.running.compareAndSet(false, true)) {
                Message.EXPORT_ALREADY_RUNNING.send(sender);
                return CommandResult.STATE_ERROR;
            }

            exporter = new Exporter.SaveFile(plugin, sender, path, includeUsers, includeGroups);
        }

        // Run the exporter in its own thread.
        plugin.getBootstrap().getScheduler().executeAsync(() -> {
            try {
                exporter.run();
            } finally {
                this.running.set(false);
            }
        });

        return CommandResult.SUCCESS;
    }

}
