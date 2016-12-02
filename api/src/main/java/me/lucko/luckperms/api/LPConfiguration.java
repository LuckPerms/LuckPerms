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

package me.lucko.luckperms.api;

import me.lucko.luckperms.api.data.DatastoreConfiguration;
import me.lucko.luckperms.api.data.MySQLConfiguration;

import java.util.Map;

/**
 * A wrapper interface for the internal LuckPerms configuration, providing read only access.
 */
@SuppressWarnings("unused")
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
     * @deprecated as of 2.6, the default group is always "default"
     */
    @Deprecated
    String getDefaultGroupNode();

    /**
     * @return the name of the default group
     * @deprecated as of 2.6, the default group is always "default"
     */
    @Deprecated
    String getDefaultGroupName();

    /**
     * @return if the users on this server will have their global permissions applied
     */
    boolean getIncludeGlobalPerms();

    /**
     * @return if the users on this server will have their global world permissions applied
     * @since 2.9
     */
    boolean getIncludeGlobalWorldPerms();

    /**
     * @return true if the platform is applying global groups
     * @since 2.9
     */
    boolean getApplyGlobalGroups();

    /**
     * @return true if the platform is applying global world groups
     * @since 2.9
     */
    boolean getApplyGlobalWorldGroups();

    /**
     * @return the online mode setting in the config
     */
    boolean getOnlineMode();

    /**
     * @return if LuckPerms is applying wildcard permissions
     */
    boolean getApplyWildcards();

    /**
     * @return if LuckPerms is resolving and applying regex permissions
     */
    boolean getApplyRegex();

    /**
     * @return if LuckPerms is expanding shorthand permissions
     */
    boolean getApplyShorthand();

    /**
     * @return if LuckPerms will send notifications to users when permissions are modified
     * @since 2.7
     */
    boolean getLogNotify();

    /**
     * @return true if permission checks are being recorded / debugged
     * @since 2.9
     * @deprecated as this value is now always false. Functionality was replaced by the verbose command.
     */
    @Deprecated
    boolean getDebugPermissionChecks();

    /**
     * @return true if the vanilla op system is enabled
     * @since 2.8
     */
    boolean getEnableOps();

    /**
     * @return true if opped players are allowed to use LuckPerms commands
     * @since 2.8
     */
    boolean getCommandsAllowOp();

    /**
     * @return true if auto op is enabled
     * @since 2.9
     */
    boolean getAutoOp();

    /**
     * @return the name of the server used within Vault operations
     * @since 2.7
     */
    String getVaultServer();

    /**
     * @return true if global permissions should be considered when retrieving meta or player groups
     * @since 2.7
     */
    boolean getVaultIncludeGlobal();

    /**
     * @return the database values set in the configuration
     * @deprecated use {@link #getDatastoreConfig()}
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    MySQLConfiguration getDatabaseValues();

    /**
     * @return the values set for data storage in the configuration
     */
    DatastoreConfiguration getDatastoreConfig();

    /**
     * @return the storage method string from the configuration
     */
    String getStorageMethod();

    /**
     * @return true if split storage is enabled
     * @since 2.7
     */
    boolean getSplitStorage();

    /**
     * @return a map of split storage options, where the key is the storage section, and the value is the storage
     * method. For example: key = user, value = json
     * @since 2.7
     */
    Map<String, String> getSplitStorageOptions();

}
