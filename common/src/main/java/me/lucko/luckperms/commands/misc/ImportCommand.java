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

package me.lucko.luckperms.commands.misc;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.CommandResult;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SingleMainCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.data.Importer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

public class ImportCommand extends SingleMainCommand {
    public ImportCommand() {
        super("Import", "/%s import <file>", 1, Permission.IMPORT);
    }

    @Override
    protected CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        if (args.size() == 0) {
            sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        Importer importer = plugin.getImporter();

        File f = new File(plugin.getMainDir(), args.get(0));
        if (!f.exists()) {
            Message.IMPORT_LOG_DOESNT_EXIST.send(sender, f.getAbsolutePath());
            return CommandResult.INVALID_ARGS;
        }

        if (!Files.isReadable(f.toPath())) {
            Message.IMPORT_LOG_NOT_READABLE.send(sender, f.getAbsolutePath());
            return CommandResult.FAILURE;
        }

        List<String> commands;

        try {
            commands = Files.readAllLines(f.toPath(), Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
            Message.IMPORT_LOG_FAILURE.send(sender);
            return CommandResult.FAILURE;
        }

        if (!importer.startRun()) {
            Message.IMPORT_ALREADY_RUNNING.send(sender);
            return CommandResult.STATE_ERROR;
        }

        // Run the importer in its own thread.
        plugin.doAsync(() -> importer.start(sender, commands));
        return CommandResult.SUCCESS;
    }
}
