package me.lucko.luckperms;

import me.lucko.luckperms.utils.LPConfiguration;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class BungeeConfig implements LPConfiguration {
    private final LPBungeePlugin plugin;
    private Configuration configuration;

    public BungeeConfig(LPBungeePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    private void reload() {
        try {
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(makeFile("config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File makeFile(String file) throws IOException {
        File cfg = new File(plugin.getDataFolder(), file);

        if (!cfg.exists()) {
            plugin.getDataFolder().mkdir();
            try (InputStream is = plugin.getResourceAsStream(file)) {
                Files.copy(is, cfg.toPath());
            }
        }

        return cfg;
    }


    @Override
    public String getServer() {
        return configuration.getString("server");
    }

    @Override
    public String getPrefix() {
        return configuration.getString("prefix");
    }

    @Override
    public int getSyncTime() {
        return configuration.getInt("sql.sync-minutes");
    }

    @Override
    public String getDefaultGroupNode() {
        return "luckperms.group." + configuration.getString("default-group");
    }

    @Override
    public String getDefaultGroupName() {
        return configuration.getString("default-group");
    }

    @Override
    public boolean getIncludeGlobalPerms() {
        return configuration.getBoolean("include-global");
    }

    @Override
    public String getDatabaseValue(String value) {
        return configuration.getString("sql." + value);
    }
}
