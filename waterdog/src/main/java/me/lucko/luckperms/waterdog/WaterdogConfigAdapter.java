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

package me.lucko.luckperms.waterdog;

import dev.waterdog.waterdogpe.utils.config.Configuration;
import dev.waterdog.waterdogpe.utils.config.YamlConfig;
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WaterdogConfigAdapter implements ConfigurationAdapter {
    private final LuckPermsPlugin plugin;
    private final File file;
    private Configuration configuration;

    public WaterdogConfigAdapter(LuckPermsPlugin plugin, File file) {
        this.plugin = plugin;
        this.file = file;
        reload();
    }

    @Override
    public void reload() {
        try {
            this.configuration = new YamlConfig(this.file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getString(String path, String def) {
        return this.configuration.getString(path, def);
    }

    @Override
    public int getInteger(String path, int def) {
        return this.configuration.getInt(path, def);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return this.configuration.getBoolean(path, def);
    }

    @Override
    public List<String> getStringList(String path, List<String> def) {
        return Optional.ofNullable(this.configuration.getStringList(path)).orElse(def);
    }

    @Override
    public List<String> getKeys(String path, List<String> def) {
        Map<String, Object> section = (Map<String, Object>) this.configuration.get(path);
        if (section == null) {
            return def;
        }

        return Optional.of((List<String>) new ArrayList<>(section.keySet())).orElse(def);
    }

    @Override
    public Map<String, String> getStringMap(String path, Map<String, String> def) {
        Map<String, Object> section = (Map<String, Object>) this.configuration.get(path);
        Map<String, String> map = new HashMap<>();

        if (section == null) {
            return def;
        }

        for (String key : section.keySet()) {
            map.put(key, section.get(key).toString());
        }

        return map;
    }

    @Override
    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }
}
