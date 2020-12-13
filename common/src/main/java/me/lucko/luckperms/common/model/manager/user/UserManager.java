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

package me.lucko.luckperms.common.model.manager.user;

import me.lucko.luckperms.common.calculator.PermissionCalculator;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.Manager;

import net.luckperms.api.node.Node;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserManager<T extends User> extends Manager<UUID, User, T> {

    T getOrMake(UUID id, String username);

    /**
     * Get a user object by name
     *
     * @param name The name to search by
     * @return a {@link User} object if the user is loaded, returns null if the user is not loaded
     */
    T getByUsername(String name);

    /**
     * Gives the user the default group if necessary.
     *
     * @param user the user to give to
     */
    boolean giveDefaultIfNeeded(User user);

    /**
     * Check whether the user's state indicates that they should be persisted to storage.
     *
     * @param user the user to check
     * @return true if the user should be saved
     */
    boolean isNonDefaultUser(User user);

    /**
     * Gets whether the given node is a default node given by {@link #giveDefaultIfNeeded(User)}.
     *
     * @param node the node
     * @return true if it is the default node
     */
    boolean isDefaultNode(Node node);

    /**
     * Gets the instance responsible for unloading unneeded users.
     *
     * @return the housekeeper
     */
    UserHousekeeper getHouseKeeper();

    /**
     * Reloads the data of all *online* users
     */
    CompletableFuture<Void> loadAllUsers();

    /**
     * Invalidates the cached data for *loaded* users.
     */
    void invalidateAllUserCaches();

    /**
     * Invalidates the {@link PermissionCalculator}s for *loaded* users.
     */
    void invalidateAllPermissionCalculators();

}
