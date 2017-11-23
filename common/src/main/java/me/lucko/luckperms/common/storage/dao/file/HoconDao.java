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

package me.lucko.luckperms.common.storage.dao.file;

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class HoconDao extends ConfigurateDao {

    public HoconDao(LuckPermsPlugin plugin, String dataFolderName) {
        super(plugin, "HOCON", ".conf", dataFolderName);
    }

    @Override
    protected ConfigurationNode readFile(File file) throws IOException {
        if (!file.exists()) {
            return null;
        }

        HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                .setSource(() -> Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8))
                .setSink(() -> Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8))
                .build();

        return loader.load();
    }

    @Override
    protected void saveFile(File file, ConfigurationNode node) throws IOException {
        if (node == null) {
            if (file.exists()) {
                file.delete();
            }
            return;
        }

        HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                .setSource(() -> Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8))
                .setSink(() -> Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8))
                .build();

        loader.save(node);
    }
}
