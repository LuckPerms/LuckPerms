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
 * Read-only access to the LuckPerms configuration settings
 */
public interface LPConfiguration {

    /**
     * Returns the name of this server
     * @return the name of this server
     */
    String getServer();

    /**
     * Returns how often a sync task will run in minutes
     * @return how often a sync task will run in minutes
     */
    int getSyncTime();

    /**
     * Returns the default group, in a node representation
     * @return the default group, in a node representation
     * @deprecated as of 2.6, the default group is always "default"
     */
    @Deprecated
    String getDefaultGroupNode();

    /**
     * Returns the name of the default group
     * @return the name of the default group
     * @deprecated as of 2.6, the default group is always "default"
     */
    @Deprecated
    String getDefaultGroupName();

    /**
     * Returns if the users on this server will have their global permissions applied
     * @return if the users on this server will have their global permissions applied
     */
    boolean getIncludeGlobalPerms();

    /**
     * Returns if the users on this server will have their global world permissions applied
     * @return if the users on this server will have their global world permissions applied
     * @since 2.9
     */
    boolean getIncludeGlobalWorldPerms();

    /**
     * Returns true if the platform is applying global groups
     * @return true if the platform is applying global groups
     * @since 2.9
     */
    boolean getApplyGlobalGroups();

    /**
     * Returns true if the platform is applying global world groups
     * @return true if the platform is applying global world groups
     * @since 2.9
     */
    boolean getApplyGlobalWorldGroups();

    /**
     * Returns the online mode setting
     * @return the online mode setting
     */
    boolean getOnlineMode();

    /**
     * Returns if LuckPerms is applying wildcard permissions
     * @return if LuckPerms is applying wildcard permissions
     */
    boolean getApplyWildcards();

    /**
     * Returns if LuckPerms is resolving and applying regex permissions
     * @return if LuckPerms is resolving and applying regex permissions
     */
    boolean getApplyRegex();

    /**
     * Returns if LuckPerms is expanding shorthand permissions
     * @return if LuckPerms is expanding shorthand permissions
     */
    boolean getApplyShorthand();

    /**
     * Returns if LuckPerms will send notifications to users when permissions are modified
     * @return if LuckPerms will send notifications to users when permissions are modified
     * @since 2.7
     */
    boolean getLogNotify();

    /**
     * Returns true if permission checks are being recorded / debugged
     * @return true if permission checks are being recorded / debugged
     * @since 2.9
     * @deprecated as this value is now always false. Functionality was replaced by the verbose command.
     */
    @Deprecated
    boolean getDebugPermissionChecks();

    /**
     * Returns true if the vanilla op system is enabled
     * @return true if the vanilla op system is enabled
     * @since 2.8
     */
    boolean getEnableOps();

    /**
     * Returns true if opped players are allowed to use LuckPerms commands
     * @return true if opped players are allowed to use LuckPerms commands
     * @since 2.8
     */
    boolean getCommandsAllowOp();

    /**
     * Returns true if auto op is enabled
     * @return true if auto op is enabled
     * @since 2.9
     */
    boolean getAutoOp();

    /**
     * Returns the name of the server used within Vault operations
     * @return the name of the server used within Vault operations
     * @since 2.7
     */
    String getVaultServer();

    /**
     * Returns true if global permissions should be considered when retrieving meta or player groups
     * @return true if global permissions should be considered when retrieving meta or player groups
     * @since 2.7
     */
    boolean getVaultIncludeGlobal();

    /**
     * Returns the database values set in the configuration
     * @return the database values set in the configuration
     * @deprecated use {@link #getDatastoreConfig()}
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    MySQLConfiguration getDatabaseValues();

    /**
     * Returns the values set for data storage in the configuration
     * @return the values set for data storage in the configuration
     */
    DatastoreConfiguration getDatastoreConfig();

    /**
     * Returns the storage method string from the configuration
     * @return the storage method string from the configuration
     */
    String getStorageMethod();

    /**
     * Returns true if split storage is enabled
     * @return true if split storage is enabled
     * @since 2.7
     */
    boolean getSplitStorage();

    /**
     * Returns a map of split storage options
     * @return a map of split storage options, where the key is the storage section, and the value is the storage
     * method. For example: key = user, value = json
     * @since 2.7
     */
    Map<String, String> getSplitStorageOptions();

}
