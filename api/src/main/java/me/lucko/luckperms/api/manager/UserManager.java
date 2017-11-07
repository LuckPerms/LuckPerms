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

package me.lucko.luckperms.api.manager;

import me.lucko.luckperms.api.User;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents the object responsible for managing {@link User} instances.
 *
 * @since 4.0
 */
public interface UserManager {

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
     * @param uuid the uuid of the user to get
     * @return an optional {@link User} object
     * @throws NullPointerException if the uuid is null
     */
    @Nonnull
    default Optional<User> getUserOpt(@Nonnull UUID uuid) {
        return Optional.ofNullable(getUser(uuid));
    }

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
     * @param name the username of the user to get
     * @return an optional {@link User} object
     * @throws NullPointerException if the name is null
     */
    @Nonnull
    default Optional<User> getUserOpt(@Nonnull String name) {
        return Optional.ofNullable(getUser(name));
    }

    /**
     * Gets a set of all loaded users.
     *
     * @return a {@link Set} of {@link User} objects
     */
    @Nonnull
    Set<User> getLoadedUsers();

    /**
     * Check if a user is loaded in memory
     *
     * @param uuid the uuid to check for
     * @return true if the user is loaded
     * @throws NullPointerException if the uuid is null
     */
    boolean isLoaded(@Nonnull UUID uuid);

    /**
     * Unload a user from the internal storage, if they're not currently online.
     *
     * @param user the user to unload
     * @throws NullPointerException if the user is null
     */
    void cleanupUser(@Nonnull User user);

}
