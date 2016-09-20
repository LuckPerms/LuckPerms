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

package me.lucko.luckperms.api.implementation.internal;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.api.LPConfiguration;
import me.lucko.luckperms.api.data.DatastoreConfiguration;
import me.lucko.luckperms.api.data.MySQLConfiguration;

import java.util.Map;

/**
 * Provides a link between {@link LPConfiguration} and {@link me.lucko.luckperms.core.LPConfiguration}
 */
@AllArgsConstructor
public class LPConfigurationLink implements LPConfiguration {
    private final me.lucko.luckperms.core.LPConfiguration master;

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
        return master.getIncludeGlobalPerms();
    }

    @Override
    public boolean getIncludeGlobalWorldPerms() {
        return master.getIncludeGlobalWorldPerms();
    }

    @Override
    public boolean getApplyGlobalGroups() {
        return master.getApplyGlobalGroups();
    }

    @Override
    public boolean getApplyGlobalWorldGroups() {
        return master.getApplyGlobalWorldGroups();
    }

    @Override
    public boolean getOnlineMode() {
        return master.getOnlineMode();
    }

    @Override
    public boolean getApplyWildcards() {
        return master.getApplyWildcards();
    }

    @Override
    public boolean getApplyRegex() {
        return master.getApplyRegex();
    }

    @Override
    public boolean getApplyShorthand() {
        return master.getApplyShorthand();
    }

    @Override
    public boolean getLogNotify() {
        return master.getLogNotify();
    }

    @Override
    public boolean getDebugPermissionChecks() {
        return master.getDebugPermissionChecks();
    }

    @Override
    public boolean getEnableOps() {
        return master.getEnableOps();
    }

    @Override
    public boolean getCommandsAllowOp() {
        return master.getCommandsAllowOp();
    }

    @Override
    public boolean getAutoOp() {
        return master.getAutoOp();
    }

    @Override
    public String getVaultServer() {
        return master.getVaultServer();
    }

    @Override
    public boolean getVaultIncludeGlobal() {
        return master.getVaultIncludeGlobal();
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
        return master.getSplitStorage();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> getSplitStorageOptions() {
        return master.getSplitStorageOptions();
    }
}
