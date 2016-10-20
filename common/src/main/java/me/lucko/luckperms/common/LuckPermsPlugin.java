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

package me.lucko.luckperms.common;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.PlatformType;
import me.lucko.luckperms.common.api.ApiProvider;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.commands.ConsecutiveExecutor;
import me.lucko.luckperms.common.commands.Sender;
import me.lucko.luckperms.common.config.LPConfiguration;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.contexts.ContextManager;
import me.lucko.luckperms.common.core.UuidCache;
import me.lucko.luckperms.common.data.Importer;
import me.lucko.luckperms.common.groups.GroupManager;
import me.lucko.luckperms.common.storage.Datastore;
import me.lucko.luckperms.common.tracks.TrackManager;
import me.lucko.luckperms.common.users.UserManager;
import me.lucko.luckperms.common.utils.BufferedRequest;
import me.lucko.luckperms.common.utils.LocaleManager;

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
    ConsecutiveExecutor getConsecutiveExecutor();
    LocaleManager getLocaleManager();
    ContextManager getContextManager();
    CalculatorFactory getCalculatorFactory();

    /**
     * @return the version of the plugin
     */
    String getVersion();

    /**
     * @return the platform type
     */
    PlatformType getType();

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
    default Message getPlayerStatus(UUID uuid) {
        UUID external = getUuidCache().getExternalUUID(uuid);
        return isOnline(external) ? Message.PLAYER_ONLINE : Message.PLAYER_OFFLINE;
    }

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
     * Gets the UUIDs of the users online on the platform
     * @return a {@link Set} of UUIDs
     */
    Set<UUID> getOnlinePlayers();

    /**
     * Checks if a user is online
     * @param external the users external uuid
     * @return true if the user is online
     */
    boolean isOnline(UUID external);

    /**
     * @return a {@link List} of senders online on the platform
     */
    List<Sender> getNotifyListeners();

    /**
     * @return the console sender of the instance
     */
    Sender getConsoleSender();

    /**
     * Gets a set of Contexts that should be pre-processed in advance
     * @param op if the user being processed is op
     * @return a set of contexts
     */
    Set<Contexts> getPreProcessContexts(boolean op);

    /**
     * Gets a set of players ignoring logging output
     * @return a {@link Set} of uuids
     */
    Set<UUID> getIgnoringLogs();

    /**
     * Gets a loaded plugins instance from the platform
     * @param name the name of the plugin
     * @return a plugin instance
     */
    Object getPlugin(String name);

    /**
     * Gets a provided service from the platform.
     * @param clazz the class of the service
     * @return the service instance, if it is provided for
     */
    Object getService(Class clazz);

    /**
     * Used as a backup for migration
     * @param playerName the players name
     * @return a uuid if found, or null if not
     */
    UUID getUUID(String playerName);

    /**
     * Checks if a plugin is loaded on the platform
     * @param name the name of the plugin
     * @return true if the plugin is loaded
     */
    boolean isPluginLoaded(String name);

    /**
     * Runs an update task
     */
    BufferedRequest<Void> getUpdateTaskBuffer();

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

    /**
     * Execute a runnable asynchronously on a loop
     * @param r the task to run
     * @param interval the time between runs in ticks
     */
    void doAsyncRepeating(Runnable r, long interval);

}
