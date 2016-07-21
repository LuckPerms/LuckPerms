package me.lucko.luckperms;

import me.lucko.luckperms.utils.LPConfiguration;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

class BungeeConfig extends LPConfiguration<LPBungeePlugin> {
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
