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

        if (Patterns.NON_ALPHA_NUMERIC.matcher(getDefaultGroupName()).find()) {
            plugin.getLog().severe("Default group defined in config.yml contains invalid characters. Group names can " +
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
        return getInt("data.sync-minutes", 3);
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

    public boolean getApplyWildcards() {
        return getBoolean("apply-wildcards", true);
    }

    public boolean getApplyRegex() {
        return getBoolean("apply-regex", true);
    }

    public boolean getApplyShorthand() {
        return getBoolean("apply-shorthand", true);
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
}
