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

package me.lucko.luckperms.sponge;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.google.common.base.Splitter;

import me.lucko.luckperms.common.config.ConfigurationAdapter;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.SimpleConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SpongeConfigAdapter implements ConfigurationAdapter {

    @Getter
    private final LPSpongePlugin plugin;

    private ConfigurationNode root;

    private Path makeFile(Path file) throws IOException {
        File cfg = file.toFile();
        //noinspection ResultOfMethodCallIgnored
        cfg.getParentFile().mkdirs();

        if (!cfg.exists()) {
            try (InputStream is = plugin.getClass().getClassLoader().getResourceAsStream("luckperms.conf")) {
                Files.copy(is, cfg.toPath());
            }
        }

        return cfg.toPath();
    }

    @Override
    public void init() {
        try {
            ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder()
                    .setPath(makeFile(plugin.getConfigDir().resolve("luckperms.conf")))
                    .build();

            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ConfigurationNode resolvePath(String path) {
        Iterable<String> paths = Splitter.on('.').split(path);
        ConfigurationNode node = root;

        if (node == null) {
            throw new RuntimeException("Config is not loaded.");
        }

        for (String s : paths) {
            node = node.getNode(s);

            if (node == null) {
                return SimpleConfigurationNode.root();
            }
        }

        return node;
    }

    @Override
    public boolean contains(String path) {
        return !resolvePath(path).isVirtual();
    }

    @Override
    public String getString(String path, String def) {
        return resolvePath(path).getString(def);
    }

    @Override
    public int getInt(String path, int def) {
        return resolvePath(path).getInt(def);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return resolvePath(path).getBoolean(def);
    }

    @Override
    public List<String> getList(String path, List<String> def) {
        ConfigurationNode node = resolvePath(path);
        if (node.isVirtual()) {
            return def;
        }

        return node.getList(Object::toString);
    }

    @Override
    public List<String> getObjectList(String path, List<String> def) {
        ConfigurationNode node = resolvePath(path);
        if (node.isVirtual()) {
            return def;
        }

        return node.getChildrenMap().keySet().stream().map(Object::toString).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> getMap(String path, Map<String, String> def) {
        ConfigurationNode node = resolvePath(path);
        if (node.isVirtual()) {
            return def;
        }

        Map<String, Object> m = (Map<String, Object>) node.getValue(Collections.emptyMap());
        return m.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().toString()));
    }
}
