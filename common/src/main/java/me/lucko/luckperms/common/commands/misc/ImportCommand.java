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

import me.lucko.luckperms.common.backup.Importer;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.utils.Predicates;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImportCommand extends SingleCommand {
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ImportCommand(LocaleManager locale) {
        super(CommandSpec.IMPORT.localize(locale), "Import", CommandPermission.IMPORT, Predicates.not(1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        if (this.running.get()) {
            Message.IMPORT_ALREADY_RUNNING.send(sender);
            return CommandResult.STATE_ERROR;
        }

        Path path = plugin.getBootstrap().getDataDirectory().resolve(args.get(0));
        if (!Files.exists(path)) {
            Message.IMPORT_LOG_DOESNT_EXIST.send(sender, path.toString());
            return CommandResult.INVALID_ARGS;
        }

        if (!Files.isReadable(path)) {
            Message.IMPORT_LOG_NOT_READABLE.send(sender, path.toString());
            return CommandResult.FAILURE;
        }

        List<String> commands;

        try {
            commands = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            Message.IMPORT_LOG_FAILURE.send(sender);
            return CommandResult.FAILURE;
        }

        if (!this.running.compareAndSet(false, true)) {
            Message.IMPORT_ALREADY_RUNNING.send(sender);
            return CommandResult.STATE_ERROR;
        }

        Importer importer = new Importer(plugin.getCommandManager(), sender, commands);

        // Run the importer in its own thread.
        plugin.getBootstrap().getScheduler().executeAsync(() -> {
            try {
                importer.run();
            } finally {
                this.running.set(false);
            }
        });

        return CommandResult.SUCCESS;
    }
}
