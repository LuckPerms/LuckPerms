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

package me.lucko.luckperms.common.api.internal;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.api.LPConfiguration;
import me.lucko.luckperms.api.data.DatastoreConfiguration;
import me.lucko.luckperms.api.data.MySQLConfiguration;

import java.util.Map;

/**
 * Provides a link between {@link LPConfiguration} and {@link me.lucko.luckperms.common.config.LPConfiguration}
 */
@AllArgsConstructor
public class LPConfigurationLink implements LPConfiguration {
    private final me.lucko.luckperms.common.config.LPConfiguration master;

    @Override
    public String getServer() {
        return master.getServer();
    }

    @Override
    public int getSyncTime() {
        return master.getSyncTime();
    }

    @Override
    public String getDefaultGroupNode() {
        return master.getDefaultGroupNode();
    }

    @Override
    public String getDefaultGroupName() {
        return master.getDefaultGroupName();
    }

    @Override
    public boolean getIncludeGlobalPerms() {
        return master.isIncludingGlobalPerms();
    }

    @Override
    public boolean getIncludeGlobalWorldPerms() {
        return master.isIncludingGlobalWorldPerms();
    }

    @Override
    public boolean getApplyGlobalGroups() {
        return master.isApplyingGlobalGroups();
    }

    @Override
    public boolean getApplyGlobalWorldGroups() {
        return master.isApplyingGlobalWorldGroups();
    }

    @Override
    public boolean getOnlineMode() {
        return master.isOnlineMode();
    }

    @Override
    public boolean getApplyWildcards() {
        return master.isApplyingWildcards();
    }

    @Override
    public boolean getApplyRegex() {
        return master.isApplyingRegex();
    }

    @Override
    public boolean getApplyShorthand() {
        return master.isApplyingShorthand();
    }

    @Override
    public boolean getLogNotify() {
        return master.isLogNotify();
    }

    @Override
    public boolean getDebugPermissionChecks() {
        return false;
    }

    @Override
    public boolean getEnableOps() {
        return master.isOpsEnabled();
    }

    @Override
    public boolean getCommandsAllowOp() {
        return master.isCommandsAllowOp();
    }

    @Override
    public boolean getAutoOp() {
        return master.isAutoOp();
    }

    @Override
    public String getVaultServer() {
        return master.getVaultServer();
    }

    @Override
    public boolean getVaultIncludeGlobal() {
        return master.isVaultIncludingGlobal();
    }

    @SuppressWarnings("deprecation")
    @Override
    public MySQLConfiguration getDatabaseValues() {
        return getDatastoreConfig();
    }

    @Override
    public DatastoreConfiguration getDatastoreConfig() {
        return master.getDatabaseValues();
    }

    @Override
    public String getStorageMethod() {
        return master.getStorageMethod();
    }

    @Override
    public boolean getSplitStorage() {
        return master.isSplitStorage();
    }

    @Override
    public Map<String, String> getSplitStorageOptions() {
        return master.getSplitStorageOptions();
    }
}
