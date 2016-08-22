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

}
