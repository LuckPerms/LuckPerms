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

import me.lucko.luckperms.api.event.LPListener;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The root API interface in LuckPerms
 */
@SuppressWarnings("unused")
public interface LuckPermsApi {

    /**
     * Schedules an update task to run
     */
    void runUpdateTask();

    /**
     * @return the version of the plugin running on the platform
     */
    String getVersion();

    /**
     * Registers a listener to be sent LuckPerms events
     * @param listener the listener instance
     */
    void registerListener(LPListener listener);

    /**
     * Unregisters a previously registered listener from the EventBus
     * @param listener the listener instance to unregister
     */
    void unregisterListener(LPListener listener);

    /**
     * Gets a wrapped {@link LPConfiguration} instance, with read only access
     * @return a configuration instance
     */
    LPConfiguration getConfiguration();

    /**
     * Gets a wrapped {@link Datastore} instance, with somewhat limited access
     * @return a datastore instance
     */
    Datastore getDatastore();

    /**
     * Gets the {@link Logger} wrapping used by the platform
     * @return the logger instance
     */
    Logger getLogger();

    /**
     * Gets a wrapped {@link UuidCache} instance, providing read access to the LuckPerms internal uuid caching system
     * @return a uuidcache instance
     */
    UuidCache getUuidCache();

    /**
     * Gets a wrapped user object from the user storage
     * @param uuid the uuid of the user to get
     * @return a {@link User} object, if one matching the uuid is loaded, or null if not
     */
    User getUser(UUID uuid);

    /**
     * Gets a wrapped user object from the user storage. This method does not return null, unlike {@link #getUser(UUID)}
     * @param uuid the uuid of the user to get
     * @return an optional {@link User} object
     */
    Optional<User> getUserSafe(UUID uuid);

    /**
     * Gets a wrapped user object from the user storage
     * @param name the username of the user to get
     * @return a {@link User} object, if one matching the uuid is loaded, or null if not
     */
    User getUser(String name);

    /**
     * Gets a wrapped user object from the user storage. This method does not return null, unlike {@link #getUser(String)}
     * @param name the username of the user to get
     * @return an optional {@link User} object
     */
    Optional<User> getUserSafe(String name);

    /**
     * Gets a set of all loaded users.
     * @return a {@link Set} of {@link User} objects
     */
    Set<User> getUsers();

    /**
     * Check if a user is loaded in memory
     * @param uuid the uuid to check for
     * @return true if the user is loaded
     */
    boolean isUserLoaded(UUID uuid);

    /**
     * Gets a wrapped group object from the group storage
     * @param name the name of the group to get
     * @return a {@link Group} object, if one matching the name exists, or null if not
     */
    Group getGroup(String name);

    /**
     * Gets a wrapped group object from the group storage. This method does not return null, unlike {@link #getGroup}
     * @param name the name of the group to get
     * @return an optional {@link Group} object
     */
    Optional<Group> getGroupSafe(String name);

    /**
     * Gets a set of all loaded groups.
     * @return a {@link Set} of {@link Group} objects
     */
    Set<Group> getGroups();

    /**
     * Check if a group is loaded in memory
     * @param name the name to check for
     * @return true if the group is loaded
     */
    boolean isGroupLoaded(String name);

    /**
     * Gets a wrapped track object from the track storage
     * @param name the name of the track to get
     * @return a {@link Track} object, if one matching the name exists, or null if not
     */
    Track getTrack(String name);

    /**
     * Gets a wrapped tracj object from the track storage. This method does not return null, unlike {@link #getTrack}
     * @param name the name of the track to get
     * @return an optional {@link Track} object
     */
    Optional<Track> getTrackSafe(String name);

    /**
     * Gets a set of all loaded tracks.
     * @return a {@link Set} of {@link Track} objects
     */
    Set<Track> getTracks();

    /**
     * Check if a track is loaded in memory
     * @param name the name to check for
     * @return true if the track is loaded
     */
    boolean isTrackLoaded(String name);

}
