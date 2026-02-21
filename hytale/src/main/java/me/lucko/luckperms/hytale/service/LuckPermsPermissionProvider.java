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

package me.lucko.luckperms.hytale.service;

import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.hypixel.hytale.server.core.permissions.provider.PermissionProvider;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.hytale.LPHytalePlugin;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.util.Tristate;
import org.jspecify.annotations.NonNull;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LuckPermsPermissionProvider implements PermissionProvider {
    private static final String PERMISSION_PROVIDER_NAME = "LuckPerms";

    /** LuckPerms plugin */
    private final LPHytalePlugin plugin;
    /** The Hytale built-in permission provider implementation */
    private final HytalePermissionsProvider hytaleProvider;
    /** The map of player virtual groups */
    private final PlayerVirtualGroupsMap playerVirtualGroupsMap;
    /** Whether to delegate operations on groups/users not managed by LuckPerms to the built-in Hytale provider */
    private final boolean delegateToHytaleProvider;
    /** A set of UUIDs of users who have been delegated to the Hytale provider */
    private final Set<UUID> delegatedUsers = ConcurrentHashMap.newKeySet();

    public LuckPermsPermissionProvider(LPHytalePlugin plugin, HytalePermissionsProvider hytaleProvider, PlayerVirtualGroupsMap playerVirtualGroupsMap) {
        this.plugin = plugin;
        this.hytaleProvider = hytaleProvider;
        this.playerVirtualGroupsMap = playerVirtualGroupsMap;
        this.delegateToHytaleProvider = hytaleProvider != null && plugin.getConfiguration().get(ConfigKeys.DELEGATE_TO_HYTALE_PERMISSIONS_PROVIDER);
    }

    public HytalePermissionsProvider getHytaleProvider() {
        return this.hytaleProvider;
    }

    @Override
    public String getName() {
        return PERMISSION_PROVIDER_NAME;
    }

    @Override
    public Set<String> getUserPermissions(@NonNull UUID userUniqueId) {
        User user = this.plugin.getUserManager().getIfLoaded(userUniqueId);
        if (user != null) {
            return new LuckPermsPermissionsSet(user);
        } else if (this.delegateToHytaleProvider) {
            if (this.delegatedUsers.add(userUniqueId)) {
                this.plugin.getLogger().warn("LuckPerms does not have permissions data loaded for user '" + userUniqueId + "', so their checks will be delegated to the Hytale provider.");
            }
            return this.hytaleProvider.getUserPermissions(userUniqueId);
        } else {
            return Set.of();
        }
    }

    @Override
    public void addUserToGroup(@NonNull UUID userUniqueId, @NonNull String groupName) {
        this.playerVirtualGroupsMap.addPlayerToGroup(userUniqueId, groupName);
        if (this.delegateToHytaleProvider) {
            this.hytaleProvider.addUserToGroup(userUniqueId, groupName);
        }
    }

    @Override
    public void removeUserFromGroup(@NonNull UUID userUniqueId, @NonNull String groupName) {
        this.playerVirtualGroupsMap.removePlayerFromGroup(userUniqueId, groupName);
        if (this.delegateToHytaleProvider) {
            this.hytaleProvider.removeUserFromGroup(userUniqueId, groupName);
        }
    }

    @Override
    public Set<String> getGroupsForUser(@NonNull UUID userUniqueId) {
        Set<String> virtualGroups = this.playerVirtualGroupsMap.getPlayerGroups(userUniqueId);

        User user = this.plugin.getUserManager().getIfLoaded(userUniqueId);
        if (user != null) {
            Set<String> groups = new HashSet<>(virtualGroups);
            for (InheritanceNode node : user.getOwnInheritanceNodes(user.getQueryOptions())) {
                groups.add(node.getGroupName());
            }
            return groups;

        } else if (this.delegateToHytaleProvider) {
            if (this.delegatedUsers.add(userUniqueId)) {
                this.plugin.getLogger().warn("LuckPerms does not have permissions data loaded for user '" + userUniqueId + "', so their checks will be delegated to the Hytale provider.");
            }

            Set<String> groups = new HashSet<>(virtualGroups);
            groups.addAll(this.hytaleProvider.getGroupsForUser(userUniqueId));
            return groups;

        } else {
            return virtualGroups;
        }
    }

    @Override
    public void addUserPermissions(@NonNull UUID userUniqueId, @NonNull Set<String> permissions) {
        if (this.delegateToHytaleProvider) {
            this.hytaleProvider.addUserPermissions(userUniqueId, permissions);
        }
    }

    @Override
    public void removeUserPermissions(@NonNull UUID userUniqueId, @NonNull Set<String> permissions) {
        if (this.delegateToHytaleProvider) {
            this.hytaleProvider.removeUserPermissions(userUniqueId, permissions);
        }
    }

    @Override
    public void addGroupPermissions(@NonNull String groupName, @NonNull Set<String> permissions) {
        if (this.delegateToHytaleProvider) {
            this.hytaleProvider.addGroupPermissions(groupName, permissions);
        }
    }

    @Override
    public void removeGroupPermissions(@NonNull String groupName, @NonNull Set<String> permissions) {
        if (this.delegateToHytaleProvider) {
            this.hytaleProvider.removeGroupPermissions(groupName, permissions);
        }
    }

    @Override
    public Set<String> getGroupPermissions(@NonNull String groupName) {
        return this.delegateToHytaleProvider
                ? this.hytaleProvider.getGroupPermissions(groupName)
                : Set.of();
    }

    /**
     * A permissions set that tricks {@link PermissionsModule#hasPermission(Set, String)} into always
     * returning according to LuckPerms data.
     */
    private static final class LuckPermsPermissionsSet extends AbstractSet<String> {
        private static final String WILDCARD_PERMISSION = "*";
        private static final String NEGATIVE_WILDCARD_PERMISSION = "-*";

        private final User user;

        private LuckPermsPermissionsSet(User user) {
            this.user = user;
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof String permission)) {
                throw new IllegalArgumentException("Not a string: " + o);
            }

            if (WILDCARD_PERMISSION.equals(permission) || NEGATIVE_WILDCARD_PERMISSION.equals(permission)) {
                return false; // let LuckPerms handle wildcards itself
            }

            boolean inverted = false;
            if (!permission.isEmpty() && permission.charAt(0) == '-') {
                inverted = true;
                permission = permission.substring(1);
            }

            Tristate result = this.user.getCachedData().getPermissionData().checkPermission(permission, CheckOrigin.PLATFORM_API_HAS_PERMISSION).result();
            return inverted != result.asBoolean();
        }

        @Override
        public Iterator<String> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }
    }
}
