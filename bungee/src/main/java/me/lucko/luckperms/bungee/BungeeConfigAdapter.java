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

package me.lucko.luckperms.bungee;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.common.config.ConfigurationAdapter;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class BungeeConfigAdapter implements ConfigurationAdapter {

    @Getter
    private final LPBungeePlugin plugin;

    private Configuration configuration;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private File makeFile(String file) throws IOException {
        File configFile = new File(plugin.getDataFolder(), file);

        if (!configFile.exists()) {
            plugin.getDataFolder().mkdir();
            try (InputStream is = plugin.getResourceAsStream(file)) {
                Files.copy(is, configFile.toPath());
            }
        }

        return configFile;
    }

    @Override
    public void init() {
        try {
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(makeFile("config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean contains(String path) {
        return configuration.contains(path);
    }

    @Override
    public String getString(String path, String def) {
        return configuration.getString(path, def);
    }

    @Override
    public int getInt(String path, int def) {
        return configuration.getInt(path, def);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return configuration.getBoolean(path, def);
    }

    @Override
    public List<String> getList(String path, List<String> def) {
        return Optional.ofNullable(configuration.getStringList(path)).orElse(def);
    }

    @Override
    public List<String> getObjectList(String path, List<String> def) {
        Configuration section = configuration.getSection(path);
        if (section == null) {
            return def;
        }

        return Optional.of((List<String>) new ArrayList<>(section.getKeys())).orElse(def);
    }

    @Override
    public Map<String, String> getMap(String path, Map<String, String> def) {
        Map<String, String> map = new HashMap<>();
        Configuration section = configuration.getSection(path);
        if (section == null) {
            return def;
        }

        for (String key : section.getKeys()) {
            map.put(key, section.get(key).toString());
        }

        return map;
    }
}
