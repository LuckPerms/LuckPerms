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

package me.lucko.luckperms.common.api.delegates;

import me.lucko.luckperms.api.LPConfiguration;
import me.lucko.luckperms.api.data.DatastoreConfiguration;
import me.lucko.luckperms.common.config.ConfigKey;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;

import java.util.Map;

/**
 * Provides a link between {@link LPConfiguration} and {@link LuckPermsConfiguration}
 */
public class LPConfigurationDelegate implements LPConfiguration {
    private final LuckPermsConfiguration handle;
    private final Unsafe unsafe;

    public LPConfigurationDelegate(LuckPermsConfiguration handle) {
        this.handle = handle;
        this.unsafe = new UnsafeImpl();
    }

    @Override
    public String getServer() {
        return handle.get(ConfigKeys.SERVER);
    }

    @Override
    public int getSyncTime() {
        return handle.get(ConfigKeys.SYNC_TIME);
    }

    @Override
    public boolean getIncludeGlobalPerms() {
        return handle.get(ConfigKeys.INCLUDING_GLOBAL_PERMS);
    }

    @Override
    public boolean getIncludeGlobalWorldPerms() {
        return handle.get(ConfigKeys.INCLUDING_GLOBAL_WORLD_PERMS);
    }

    @Override
    public boolean getApplyGlobalGroups() {
        return handle.get(ConfigKeys.APPLYING_GLOBAL_GROUPS);
    }

    @Override
    public boolean getApplyGlobalWorldGroups() {
        return handle.get(ConfigKeys.APPLYING_GLOBAL_WORLD_GROUPS);
    }

    @Override
    public boolean getOnlineMode() {
        return handle.get(ConfigKeys.USE_SERVER_UUIDS);
    }

    @Override
    public boolean getApplyWildcards() {
        return handle.get(ConfigKeys.APPLYING_WILDCARDS);
    }

    @Override
    public boolean getApplyRegex() {
        return handle.get(ConfigKeys.APPLYING_REGEX);
    }

    @Override
    public boolean getApplyShorthand() {
        return handle.get(ConfigKeys.APPLYING_SHORTHAND);
    }

    @Override
    public boolean getLogNotify() {
        return handle.get(ConfigKeys.LOG_NOTIFY);
    }

    @Override
    public boolean getEnableOps() {
        return handle.get(ConfigKeys.OPS_ENABLED);
    }

    @Override
    public boolean getCommandsAllowOp() {
        return handle.get(ConfigKeys.COMMANDS_ALLOW_OP);
    }

    @Override
    public boolean getAutoOp() {
        return handle.get(ConfigKeys.AUTO_OP);
    }

    @Override
    public String getVaultServer() {
        return handle.get(ConfigKeys.VAULT_SERVER);
    }

    @Override
    public boolean getVaultIncludeGlobal() {
        return handle.get(ConfigKeys.VAULT_INCLUDING_GLOBAL);
    }

    @Override
    public DatastoreConfiguration getDatastoreConfig() {
        return handle.get(ConfigKeys.DATABASE_VALUES);
    }

    @Override
    public String getStorageMethod() {
        return handle.get(ConfigKeys.STORAGE_METHOD);
    }

    @Override
    public boolean getSplitStorage() {
        return handle.get(ConfigKeys.SPLIT_STORAGE);
    }

    @Override
    public Map<String, String> getSplitStorageOptions() {
        return handle.get(ConfigKeys.SPLIT_STORAGE_OPTIONS);
    }

    @Override
    public Unsafe unsafe() {
        return unsafe;
    }

    private final class UnsafeImpl implements Unsafe {

        @Override
        public Object getObject(String key) {
            ConfigKey<?> configKey = ConfigKeys.getAllKeys().get(key.toUpperCase());
            if (configKey == null) {
                throw new IllegalArgumentException("Unknown key: " + key);
            }
            return handle.get(configKey);
        }
    }
}
