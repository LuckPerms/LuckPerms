package me.lucko.luckperms.utils;

import lombok.AccessLevel;
import lombok.Getter;
import me.lucko.luckperms.LuckPermsPlugin;

public abstract class LPConfiguration<T extends LuckPermsPlugin> {

    @Getter(AccessLevel.PROTECTED)
    private final T plugin;

    private final String defaultServerName;
    private final boolean defaultIncludeGlobal;
    private final String defaultStorage;

    public LPConfiguration(T plugin, String defaultServerName, boolean defaultIncludeGlobal, String defaultStorage) {
        this.plugin = plugin;
        this.defaultServerName = defaultServerName;
        this.defaultIncludeGlobal = defaultIncludeGlobal;
        this.defaultStorage = defaultStorage;
        init();

        if (Patterns.NON_ALPHA_NUMERIC.matcher(getServer()).find()) {
            plugin.getLogger().severe("Server name defined in config.yml contains invalid characters. Server names can " +
                    "only contain alphanumeric characters.\nDefined server name '" + getServer() + "' will be replaced with '" +
                    defaultServerName + "' (the default)");
            set("server", defaultServerName);
        }

        if (Patterns.NON_ALPHA_NUMERIC.matcher(getDefaultGroupName()).find()) {
            plugin.getLogger().severe("Default group defined in config.yml contains invalid characters. Group names can " +
                    "only contain alphanumeric characters.\nDefined default group name '" + getDefaultGroupName() +
                    "' will be replaced with 'default' (the default)");
            set("default-group", "default");
        }
    }

    protected abstract void init();
    protected abstract void set(String path, Object value);
    protected abstract String getString(String path, String def);
    protected abstract int getInt(String path, int def);
    protected abstract boolean getBoolean(String path, boolean def);

    public String getServer() {
        return getString("server", defaultServerName);
    }

    public int getSyncTime() {
        return getInt("sql.sync-minutes", 3);
    }

    public String getDefaultGroupNode() {
        return "group." + getDefaultGroupName();
    }

    public String getDefaultGroupName() {
        return getString("default-group", "default");
    }

    public boolean getIncludeGlobalPerms() {
        return getBoolean("include-global", defaultIncludeGlobal);
    }

    public boolean getOnlineMode() {
        return getBoolean("online-mode", true);
    }

    public String getDatabaseValue(String value) {
        return getString("sql." + value, null);
    }

    public String getStorageMethod() {
        return getString("storage-method", defaultStorage);
    }
}
