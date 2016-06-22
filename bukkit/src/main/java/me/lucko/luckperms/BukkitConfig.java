package me.lucko.luckperms;

import me.lucko.luckperms.utils.LPConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class BukkitConfig implements LPConfiguration {
    private final LPBukkitPlugin plugin;
    private YamlConfiguration configuration;

    public BukkitConfig(LPBukkitPlugin plugin) {
        this.plugin = plugin;
        create();
    }

    private void create() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("config.yml", false);
        }

        configuration = new YamlConfiguration();

        try {
            configuration.load(configFile);

        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getServer() {
        return configuration.getString("server", "global");
    }

    @Override
    public String getPrefix() {
        return configuration.getString("prefix", "&7&l[&b&lL&a&lP&7&l] &c");
    }

    @Override
    public int getSyncTime() {
        return configuration.getInt("sql.sync-minutes", 3);
    }

    @Override
    public String getDefaultGroupNode() {
        return "luckperms.group." + configuration.getString("default-group", "default");
    }

    @Override
    public String getDefaultGroupName() {
        return configuration.getString("default-group", "default");
    }

    @Override
    public boolean getIncludeGlobalPerms() {
        return configuration.getBoolean("include-global", true);
    }

    @Override
    public String getDatabaseValue(String value) {
        return configuration.getString("sql." + value);
    }

    @Override
    public String getStorageMethod() {
        return configuration.getString("storage-method", "sqlite");
    }
}
