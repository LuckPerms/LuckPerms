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
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.event.EventBus;
import me.lucko.luckperms.api.metastacking.MetaStackFactory;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
     * Schedules an update task to run
     */
    void runUpdateTask();

    /**
     * Gets the API version
     *
     * @return the version of the API running on the platform
     * @since 2.6
     */
    double getApiVersion();

    /**
     * Gets the plugin version
     *
     * @return the version of the plugin running on the platform
     */
    @Nonnull
    String getVersion();

    /**
     * Gets the platform LuckPerms is running on
     *
     * @return the platform LuckPerms is running on
     * @since 2.7
     */
    @Nonnull
    PlatformType getPlatformType();

    /**
     * Gets the event bus, used for subscribing to events
     *
     * @return the event bus
     * @since 3.0
     */
    @Nonnull
    EventBus getEventBus();

    /**
     * Gets a wrapped {@link LPConfiguration} instance, with read only access
     *
     * @return a configuration instance
     */
    @Nonnull
    LPConfiguration getConfiguration();

    /**
     * Gets a wrapped {@link Storage} instance.
     *
     * @return a storage instance
     * @since 2.14
     */
    @Nonnull
    Storage getStorage();

    /**
     * Gets the messaging service in use on the platform, if present.
     *
     * @return an optional that may contain a messaging service instance.
     */
    @Nonnull
    Optional<MessagingService> getMessagingService();

    /**
     * Gets the {@link Logger} wrapping used by the platform
     *
     * @return the logger instance
     */
    @Nonnull
    Logger getLogger();

    /**
     * Gets a wrapped {@link UuidCache} instance, providing read access to the LuckPerms internal uuid caching system
     *
     * @return a uuid cache instance
     */
    @Nonnull
    UuidCache getUuidCache();

    /**
     * Gets a wrapped user object from the user storage
     *
     * @param uuid the uuid of the user to get
     * @return a {@link User} object, if one matching the uuid is loaded, or null if not
     * @throws NullPointerException if the uuid is null
     */
    @Nullable
    User getUser(@Nonnull UUID uuid);

    /**
     * Gets a wrapped user object from the user storage.
     *
     * <p>This method does not return null, unlike {@link #getUser(UUID)}</p>
     *
     * @param uuid the uuid of the user to get
     * @return an optional {@link User} object
     * @throws NullPointerException if the uuid is null
     */
    @Nonnull
    Optional<User> getUserSafe(@Nonnull UUID uuid);

    /**
     * Gets a wrapped user object from the user storage
     *
     * @param name the username of the user to get
     * @return a {@link User} object, if one matching the uuid is loaded, or null if not
     * @throws NullPointerException if the name is null
     */
    @Nullable
    User getUser(@Nonnull String name);

    /**
     * Gets a wrapped user object from the user storage.
     *
     * <p>This method does not return null, unlike {@link #getUser(String)}</p>
     *
     * @param name the username of the user to get
     * @return an optional {@link User} object
     * @throws NullPointerException if the name is null
     */
    @Nonnull
    Optional<User> getUserSafe(@Nonnull String name);

    /**
     * Gets a set of all loaded users.
     *
     * @return a {@link Set} of {@link User} objects
     */
    @Nonnull
    Set<User> getUsers();

    /**
     * Check if a user is loaded in memory
     *
     * @param uuid the uuid to check for
     * @return true if the user is loaded
     * @throws NullPointerException if the uuid is null
     */
    boolean isUserLoaded(@Nonnull UUID uuid);

    /**
     * Unload a user from the internal storage, if they're not currently online.
     *
     * @param user the user to unload
     * @throws NullPointerException if the user is null
     * @since 2.6
     */
    void cleanupUser(@Nonnull User user);

    /**
     * Gets a wrapped group object from the group storage
     *
     * @param name the name of the group to get
     * @return a {@link Group} object, if one matching the name exists, or null if not
     * @throws NullPointerException if the name is null
     */
    @Nullable
    Group getGroup(@Nonnull String name);

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
    Optional<Group> getGroupSafe(@Nonnull String name);

    /**
     * Gets a set of all loaded groups.
     *
     * @return a {@link Set} of {@link Group} objects
     */
    @Nonnull
    Set<Group> getGroups();

    /**
     * Check if a group is loaded in memory
     *
     * @param name the name to check for
     * @return true if the group is loaded
     * @throws NullPointerException if the name is null
     */
    boolean isGroupLoaded(@Nonnull String name);

    /**
     * Gets a wrapped track object from the track storage
     *
     * @param name the name of the track to get
     * @return a {@link Track} object, if one matching the name exists, or null if not
     * @throws NullPointerException if the name is null
     */
    @Nullable
    Track getTrack(@Nonnull String name);

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
    Optional<Track> getTrackSafe(@Nonnull String name);

    /**
     * Gets a set of all loaded tracks.
     *
     * @return a {@link Set} of {@link Track} objects
     */
    @Nonnull
    Set<Track> getTracks();

    /**
     * Check if a track is loaded in memory
     *
     * @param name the name to check for
     * @return true if the track is loaded
     * @throws NullPointerException if the name is null
     */
    boolean isTrackLoaded(@Nonnull String name);

    /**
     * Gets the node factory instance for the platform
     *
     * @return the node factory
     */
    @Nonnull
    NodeFactory getNodeFactory();

    /**
     * Gets the MetaStackFactory, used for creating MetaStacks.
     *
     * @return the meta stack factory
     * @since 3.2
     */
    @Nonnull
    MetaStackFactory getMetaStackFactory();

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
    Node.Builder buildNode(@Nonnull String permission) throws IllegalArgumentException;

    /**
     * Register a custom context calculator to the server
     *
     * @param contextCalculator the context calculator to register. The type MUST be the player class of the platform.
     * @throws ClassCastException if the type is not the player class of the platform.
     */
    void registerContextCalculator(@Nonnull ContextCalculator<?> contextCalculator);

    /**
     * Gets a calculated context instance for the user using the rules of the platform.
     *
     * <p> These values are calculated using the options in the configuration, and the provided calculators.
     *
     * @param user the user to get contexts for
     * @return an optional containing contexts. Will return empty if the user is not online.
     */
    @Nonnull
    Optional<Contexts> getContextForUser(@Nonnull User user);

    /**
     * Gets set of contexts applicable to a player using the platforms {@link ContextCalculator}s.
     *
     * @param player the player to calculate for. Must be the player instance for the platform.
     * @return a set of contexts.
     * @since 2.17
     */
    @Nonnull
    ContextSet getContextForPlayer(@Nonnull Object player);

    /**
     * Gets a Contexts instance for the player using the platforms {@link ContextCalculator}s.
     *
     * @param player the player to calculate for. Must be the player instance for the platform.
     * @return a set of contexts.
     * @since 3.3
     */
    @Nonnull
    Contexts getContextsForPlayer(@Nonnull Object player);

    /**
     * Gets the unique players which have connected to the server since it started.
     *
     * @return the unique connections
     * @since 3.3
     */
    @Nonnull
    Set<UUID> getUniqueConnections();

    /**
     * Gets the time when the plugin first started in milliseconds.
     *
     * @return the enable time
     * @since 3.3
     */
    long getStartTime();

}
