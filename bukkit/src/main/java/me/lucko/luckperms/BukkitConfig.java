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
