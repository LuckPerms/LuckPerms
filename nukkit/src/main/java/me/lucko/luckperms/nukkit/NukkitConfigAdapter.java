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

package me.lucko.luckperms.nukkit;

import me.lucko.luckperms.common.config.adapter.AbstractConfigurationAdapter;
import me.lucko.luckperms.common.config.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NukkitConfigAdapter extends AbstractConfigurationAdapter implements ConfigurationAdapter {

    private final File file;
    private Config configuration;

    public NukkitConfigAdapter(LuckPermsPlugin plugin, File file) {
        super(plugin);
        this.file = file;
        reload();
    }

    @Override
    public void reload() {
        this.configuration = new Config(this.file, Config.YAML);
    }

    @Override
    public boolean contains(String path) {
        return this.configuration.exists(path);
    }

    @Override
    public String getString(String path, String def) {
        return this.configuration.getString(path, def);
    }

    @Override
    public int getInt(String path, int def) {
        return this.configuration.getInt(path, def);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return this.configuration.getBoolean(path, def);
    }

    @Override
    public List<String> getList(String path, List<String> def) {
        List<String> ret = this.configuration.getStringList(path);
        return ret == null ? def : ret;
    }

    @Override
    public List<String> getObjectList(String path, List<String> def) {
        ConfigSection section = this.configuration.getSection(path);
        if (section == null) {
            return def;
        }

        Set<String> keys = section.getKeys(false);
        return keys == null ? def : new ArrayList<>(keys);
    }

    @Override
    public Map<String, String> getMap(String path, Map<String, String> def) {
        Map<String, String> map = new HashMap<>();
        ConfigSection section = this.configuration.getSection(path);
        if (section == null) {
            return def;
        }

        for (String key : section.getKeys(false)) {
            map.put(key, section.getString(key));
        }

        return map;
    }
}
