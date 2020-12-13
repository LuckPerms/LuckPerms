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

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.AbstractManager;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.verbose.event.MetaCheckEvent;

import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractUserManager<T extends User> extends AbstractManager<UUID, User, T> implements UserManager<T> {

    private final LuckPermsPlugin plugin;
    private final UserHousekeeper housekeeper;

    public AbstractUserManager(LuckPermsPlugin plugin, UserHousekeeper.TimeoutSettings timeoutSettings) {
        this.plugin = plugin;
        this.housekeeper = new UserHousekeeper(plugin, this, timeoutSettings);
        this.plugin.getBootstrap().getScheduler().asyncRepeating(this.housekeeper, 30, TimeUnit.SECONDS);
    }

    @Override
    public T getOrMake(UUID id, String username) {
        T user = getOrMake(id);
        if (username != null) {
            user.setUsername(username, false);
        }
        return user;
    }

    @Override
    public T getByUsername(String name) {
        for (T user : getAll().values()) {
            Optional<String> n = user.getUsername();
            if (n.isPresent() && n.get().equalsIgnoreCase(name)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public boolean giveDefaultIfNeeded(User user) {
        boolean requireSave = false;

        Collection<InheritanceNode> globalGroups = user.normalData().inheritanceNodesInContext(ImmutableContextSetImpl.EMPTY);

        // check that they are actually a member of their primary group, otherwise remove it
        if (this.plugin.getConfiguration().get(ConfigKeys.PRIMARY_GROUP_CALCULATION_METHOD).equals("stored")) {
            String primaryGroup = user.getCachedData().getMetaData(this.plugin.getConfiguration().get(ConfigKeys.GLOBAL_QUERY_OPTIONS)).getPrimaryGroup(MetaCheckEvent.Origin.INTERNAL);
            boolean memberOfPrimaryGroup = false;

            for (InheritanceNode node : globalGroups) {
                if (node.getGroupName().equalsIgnoreCase(primaryGroup)) {
                    memberOfPrimaryGroup = true;
                    break;
                }
            }

            // need to find a new primary group for the user.
            if (!memberOfPrimaryGroup) {
                String group = globalGroups.stream()
                        .findFirst()
                        .map(InheritanceNode::getGroupName)
                        .orElse(null);

                // if the group is null, it'll be resolved in the next step
                if (group != null) {
                    user.getPrimaryGroup().setStoredValue(group);
                    requireSave = true;
                }
            }
        }

        // check that all users are member of at least one group
        boolean hasGroup = false;
        if (user.getPrimaryGroup().getStoredValue().isPresent()) {
            hasGroup = !globalGroups.isEmpty();
        }

        if (!hasGroup) {
            user.getPrimaryGroup().setStoredValue(GroupManager.DEFAULT_GROUP_NAME);
            user.setNode(DataType.NORMAL, Inheritance.builder(GroupManager.DEFAULT_GROUP_NAME).build(), false);
            requireSave = true;
        }

        return requireSave;
    }

    @Override
    public boolean isNonDefaultUser(User user) {
        if (user.normalData().size() != 1) {
            return true;
        }

        List<Node> nodes = user.normalData().asList();
        if (nodes.size() != 1) {
            return true;
        }

        Node onlyNode = nodes.iterator().next();
        if (!isDefaultNode(onlyNode)) {
            return true;
        }

        // Not in the default primary group
        return !user.getPrimaryGroup().getStoredValue().orElse(GroupManager.DEFAULT_GROUP_NAME).equalsIgnoreCase(GroupManager.DEFAULT_GROUP_NAME);
    }

    @Override
    public boolean isDefaultNode(Node node) {
        return node instanceof InheritanceNode &&
                node.getValue() &&
                !node.hasExpiry() &&
                node.getContexts().isEmpty() &&
                ((InheritanceNode) node).getGroupName().equalsIgnoreCase(GroupManager.DEFAULT_GROUP_NAME);
    }

    @Override
    public UserHousekeeper getHouseKeeper() {
        return this.housekeeper;
    }

    @Override
    public CompletableFuture<Void> loadAllUsers() {
        Set<UUID> ids = new HashSet<>(getAll().keySet());
        ids.addAll(this.plugin.getBootstrap().getOnlinePlayers());

        CompletableFuture<?>[] loadTasks = ids.stream()
                .map(id -> this.plugin.getStorage().loadUser(id, null))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(loadTasks);
    }

    @Override
    public void invalidateAllUserCaches() {
        getAll().values().forEach(u -> u.getCachedData().invalidate());
    }

    @Override
    public void invalidateAllPermissionCalculators() {
        getAll().values().forEach(u -> u.getCachedData().invalidatePermissionCalculators());
    }

}
