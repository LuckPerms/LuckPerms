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
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.references.UserIdentifier;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class GenericUserManager extends AbstractManager<UserIdentifier, User> implements UserManager {

    private final LuckPermsPlugin plugin;

    @Override
    public User getOrMake(UserIdentifier id) {
        User ret = super.getOrMake(id);
        if (id.getUsername().isPresent()) {
            ret.setName(id.getUsername().get(), false);
        }
        return ret;
    }

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
    public boolean cleanup(User user) {
        if (!plugin.isPlayerOnline(plugin.getUuidCache().getExternalUUID(user.getUuid()))) {
            unload(user);
            return true;
        } else {
            return false;
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
                        user.getCachedData().invalidateCaches();
                        unload(user);
                        plugin.getUuidCache().clearCache(uuid);
                    }
                }, 40L);
            }
        }, 40L);
    }

    @Override
    public CompletableFuture<Void> updateAllUsers() {
        return CompletableFuture.runAsync(
                () -> plugin.getOnlinePlayers()
                        .map(u -> plugin.getUuidCache().getUUID(u))
                        .forEach(u -> plugin.getStorage().loadUser(u, null).join()),
                plugin.getScheduler().async()
        );
    }

    public static boolean giveDefaultIfNeeded(User user, boolean save, LuckPermsPlugin plugin) {
        boolean work = false;

        // check that they are actually a member of their primary group, otherwise remove it
        if (plugin.getConfiguration().get(ConfigKeys.PRIMARY_GROUP_CALCULATION_METHOD).equals("stored")) {
            String pg = user.getPrimaryGroup().getValue();
            boolean has = false;

            for (Node node : user.getEnduringNodes().get(ImmutableContextSet.empty())) {
                if (node.isGroupNode() && node.getGroupName().equalsIgnoreCase(pg)) {
                    has = true;
                    break;
                }
            }

            // need to find a new primary group for the user.
            if (!has) {
                String group = user.getEnduringNodes().get(ImmutableContextSet.empty()).stream()
                        .filter(Node::isGroupNode)
                        .findFirst()
                        .map(Node::getGroupName)
                        .orElse(null);

                // if the group is null, it'll be resolved in the next step
                if (group != null) {
                    user.getPrimaryGroup().setStoredValue(group);
                    work = true;
                }
            }
        }

        // check that all users are member of at least one group
        boolean hasGroup = false;
        if (user.getPrimaryGroup().getStoredValue().isPresent()) {
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

        if (!hasGroup) {
            user.getPrimaryGroup().setStoredValue(NodeFactory.DEFAULT_GROUP_NAME);
            user.setPermission(NodeFactory.buildGroupNode(NodeFactory.DEFAULT_GROUP_NAME).build());
            work = true;
        }

        if (work && save) {
            plugin.getStorage().saveUser(user);
        }

        return work;
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

            if (!node.getGroupName().equalsIgnoreCase(NodeFactory.DEFAULT_GROUP_NAME)) {
                // The user's only node is not the default group one.
                return true;
            }
        }

        // Not in the default primary group
        return !user.getPrimaryGroup().getStoredValue().orElse(NodeFactory.DEFAULT_GROUP_NAME).equalsIgnoreCase(NodeFactory.DEFAULT_GROUP_NAME);
    }
}
