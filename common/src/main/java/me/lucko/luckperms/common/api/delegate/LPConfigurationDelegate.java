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

package me.lucko.luckperms.common.api.delegate;

import lombok.AllArgsConstructor;

import me.lucko.luckperms.api.LPConfiguration;
import me.lucko.luckperms.api.data.DatastoreConfiguration;
import me.lucko.luckperms.api.data.MySQLConfiguration;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;

import java.util.Map;

/**
 * Provides a link between {@link LPConfiguration} and {@link LuckPermsConfiguration}
 */
@AllArgsConstructor
public class LPConfigurationDelegate implements LPConfiguration {
    private final LuckPermsConfiguration master;

    @Override
    public String getServer() {
        return master.get(ConfigKeys.SERVER);
    }

    @Override
    public int getSyncTime() {
        return master.get(ConfigKeys.SYNC_TIME);
    }

    @Override
    public String getDefaultGroupNode() {
        return master.get(ConfigKeys.DEFAULT_GROUP_NODE);
    }

    @Override
    public String getDefaultGroupName() {
        return master.get(ConfigKeys.DEFAULT_GROUP_NAME);
    }

    @Override
    public boolean getIncludeGlobalPerms() {
        return master.get(ConfigKeys.INCLUDING_GLOBAL_PERMS);
    }

    @Override
    public boolean getIncludeGlobalWorldPerms() {
        return master.get(ConfigKeys.INCLUDING_GLOBAL_WORLD_PERMS);
    }

    @Override
    public boolean getApplyGlobalGroups() {
        return master.get(ConfigKeys.APPLYING_GLOBAL_GROUPS);
    }

    @Override
    public boolean getApplyGlobalWorldGroups() {
        return master.get(ConfigKeys.APPLYING_GLOBAL_WORLD_GROUPS);
    }

    @Override
    public boolean getOnlineMode() {
        return master.get(ConfigKeys.ONLINE_MODE);
    }

    @Override
    public boolean getApplyWildcards() {
        return master.get(ConfigKeys.APPLYING_WILDCARDS);
    }

    @Override
    public boolean getApplyRegex() {
        return master.get(ConfigKeys.APPLYING_REGEX);
    }

    @Override
    public boolean getApplyShorthand() {
        return master.get(ConfigKeys.APPLYING_SHORTHAND);
    }

    @Override
    public boolean getLogNotify() {
        return master.get(ConfigKeys.LOG_NOTIFY);
    }

    @Override
    public boolean getDebugPermissionChecks() {
        return false;
    }

    @Override
    public boolean getEnableOps() {
        return master.get(ConfigKeys.OPS_ENABLED);
    }

    @Override
    public boolean getCommandsAllowOp() {
        return master.get(ConfigKeys.COMMANDS_ALLOW_OP);
    }

    @Override
    public boolean getAutoOp() {
        return master.get(ConfigKeys.AUTO_OP);
    }

    @Override
    public String getVaultServer() {
        return master.get(ConfigKeys.VAULT_SERVER);
    }

    @Override
    public boolean getVaultIncludeGlobal() {
        return master.get(ConfigKeys.VAULT_INCLUDING_GLOBAL);
    }

    @SuppressWarnings("deprecation")
    @Override
    public MySQLConfiguration getDatabaseValues() {
        return getDatastoreConfig();
    }

    @Override
    public DatastoreConfiguration getDatastoreConfig() {
        return master.get(ConfigKeys.DATABASE_VALUES);
    }

    @Override
    public String getStorageMethod() {
        return master.get(ConfigKeys.STORAGE_METHOD);
    }

    @Override
    public boolean getSplitStorage() {
        return master.get(ConfigKeys.SPLIT_STORAGE);
    }

    @Override
    public Map<String, String> getSplitStorageOptions() {
        return master.get(ConfigKeys.SPLIT_STORAGE_OPTIONS);
    }
}
