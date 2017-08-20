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

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.references.UserIdentifier;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class GenericUserManager extends AbstractManager<UserIdentifier, User> implements UserManager {
    public static boolean giveDefaultIfNeeded(User user, boolean save, LuckPermsPlugin plugin) {
        boolean hasGroup = false;

        if (user.getPrimaryGroup().getStoredValue() != null && !user.getPrimaryGroup().getStoredValue().isEmpty()) {
            for (Node node : user.getEnduringNodes().values()) {
                if (node.hasSpecificContext()) {
                    continue;
                }

                if (node.isGroupNode()) {
                    hasGroup = true;
                    break;
                }
            }
        }

        if (hasGroup) {
            return false;
        }

        user.getPrimaryGroup().setStoredValue("default");
        user.setPermission(NodeFactory.make("group.default"));

        if (save) {
            plugin.getStorage().saveUser(user);
        }

        return true;
    }

    @Override
    public User getOrMake(UserIdentifier id) {
        User ret = super.getOrMake(id);
        if (id.getUsername().isPresent()) {
            ret.setName(id.getUsername().get(), false);
        }
        return ret;
    }

    /**
     * Check whether the user's state indicates that they should be persisted to storage.
     *
     * @param user the user to check
     * @return true if the user should be saved
     */
    public static boolean shouldSave(User user) {
        if (user.getEnduringNodes().size() != 1) {
            return true;
        }

        for (Node node : user.getEnduringNodes().values()) {
            // There's only one.
            if (!node.isGroupNode()) {
                return true;
            }

            if (node.isTemporary() || node.isServerSpecific() || node.isWorldSpecific()) {
                return true;
            }

            if (!node.getGroupName().equalsIgnoreCase("default")) {
                // The user's only node is not the default group one.
                return true;
            }
        }

        // Not in the default primary group
        return !user.getPrimaryGroup().getStoredValue().equalsIgnoreCase("default");
    }

    private final LuckPermsPlugin plugin;

    @Override
    public User apply(UserIdentifier id) {
        return !id.getUsername().isPresent() ?
                new User(id.getUuid(), plugin) :
                new User(id.getUuid(), id.getUsername().get(), plugin);
    }

    @Override
    public User getByUsername(String name) {
        for (User user : getAll().values()) {
            Optional<String> n = user.getName();
            if (n.isPresent() && n.get().equalsIgnoreCase(name)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public User getIfLoaded(UUID uuid) {
        return getIfLoaded(UserIdentifier.of(uuid, null));
    }

    @Override
    public boolean giveDefaultIfNeeded(User user, boolean save) {
        return giveDefaultIfNeeded(user, save, plugin);
    }

    @Override
    public void cleanup(User user) {
        if (!plugin.isPlayerOnline(plugin.getUuidCache().getExternalUUID(user.getUuid()))) {
            unload(user);
        }
    }

    @Override
    public void scheduleUnload(UUID uuid) {
        plugin.getScheduler().asyncLater(() -> {
            // check once to see if the user can be unloaded.
            if (getIfLoaded(plugin.getUuidCache().getUUID(uuid)) != null && !plugin.isPlayerOnline(uuid)) {

                // check again in 40 ticks, we want to be sure the player won't have re-logged before we unload them.
                plugin.getScheduler().asyncLater(() -> {
                    User user = getIfLoaded(plugin.getUuidCache().getUUID(uuid));
                    if (user != null && !plugin.isPlayerOnline(uuid)) {
                        user.unregisterData();
                        unload(user);
                        plugin.getUuidCache().clearCache(uuid);
                    }
                }, 40L);
            }
        }, 40L);
    }

    @Override
    public void updateAllUsers() {
        plugin.doSync(() -> {
            Set<UUID> players = plugin.getOnlinePlayers();
            plugin.doAsync(() -> {
                for (UUID uuid : players) {
                    UUID internal = plugin.getUuidCache().getUUID(uuid);
                    plugin.getStorage().loadUser(internal, "null").join();
                }
            });
        });
    }
}
