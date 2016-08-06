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

package me.lucko.luckperms;

import me.lucko.luckperms.utils.LPConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

class BukkitConfig extends LPConfiguration<LPBukkitPlugin> {
    private YamlConfiguration configuration;

    BukkitConfig(LPBukkitPlugin plugin) {
        super(plugin, "global", true, "sqlite");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void init() {
        File configFile = new File(getPlugin().getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            getPlugin().saveResource("config.yml", false);
        }

        configuration = new YamlConfiguration();

        try {
            configuration.load(configFile);
        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void set(String path, Object value) {
        configuration.set(path, value);
    }

    @Override
    protected String getString(String path, String def) {
        return configuration.getString(path, def);
    }

    @Override
    protected int getInt(String path, int def) {
        return configuration.getInt(path, def);
    }

    @Override
    protected boolean getBoolean(String path, boolean def) {
        return configuration.getBoolean(path, def);
    }
}
