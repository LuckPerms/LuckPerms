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

import com.google.gson.JsonObject;
import me.lucko.luckperms.common.backup.Importer;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.http.UnsuccessfulRequestException;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.util.gson.GsonProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

public class ImportCommand extends SingleCommand {
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ImportCommand() {
        super(CommandSpec.IMPORT, "Import", CommandPermission.IMPORT, Predicates.notInRange(1, 3));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        if (this.running.get()) {
            Message.IMPORT_ALREADY_RUNNING.send(sender);
            return;
        }

        boolean fromFile = !args.remove("--upload");

        JsonObject data;
        if (fromFile) {
            String fileName = args.get(0);
            Path dataDirectory = plugin.getBootstrap().getDataDirectory();
            Path path = dataDirectory.resolve(fileName);

            if (!path.getParent().equals(dataDirectory) || path.getFileName().toString().equals("config.yml")) {
                Message.FILE_NOT_WITHIN_DIRECTORY.send(sender, path.toString());
                return;
            }

            // try auto adding the '.json.gz' extension
            if (!Files.exists(path) && !fileName.contains(".")) {
                Path pathWithDefaultExtension = path.resolveSibling(fileName + ".json.gz");
                if (Files.exists(pathWithDefaultExtension)) {
                    path = pathWithDefaultExtension;
                }
            }

            if (!Files.exists(path)) {
                Message.IMPORT_FILE_DOESNT_EXIST.send(sender, path.toString());
                return;
            }

            if (!Files.isReadable(path)) {
                Message.IMPORT_FILE_NOT_READABLE.send(sender, path.toString());
                return;
            }

            if (!this.running.compareAndSet(false, true)) {
                Message.IMPORT_ALREADY_RUNNING.send(sender);
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(path)), StandardCharsets.UTF_8))) {
                data = GsonProvider.normal().fromJson(reader, JsonObject.class);
            } catch (IOException e) {
                plugin.getLogger().warn("Error whilst reading from the import file", e);
                Message.IMPORT_FILE_READ_FAILURE.send(sender);
                this.running.set(false);
                return;
            }
        } else {
            String code = args.get(0);

            if (code.isEmpty()) {
                Message.IMPORT_WEB_INVALID_CODE.send(sender, code);
                return;
            }

            try {
                data = plugin.getBytebin().getJsonContent(code).getAsJsonObject();
            } catch (UnsuccessfulRequestException e) {
                Message.HTTP_REQUEST_FAILURE.send(sender, e.getResponse().code(), e.getResponse().message());
                return;
            } catch (IOException e) {
                plugin.getLogger().severe("Error reading data to bytebin", e);
                Message.HTTP_UNKNOWN_FAILURE.send(sender);
                return;
            }

            if (data == null) {
                Message.IMPORT_UNABLE_TO_READ.send(sender, code);
                return;
            }
        }

        Importer importer = new Importer(plugin, sender, data, !args.contains("--replace"));

        // Run the importer in its own thread.
        plugin.getBootstrap().getScheduler().async(() -> {
            try {
                importer.run();
            } finally {
                this.running.set(false);
            }
        });
    }

    public boolean isRunning() {
        return this.running.get();
    }

}
