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

package me.lucko.luckperms.api;

import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.api.context.ContextManager;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.event.EventBus;
import me.lucko.luckperms.api.manager.GroupManager;
import me.lucko.luckperms.api.manager.TrackManager;
import me.lucko.luckperms.api.manager.UserManager;
import me.lucko.luckperms.api.metastacking.MetaStackFactory;
import me.lucko.luckperms.api.platform.PlatformInfo;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The LuckPerms API.
 *
 * <p>This interface is the base of the entire API package. All API functions
 * are accessed via this interface.</p>
 *
 * <p>An instance can be obtained via {@link LuckPerms#getApi()}, or the platforms
 * Services Manager.</p>
 */
public interface LuckPermsApi {

    /**
     * Gets information about the platform LuckPerms is running on.
     *
     * @return the platform info
     * @since 4.0
     */
    @Nonnull
    PlatformInfo getPlatformInfo();

    /**
     * Gets the {@link UserManager}, responsible for managing
     * {@link User} instances.
     *
     * <p>This manager can be used to retrieve instances of {@link User} by uuid
     * or name, or query all loaded users.</p>
     *
     * <p>The {@link #getStorage() storage} instance should be used to
     * load/create/save users.</p>
     *
     * @return the user manager
     * @since 4.0
     */
    @Nonnull
    UserManager getUserManager();

    /**
     * Gets the {@link GroupManager}, responsible for managing
     * {@link Group} instances.
     *
     * <p>This manager can be used to retrieve instances of {@link Group} by
     * name, or query all loaded groups.</p>
     *
     * <p>The {@link #getStorage() storage} instance should be used to
     * load/create/save/delete groups.</p>
     *
     * @return the group manager
     * @since 4.0
     */
    @Nonnull
    GroupManager getGroupManager();

    /**
     * Gets the {@link TrackManager}, responsible for managing
     * {@link Track} instances.
     *
     * <p>This manager can be used to retrieve instances of {@link Track} by
     * name, or query all loaded tracks.</p>
     *
     * <p>The {@link #getStorage() storage} instance should be used to
     * load/create/save/delete tracks.</p>
     *
     * @return the track manager
     * @since 4.0
     */
    @Nonnull
    TrackManager getTrackManager();

    /**
     * Schedules the execution of an update task, and returns an encapsulation
     * of the task as a {@link CompletableFuture}.
     *
     * <p>The exact actions performed in an update task remains an
     * implementation detail of the plugin, however, as a minimum, it is
     * expected to perform a full reload of user, group and track data, and
     * ensure that any changes are fully applied and propagated.</p>
     *
     * @return a future
     * @since 4.0
     */
    @Nonnull
    CompletableFuture<Void> runUpdateTask();

    /**
     * Gets the {@link EventBus}, used for subscribing to internal LuckPerms
     * events.
     *
     * @return the event bus
     * @since 3.0
     */
    @Nonnull
    EventBus getEventBus();

    /**
     * Gets a representation of the plugins configuration
     *
     * @return the configuration
     */
    @Nonnull
    LPConfiguration getConfiguration();

    /**
     * Gets an object representing the plugins primary {@link Storage} backend.
     *
     * <p>The instance propagates calls to the internal DAO (Data Access Object),
     * and applies any changes to the storage provider.</p>
     *
     * @return a storage instance
     * @since 2.14
     */
    @Nonnull
    Storage getStorage();

    /**
     * Gets the {@link MessagingService}, if present.
     *
     * <p>The MessagingService is used to dispatch updates throughout a network
     * of servers running the plugin.</p>
     *
     * <p>Not all instances of LuckPerms will have a messaging service setup and
     * configured, but it is recommended that all users of the API account for
     * and make use of this.</p>
     *
     * @return the messaging service instance, if present.
     */
    @Nonnull
    Optional<MessagingService> getMessagingService();

    /**
     * Gets the {@link ActionLogger}.
     *
     * <p>The action logger is responsible for saving and broadcasting defined
     * actions occurring on the platform.</p>
     *
     * @return the action logger
     * @since 4.1
     */
    ActionLogger getActionLogger();

    /**
     * Gets a {@link UuidCache}.
     *
     * <p>The uuid cache provides read access to the internal LuckPerms uuid
     * mapping system.</p>
     *
     * @return the uuid cache
     */
    @Nonnull
    UuidCache getUuidCache();

    /**
     * Gets the {@link ContextManager}.
     *
     * <p>The context manager manages {@link ContextCalculator}s, and calculates
     * applicable contexts for a given type.</p>
     *
     * @return the context manager
     * @since 4.0
     */
    ContextManager getContextManager();

    /**
     * Gets the {@link NodeFactory}.
     *
     * <p>The node factory provides methods for building {@link Node} instances.</p>
     *
     * @return the node factory
     */
    @Nonnull
    NodeFactory getNodeFactory();

    /**
     * Gets the {@link MetaStackFactory}.
     *
     * <p>The metastack factory provides methods for retrieving
     * {@link me.lucko.luckperms.api.metastacking.MetaStackElement}s and constructing
     * {@link me.lucko.luckperms.api.metastacking.MetaStackDefinition}s.</p>
     *
     * @return the meta stack factory
     * @since 3.2
     */
    @Nonnull
    MetaStackFactory getMetaStackFactory();




    /*
     * The following methods are provided only for convenience, and offer no
     * additional functionality.
     *
     * They are implemented as "default" methods, using the manager and factory
     * instances provided by the methods above.
     */



    /**
     * Gets a wrapped user object from the user storage
     *
     * @param uuid the uuid of the user to get
     * @return a {@link User} object, if one matching the uuid is loaded, or null if not
     * @throws NullPointerException if the uuid is null
     */
    @Nullable
    default User getUser(@Nonnull UUID uuid) {
        return getUserManager().getUser(uuid);
    }

    /**
     * Gets a wrapped user object from the user storage.
     *
     * @param uuid the uuid of the user to get
     * @return an optional {@link User} object
     * @throws NullPointerException if the uuid is null
     */
    @Nonnull
    default Optional<User> getUserSafe(@Nonnull UUID uuid) {
        return getUserManager().getUserOpt(uuid);
    }

    /**
     * Gets a wrapped user object from the user storage
     *
     * @param name the username of the user to get
     * @return a {@link User} object, if one matching the uuid is loaded, or null if not
     * @throws NullPointerException if the name is null
     */
    @Nullable
    default User getUser(@Nonnull String name) {
        return getUserManager().getUser(name);
    }

    /**
     * Gets a wrapped user object from the user storage.
     *
     * @param name the username of the user to get
     * @return an optional {@link User} object
     * @throws NullPointerException if the name is null
     */
    @Nonnull
    default Optional<User> getUserSafe(@Nonnull String name) {
        return getUserManager().getUserOpt(name);
    }

    /**
     * Gets a set of all loaded users.
     *
     * @return a {@link Set} of {@link User} objects
     */
    @Nonnull
    default Set<User> getUsers() {
        return getUserManager().getLoadedUsers();
    }

    /**
     * Check if a user is loaded in memory
     *
     * @param uuid the uuid to check for
     * @return true if the user is loaded
     * @throws NullPointerException if the uuid is null
     */
    default boolean isUserLoaded(@Nonnull UUID uuid) {
        return getUserManager().isLoaded(uuid);
    }

    /**
     * Unload a user from the internal storage, if they're not currently online.
     *
     * @param user the user to unload
     * @throws NullPointerException if the user is null
     */
    default void cleanupUser(@Nonnull User user) {
        getUserManager().cleanupUser(user);
    }

    /**
     * Gets a wrapped group object from the group storage
     *
     * @param name the name of the group to get
     * @return a {@link Group} object, if one matching the name exists, or null if not
     * @throws NullPointerException if the name is null
     */
    @Nullable
    default Group getGroup(@Nonnull String name) {
        return getGroupManager().getGroup(name);
    }

    /**
     * Gets a wrapped group object from the group storage.
     *
     * <p>This method does not return null, unlike {@link #getGroup}</p>
     *
     * @param name the name of the group to get
     * @return an optional {@link Group} object
     * @throws NullPointerException if the name is null
     */
    @Nonnull
    default Optional<Group> getGroupSafe(@Nonnull String name) {
        return getGroupManager().getGroupOpt(name);
    }

    /**
     * Gets a set of all loaded groups.
     *
     * @return a {@link Set} of {@link Group} objects
     */
    @Nonnull
    default Set<Group> getGroups() {
        return getGroupManager().getLoadedGroups();
    }

    /**
     * Check if a group is loaded in memory
     *
     * @param name the name to check for
     * @return true if the group is loaded
     * @throws NullPointerException if the name is null
     */
    default boolean isGroupLoaded(@Nonnull String name) {
        return getGroupManager().isLoaded(name);
    }

    /**
     * Gets a wrapped track object from the track storage
     *
     * @param name the name of the track to get
     * @return a {@link Track} object, if one matching the name exists, or null
     * if not
     * @throws NullPointerException if the name is null
     */
    @Nullable
    default Track getTrack(@Nonnull String name) {
        return getTrackManager().getTrack(name);
    }

    /**
     * Gets a wrapped track object from the track storage.
     *
     * <p>This method does not return null, unlike {@link #getTrack}</p>
     *
     * @param name the name of the track to get
     * @return an optional {@link Track} object
     * @throws NullPointerException if the name is null
     */
    @Nonnull
    default Optional<Track> getTrackSafe(@Nonnull String name) {
        return getTrackManager().getTrackOpt(name);
    }

    /**
     * Gets a set of all loaded tracks.
     *
     * @return a {@link Set} of {@link Track} objects
     */
    @Nonnull
    default Set<Track> getTracks() {
        return getTrackManager().getLoadedTracks();
    }

    /**
     * Check if a track is loaded in memory
     *
     * @param name the name to check for
     * @return true if the track is loaded
     * @throws NullPointerException if the name is null
     */
    default boolean isTrackLoaded(@Nonnull String name) {
        return getTrackManager().isLoaded(name);
    }

    /**
     * Returns a new LogEntry Builder instance
     *
     * @return a new builder
     * @since 4.0
     */
    @Nonnull
    default LogEntry.Builder newLogEntryBuilder() {
        return getActionLogger().newEntryBuilder();
    }

    /**
     * Returns a permission builder instance
     *
     * @param permission the main permission node to build
     * @return a {@link Node.Builder} instance
     * @throws IllegalArgumentException if the permission is invalid
     * @throws NullPointerException     if the permission is null
     * @since 2.6
     */
    @Nonnull
    default Node.Builder buildNode(@Nonnull String permission) throws IllegalArgumentException {
        return getNodeFactory().newBuilder(permission);
    }

    /**
     * Register a custom context calculator to the server
     *
     * @param calculator the context calculator to register. The type MUST be the player class of the platform.
     * @throws ClassCastException if the type is not the player class of the platform.
     */
    default void registerContextCalculator(@Nonnull ContextCalculator<?> calculator) {
        getContextManager().registerCalculator(calculator);
    }

    /**
     * Gets a calculated context instance for the user using the rules of the platform.
     *
     * <p> These values are calculated using the options in the configuration, and the provided calculators.
     *
     * @param user the user to get contexts for
     * @return an optional containing contexts. Will return empty if the user is not online.
     */
    @Nonnull
    default Optional<Contexts> getContextForUser(@Nonnull User user) {
        return getContextManager().lookupApplicableContexts(user);
    }

    /**
     * Gets set of contexts applicable to a player using the platforms {@link ContextCalculator}s.
     *
     * @param player the player to calculate for. Must be the player instance for the platform.
     * @return a set of contexts.
     * @since 2.17
     */
    @Nonnull
    default ContextSet getContextForPlayer(@Nonnull Object player) {
        return getContextManager().getApplicableContext(player);
    }

    /**
     * Gets a Contexts instance for the player using the platforms {@link ContextCalculator}s.
     *
     * @param player the player to calculate for. Must be the player instance for the platform.
     * @return a set of contexts.
     * @since 3.3
     */
    @Nonnull
    default Contexts getContextsForPlayer(@Nonnull Object player) {
        return getContextManager().getApplicableContexts(player);
    }

}
