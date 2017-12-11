/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
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

package me.lucko.luckperms.common.plugin;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.platform.PlatformType;
import me.lucko.luckperms.common.actionlog.LogDispatcher;
import me.lucko.luckperms.common.api.ApiProvider;
import me.lucko.luckperms.common.buffers.BufferedRequest;
import me.lucko.luckperms.common.caching.handlers.CachedStateManager;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.commands.abstraction.Command;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.contexts.ContextManager;
import me.lucko.luckperms.common.dependencies.DependencyManager;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.logging.Logger;
import me.lucko.luckperms.common.managers.GroupManager;
import me.lucko.luckperms.common.managers.TrackManager;
import me.lucko.luckperms.common.managers.UserManager;
import me.lucko.luckperms.common.messaging.ExtendedMessagingService;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.storage.Storage;
import me.lucko.luckperms.common.storage.dao.file.FileWatcher;
import me.lucko.luckperms.common.treeview.PermissionVault;
import me.lucko.luckperms.common.utils.UuidCache;
import me.lucko.luckperms.common.verbose.VerboseHandler;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Main internal interface for LuckPerms plugins, providing the base for abstraction throughout the project.
 *
 * All plugin platforms implement this interface.
 */
public interface LuckPermsPlugin {

    /**
     * Gets the user manager instance for the platform
     *
     * @return the user manager
     */
    UserManager getUserManager();

    /**
     * Gets the group manager instance for the platform
     *
     * @return the group manager
     */
    GroupManager getGroupManager();

    /**
     * Gets the track manager instance for the platform
     *
     * @return the track manager
     */
    TrackManager getTrackManager();

    /**
     * Gets the plugin's configuration
     *
     * @return the plugin config
     */
    LuckPermsConfiguration getConfiguration();

    /**
     * Gets the primary data storage instance. This is likely to be wrapped with extra layers for caching, etc.
     *
     * @return the storage handler instance
     */
    Storage getStorage();

    /**
     * Gets the redis messaging instance.
     *
     * @return the redis messaging service
     */
    Optional<ExtendedMessagingService> getMessagingService();

    /**
     * Gets a wrapped logger instance for the platform.
     *
     * @return the plugin's logger
     */
    Logger getLog();

    /**
     * Gets the UUID caching store for the platform
     *
     * @return the uuid cache
     */
    UuidCache getUuidCache();

    /**
     * Returns the class implementing the LuckPermsAPI on this platform.
     *
     * @return the api
     */
    ApiProvider getApiProvider();

    /**
     * Gets the command manager
     *
     * @return the command manager
     */
    CommandManager getCommandManager();

    /**
     * Gets the instance providing locale translations for the plugin
     *
     * @return the locale manager
     */
    LocaleManager getLocaleManager();

    /**
     * Gets the dependency manager for the plugin
     *
     * @return the dependency manager
     */
    DependencyManager getDependencyManager();

    /**
     * Gets the context manager.
     * This object handles context accumulation for all players on the platform.
     *
     * @return the context manager
     */
    ContextManager<?> getContextManager();

    /**
     * Gets the cached state manager for the platform.
     *
     * @return the cached state manager
     */
    CachedStateManager getCachedStateManager();

    /**
     * Gets the class responsible for constructing PermissionCalculators on this platform.
     *
     * @return the permission calculator factory
     */
    CalculatorFactory getCalculatorFactory();

    /**
     * Gets the verbose debug handler instance.
     *
     * @return the debug handler instance
     */
    VerboseHandler getVerboseHandler();

    /**
     * Gets the permission caching instance for the platform.
     *
     * @return the permission cache instance
     */
    PermissionVault getPermissionVault();

    /**
     * Gets the log dispatcher running on the platform
     *
     * @return the log dispatcher
     */
    LogDispatcher getLogDispatcher();

    /**
     * Gets the LuckPerms Scheduler instance
     *
     * @return the scheduler
     */
    SchedulerAdapter getScheduler();

    /**
     * Gets the file watcher running on the platform
     *
     * @return the file watcher
     */
    Optional<FileWatcher> getFileWatcher();

    /**
     * Gets a string of the plugin's version
     *
     * @return the version of the plugin
     */
    String getVersion();

    /**
     * Gets the platform type this instance of LuckPerms is running on.
     *
     * @return the platform type
     */
    PlatformType getServerType();

    /**
     * Gets the name or "brand" of the running platform
     *
     * @return the server brand
     */
    String getServerBrand();

    /**
     * Gets the version of the running platform
     *
     * @return the server version
     */
    String getServerVersion();

    /**
     * Gets the name associated with this server
     *
     * @return the server name
     */
    default String getServerName() {
        return null;
    }

    /**
     * Gets the time when the plugin first started in millis.
     *
     * @return the enable time
     */
    long getStartTime();

    /**
     * Gets the plugins main data storage directory
     *
     * <p>Bukkit: /root/plugins/LuckPerms</p>
     * <p>Bungee: /root/plugins/LuckPerms</p>
     * <p>Sponge: /root/luckperms/</p>
     *
     * @return the platforms data folder
     */
    File getDataDirectory();

    /**
     * Gets the plugins config directory.
     *
     * <p>This is the same as {@link #getDataDirectory()} on Bukkit/Bungee, but different on Sponge.</p>
     *
     * @return the platforms config folder
     */
    default File getConfigDirectory() {
        return getDataDirectory();
    }

    /**
     * Gets a bundled resource file from the jar
     *
     * @param path the path of the file
     * @return the file as an input stream
     */
    InputStream getResourceStream(String path);

    /**
     * Returns a colored string indicating the status of a player
     *
     * @param uuid The player's uuid
     * @return a formatted status string
     */
    default Message getPlayerStatus(UUID uuid) {
        UUID external = getUuidCache().getExternalUUID(uuid);
        return isPlayerOnline(external) ? Message.PLAYER_ONLINE : Message.PLAYER_OFFLINE;
    }

    /**
     * Gets a player object linked to this User. The returned object must be the same type
     * as the instance used in the platforms {@link ContextManager}
     *
     * @param user the user instance
     * @return a player object, or null, if one couldn't be found.
     */
    Object getPlayer(User user);

    /**
     * Lookup a uuid from a username, using the servers internal uuid cache.
     *
     * @param username the username to lookup
     * @return an optional uuid, if found
     */
    Optional<UUID> lookupUuid(String username);

    /**
     * Gets a calculated context instance for the user using the rules of the platform.
     *
     * @param user the user instance
     * @return a contexts object, or null if one couldn't be generated
     */
    Contexts getContextForUser(User user);

    /**
     * Gets the number of users online on the platform
     *
     * @return the number of users
     */
    int getPlayerCount();

    /**
     * Gets the usernames of the users online on the platform
     *
     * @return a {@link List} of usernames
     */
    Stream<String> getPlayerList();

    /**
     * Gets the UUIDs of the users online on the platform
     *
     * @return a {@link Set} of UUIDs
     */
    Stream<UUID> getOnlinePlayers();

    /**
     * Checks if a user is online
     *
     * @param external the users external uuid
     * @return true if the user is online
     */
    boolean isPlayerOnline(UUID external);

    /**
     * Gets a list of online Senders on the platform
     *
     * @return a {@link List} of senders online on the platform
     */
    Stream<Sender> getOnlineSenders();

    /**
     * Gets the console.
     *
     * @return the console sender of the instance
     */
    Sender getConsoleSender();

    /**
     * Gets the unique players which have connected to the server since it started.
     *
     * @return the unique connections
     */
    Set<UUID> getUniqueConnections();

    default List<Command> getExtraCommands() {
        return Collections.emptyList();
    }

    /**
     * Gets a map of extra information to be shown in the info command
     *
     * @return a map of options
     */
    default Map<String, Object> getExtraInfo() {
        return Collections.emptyMap();
    }

    /**
     * Gets the update task buffer of the platform, used for scheduling and running update tasks.
     *
     * @return the update task buffer instance
     */
    BufferedRequest<Void> getUpdateTaskBuffer();

    /**
     * Called at the end of the sync task.
     */
    default void onPostUpdate() {

    }

    static void sendStartupBanner(Sender sender, LuckPermsPlugin plugin) {
        sender.sendMessage(CommandUtils.color("&b               __       &3 __   ___  __         __  "));
        sender.sendMessage(CommandUtils.color("&b    |    |  | /  ` |__/ &3|__) |__  |__)  |\\/| /__` "));
        sender.sendMessage(CommandUtils.color("&b    |___ \\__/ \\__, |  \\ &3|    |___ |  \\  |  | .__/ "));
        sender.sendMessage(CommandUtils.color(" "));
        sender.sendMessage(CommandUtils.color("&2  Loading version &bv" + plugin.getVersion() + "&2 on " + plugin.getServerType().getFriendlyName() + " - " + plugin.getServerBrand()));
        sender.sendMessage(CommandUtils.color("&8  Running on server version " + plugin.getServerVersion()));
        sender.sendMessage(CommandUtils.color(" "));
    }

}
