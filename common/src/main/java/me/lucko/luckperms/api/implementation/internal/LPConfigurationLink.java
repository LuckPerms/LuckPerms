package me.lucko.luckperms.api.implementation.internal;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.api.LPConfiguration;
import me.lucko.luckperms.api.data.MySQLConfiguration;

/**
 * Provides a link between {@link me.lucko.luckperms.api.LPConfiguration} and {@link me.lucko.luckperms.utils.LPConfiguration}
 */
@AllArgsConstructor
public class LPConfigurationLink implements LPConfiguration {
    private final me.lucko.luckperms.utils.LPConfiguration master;

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
    public boolean getOnlineMode() {
        return master.getOnlineMode();
    }

    @Override
    public MySQLConfiguration getDatabaseValues() {
        return master.getDatabaseValues();
    }

    @Override
    public String getStorageMethod() {
        return master.getStorageMethod();
    }
}
