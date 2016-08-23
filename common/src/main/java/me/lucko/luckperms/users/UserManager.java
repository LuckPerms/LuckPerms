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

package me.lucko.luckperms.users;

import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.utils.AbstractManager;
import me.lucko.luckperms.utils.Identifiable;

import java.util.NoSuchElementException;
import java.util.UUID;

@RequiredArgsConstructor
public abstract class UserManager extends AbstractManager<UUID, User> {
    private final LuckPermsPlugin plugin;

    /**
     * Get a user object by name
     * @param name The name to search by
     * @return a {@link User} object if the user is loaded, returns null if the user is not loaded
     */
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public User get(String name) {
        try {
            return objects.values().stream()
                    .filter(u -> u.getName().equalsIgnoreCase(name))
                    .limit(1).findAny().get();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    @Override
    public void copy(User from, User to) {
        to.setNodes(from.getNodes());
        to.setPrimaryGroup(from.getPrimaryGroup());
        to.refreshPermissions();
    }

    /**
     * Set a user to the default group
     * @param user the user to give to
     */
    public void giveDefaults(User user) {
        // Setup the new user with default values
        try {
            user.setPermission(plugin.getConfiguration().getDefaultGroupNode(), true);
        } catch (ObjectAlreadyHasException ignored) {}
        user.setPrimaryGroup(plugin.getConfiguration().getDefaultGroupName());
    }

    /**
     * Checks to see if the user is online, and if they are not, runs {@link #unload(Identifiable)}
     * @param user The user to be cleaned up
     */
    public abstract void cleanup(User user);

    /**
     * Makes a new {@link User} object
     * @param uuid The UUID of the user
     * @param username The username of the user
     * @return a new {@link User} object
     */
    public abstract User make(UUID uuid, String username);

    /**
     * Reloads the data of all online users
     */
    public abstract void updateAllUsers();
}
