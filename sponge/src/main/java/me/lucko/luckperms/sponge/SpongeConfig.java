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

package me.lucko.luckperms.sponge;

import com.google.common.base.Splitter;
import me.lucko.luckperms.common.config.AbstractConfiguration;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class SpongeConfig extends AbstractConfiguration<LPSpongePlugin> {
    private ConfigurationNode root;

    SpongeConfig(LPSpongePlugin plugin) {
        super(plugin, "global", true, "sqlite");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private Path makeFile(Path file) throws IOException {
        File cfg = file.toFile();
        cfg.getParentFile().mkdirs();

        if (!cfg.exists()) {
            try (InputStream is = getPlugin().getClass().getClassLoader().getResourceAsStream("luckperms.conf")) {
                Files.copy(is, cfg.toPath());
            }
        }

        return cfg.toPath();
    }

    @Override
    protected void init() {
        try {
            ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder()
                    .setPath(makeFile(getPlugin().getConfigDir().resolve("luckperms.conf")))
                    .build();

            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ConfigurationNode getNode(String path) {
        Iterable<String> paths = Splitter.on('.').split(path);
        ConfigurationNode node = root;

        for (String s : paths) {
            node = node.getNode(s);
        }

        return node;
    }

    @Override
    protected String getString(String path, String def) {
        return getNode(path).getString(def);
    }

    @Override
    protected int getInt(String path, int def) {
        return getNode(path).getInt(def);
    }

    @Override
    protected boolean getBoolean(String path, boolean def) {
        return getNode(path).getBoolean(def);
    }

    @Override
    protected List<String> getList(String path, List<String> def) {
        ConfigurationNode node = getNode(path);
        if (node.isVirtual()) {
            return def;
        }

        return node.getList(Object::toString);
    }

    @Override
    protected List<String> getObjectList(String path, List<String> def) {
        ConfigurationNode node = getNode(path);
        if (node.isVirtual()) {
            return def;
        }

        return node.getChildrenList().stream().map(n -> (String) n.getKey()).collect(Collectors.toList());
    }

    @Override
    protected Map<String, String> getMap(String path, Map<String, String> def) {
        ConfigurationNode node = getNode(path);
        if (node.isVirtual()) {
            return def;
        }

        return node.getChildrenList().stream().collect(Collectors.toMap(n -> (String) n.getKey(), ConfigurationNode::getString));
    }
}
