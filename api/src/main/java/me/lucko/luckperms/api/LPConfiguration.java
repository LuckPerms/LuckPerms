package me.lucko.luckperms.api;

import me.lucko.luckperms.api.data.MySQLConfiguration;

/**
 * A wrapper interface for the internal LuckPerms configuration, providing read only access.
 */
public interface LPConfiguration {

    /**
     * @return the name of this server
     */
    String getServer();

    /**
     * @return how often a sync task will run in minutes
     */
    int getSyncTime();

    /**
     * @return the default group, in a node representation
     */
    String getDefaultGroupNode();

    /**
     * @return the name of the default group
     */
    String getDefaultGroupName();

    /**
     * @return if the users on this server will have their global permissions applied
     */
    boolean getIncludeGlobalPerms();

    /**
     * @return the online mode setting in the config
     */
    boolean getOnlineMode();

    /**
     * @return the database values set in the configuration
     */
    MySQLConfiguration getDatabaseValues();

    /**
     * @return the storage method string from the configuration
     */
    String getStorageMethod();

}
