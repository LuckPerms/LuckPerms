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

package me.lucko.luckperms;

import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.implementation.ApiProvider;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.core.LPConfiguration;
import me.lucko.luckperms.core.UuidCache;
import me.lucko.luckperms.data.Importer;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.storage.Datastore;
import me.lucko.luckperms.tracks.TrackManager;
import me.lucko.luckperms.users.UserManager;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Main internal interface for LuckPerms plugins, allowing the luckperms-common module to bind with the plugin instance.
 * All plugin platforms implement this interface.
 */
public interface LuckPermsPlugin {

    /*
     * Access to all of the main internal manager classes
     */
    UserManager getUserManager();
    GroupManager getGroupManager();
    TrackManager getTrackManager();
    LPConfiguration getConfiguration();
    Datastore getDatastore();
    Logger getLog();
    UuidCache getUuidCache();
    ApiProvider getApiProvider();
    Importer getImporter();

    /**
     * @return the version of the plugin
     */
    String getVersion();

    /**
     * @return the main plugin directory
     */
    File getMainDir();

    /**
     * @return the platforms data folder
     */
    File getDataFolder();

    /**
     * Returns a colored string indicating the status of a player
     * @param uuid The player's uuid
     * @return a formatted status string
     */
    Message getPlayerStatus(UUID uuid);

    /**
     * Gets the number of users online on the platform
     * @return the number of users
     */
    int getPlayerCount();

    /**
     * Gets the usernames of the users online on the platform
     * @return a {@link List} of usernames
     */
    List<String> getPlayerList();

    /**
     * @return a {@link List} of senders online on the platform
     */
    List<Sender> getSenders();

    /**
     * @return the console sender of the instance
     */
    Sender getConsoleSender();

    /**
     * Gets all possible permission nodes, used for resolving wildcards
     * @return a {@link List} of permission nodes
     */
    List<String> getPossiblePermissions();

    /**
     * Gets a set of players ignoring logging output
     * @return a {@link Set} of uuids
     */
    Set<UUID> getIgnoringLogs();

    /**
     * Runs an update task
     */
    void runUpdateTask();

    /**
     * Execute a runnable asynchronously
     * @param r the task to run
     */
    void doAsync(Runnable r);

    /**
     * Execute a runnable synchronously
     * @param r the task to run
     */
    void doSync(Runnable r);
}
