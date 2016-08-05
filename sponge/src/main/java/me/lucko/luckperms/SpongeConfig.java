package me.lucko.luckperms;

import me.lucko.luckperms.utils.LPConfiguration;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

class SpongeConfig extends LPConfiguration<LPSpongePlugin> {
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
        String[] paths = path.split("\\.");
        ConfigurationNode node = root;

        for (String s : paths) {
            node = node.getNode(s);
        }

        return node;
    }

    @Override
    protected void set(String path, Object value) {
        getNode(path).setValue(value);
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
}
