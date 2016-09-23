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

import me.lucko.luckperms.core.AbstractConfiguration;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

class BungeeConfig extends AbstractConfiguration<LPBungeePlugin> {
    private Configuration configuration;

    BungeeConfig(LPBungeePlugin plugin) {
        super(plugin, "bungee", false, "flatfile");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private File makeFile(String file) throws IOException {
        File cfg = new File(getPlugin().getDataFolder(), file);

        if (!cfg.exists()) {
            getPlugin().getDataFolder().mkdir();
            try (InputStream is = getPlugin().getResourceAsStream(file)) {
                Files.copy(is, cfg.toPath());
            }
        }

        return cfg;
    }

    @Override
    protected void init() {
        try {
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(makeFile("config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    @Override
    protected Map<String, String> getMap(String path, Map<String, String> def) {
        Map<String, String> map = new HashMap<>();
        Configuration section = configuration.getSection(path);
        if (section == null) {
            return def;
        }

        for (String key : section.getKeys()) {
            map.put(key, section.getString(key));
        }

        return map;
    }
}
