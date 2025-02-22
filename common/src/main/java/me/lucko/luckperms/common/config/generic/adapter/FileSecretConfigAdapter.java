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

package me.lucko.luckperms.common.config.generic.adapter;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class FileSecretConfigAdapter extends StringBasedConfigurationAdapter {
    private static final String PREFIX = "luckperms_";

    private final LuckPermsPlugin plugin;
    private final Path directory;

    public FileSecretConfigAdapter(LuckPermsPlugin plugin, String directory) {
        this.plugin = plugin;
        this.directory = directory == null ? null : Paths.get(directory);
    }

    public FileSecretConfigAdapter(LuckPermsPlugin plugin) {
        this(plugin, System.getenv("LUCKPERMS_FILE_SECRET_DIRECTORY"));
    }

    @Override
    protected @Nullable String resolveValue(String path) {
        if (this.directory == null) {
            return null; // not configured
        }

        // e.g.
        // 'server'            -> luckperms_server
        // 'data.table_prefix' -> luckperms_data_table_prefix
        String key = PREFIX + path.toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace('.', '_');

        Path resolvedFile = this.directory.resolve(key);

        String value;
        try {
            value = new String(Files.readAllBytes(resolvedFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }

        String printableValue = ConfigKeys.shouldCensorValue(path) ? "*****" : value;
        this.plugin.getLogger().info(String.format("Resolved configuration value from file secret: %s = %s", key, printableValue));
        return value;
    }

    @Override
    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public void reload() {
        // no-op
    }
}
