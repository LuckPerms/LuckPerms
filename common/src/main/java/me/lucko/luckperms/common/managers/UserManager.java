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

package me.lucko.luckperms.common.managers;

import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.references.Identifiable;
import me.lucko.luckperms.common.references.UserIdentifier;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserManager extends Manager<UserIdentifier, User> {

    /**
     * Get a user object by name
     *
     * @param name The name to search by
     * @return a {@link User} object if the user is loaded, returns null if the user is not loaded
     */
    User getByUsername(String name);

    /**
     * Get a user object by uuid
     *
     * @param uuid The uuid to search by
     * @return a {@link User} object if the user is loaded, returns null if the user is not loaded
     */
    User getIfLoaded(UUID uuid);

    /**
     * Gives the user the default group if necessary.
     *
     * @param user the user to give to
     */
    boolean giveDefaultIfNeeded(User user, boolean save);

    /**
     * Checks to see if the user is online, and if they are not, runs {@link #unload(Identifiable)}
     *
     * @param user The user to be cleaned up
     */
    boolean cleanup(User user);

    /**
     * Schedules a task to cleanup a user after a certain period of time, if they're not on the server anymore.
     *
     * @param uuid external uuid of the player
     */
    void scheduleUnload(UUID uuid);

    /**
     * Reloads the data of all online users
     */
    CompletableFuture<Void> updateAllUsers();

}
