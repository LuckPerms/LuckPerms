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
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExportCommand extends SingleCommand {
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ExportCommand(LocaleManager locale) {
        super(CommandSpec.EXPORT.localize(locale), "Export", CommandPermission.EXPORT, Predicates.notInRange(1, 2));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        if (this.running.get()) {
            Message.EXPORT_ALREADY_RUNNING.send(sender);
            return CommandResult.STATE_ERROR;
        }

        Path dataDirectory = plugin.getBootstrap().getDataDirectory();
        Path path = dataDirectory.resolve(args.get(0) + ".json.gz");

        if (!path.getParent().equals(dataDirectory)) {
            Message.FILE_NOT_WITHIN_DIRECTORY.send(sender, path.toString());
            return CommandResult.INVALID_ARGS;
        }

        boolean includeUsers = !args.remove("--without-users");

        if (Files.exists(path)) {
            Message.LOG_EXPORT_ALREADY_EXISTS.send(sender, path.toString());
            return CommandResult.INVALID_ARGS;
        }

        try {
            Files.createFile(path);
        } catch (IOException e) {
            Message.LOG_EXPORT_FAILURE.send(sender);
            e.printStackTrace();
            return CommandResult.FAILURE;
        }

        if (!Files.isWritable(path)) {
            Message.LOG_EXPORT_NOT_WRITABLE.send(sender, path.toString());
            return CommandResult.FAILURE;
        }

        if (!this.running.compareAndSet(false, true)) {
            Message.EXPORT_ALREADY_RUNNING.send(sender);
            return CommandResult.STATE_ERROR;
        }

        Exporter exporter = new Exporter(plugin, sender, path, includeUsers);

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
