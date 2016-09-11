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

package me.lucko.luckperms.core;

import lombok.AccessLevel;
import lombok.Getter;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.constants.Patterns;
import me.lucko.luckperms.storage.DatastoreConfiguration;

import java.util.HashMap;
import java.util.Map;

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
            plugin.getLog().severe("Server name defined in config.yml contains invalid characters. Server names can " +
                    "only contain alphanumeric characters.\nDefined server name '" + getServer() + "' will be replaced with '" +
                    defaultServerName + "' (the default)");
            set("server", defaultServerName);
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
        return getInt("data.sync-minutes", 3);
    }

    /**
     * As of 2.6, this value is a constant
     * @return the default group node
     */
    @SuppressWarnings("SameReturnValue")
    public String getDefaultGroupNode() {
        return "group.default";
    }

    /**
     * As of 2.6, this value is a constant
     * @return the name of the default group
     */
    @SuppressWarnings("SameReturnValue")
    public String getDefaultGroupName() {
        return "default";
    }

    public boolean getIncludeGlobalPerms() {
        return getBoolean("include-global", defaultIncludeGlobal);
    }

    public boolean getOnlineMode() {
        return getBoolean("online-mode", true);
    }

    public boolean getApplyWildcards() {
        return getBoolean("apply-wildcards", true);
    }

    public boolean getApplyRegex() {
        return getBoolean("apply-regex", true);
    }

    public boolean getApplyShorthand() {
        return getBoolean("apply-shorthand", true);
    }

    public boolean getLogNotify() {
        return getBoolean("log-notify", true);
    }

    public boolean getDebugPermissionChecks() {
        return getBoolean("debug-permission-checks", false);
    }

    public boolean getEnableOps() {
        return !getAutoOp() && getBoolean("enable-ops", true);
    }

    public boolean getCommandsAllowOp() {
        return getBoolean("commands-allow-op", true);
    }

    public boolean getAutoOp() {
        return getBoolean("auto-op", false);
    }

    public String getVaultServer() {
        return getString("vault-server", "global");
    }

    public boolean getVaultIncludeGlobal() {
        return getBoolean("vault-include-global", true);
    }

    public DatastoreConfiguration getDatabaseValues() {
        return new DatastoreConfiguration(
                getString("data.address", null),
                getString("data.database", null),
                getString("data.username", null),
                getString("data.password", null)
        );
    }

    public String getStorageMethod() {
        return getString("storage-method", defaultStorage);
    }

    public boolean getSplitStorage() {
        return getBoolean("split-storage.enabled", false);
    }

    public Map<String, String> getSplitStorageOptions() {
        Map<String, String> map = new HashMap<>();
        map.put("user", getString("split-storage.methods.user", defaultStorage));
        map.put("group", getString("split-storage.methods.group", defaultStorage));
        map.put("track", getString("split-storage.methods.track", defaultStorage));
        map.put("uuid", getString("split-storage.methods.uuid", defaultStorage));
        map.put("log", getString("split-storage.methods.log", defaultStorage));

        return map;
    }
}
