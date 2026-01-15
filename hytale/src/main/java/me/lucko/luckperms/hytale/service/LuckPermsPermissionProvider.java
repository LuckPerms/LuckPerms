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
import com.hypixel.hytale.server.core.permissions.provider.PermissionProvider;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.hytale.LPHytalePlugin;
import net.luckperms.api.util.Tristate;
import org.jspecify.annotations.NonNull;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class LuckPermsPermissionProvider implements PermissionProvider {
    private final LPHytalePlugin plugin;
    private final PermissionProvider hytaleProvider;
    private final PlayerVirtualGroupsMap playerVirtualGroupsMap;

    public LuckPermsPermissionProvider(LPHytalePlugin plugin, PermissionProvider hytaleProvider, PlayerVirtualGroupsMap playerVirtualGroupsMap) {
        this.plugin = plugin;
        this.hytaleProvider = hytaleProvider;
        this.playerVirtualGroupsMap = playerVirtualGroupsMap;
    }

    public PermissionProvider getHytaleProvider() {
        return this.hytaleProvider;
    }

    @Override
    public String getName() {
        return "LuckPerms";
    }

    @Override
    public Set<String> getUserPermissions(@NonNull UUID uuid) {
        User user = this.plugin.getUserManager().getIfLoaded(uuid);
        if (user == null) {
            return Set.of();
        }
        return new HackyPermissionSet(user);
    }

    /**
     * A hack to trick {@link PermissionsModule#hasPermission(Set, String)} into always returning according
     * to LuckPerms data.
     */
    private static final class HackyPermissionSet extends AbstractSet<String> {
        private static final String WILDCARD_PERMISSION = "*";
        private static final String NEGATIVE_WILDCARD_PERMISSION = "-*";

        private final User user;

        private HackyPermissionSet(User user) {
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

    @Override
    public void addUserToGroup(@NonNull UUID uuid, @NonNull String group) {
        this.playerVirtualGroupsMap.addPlayerToGroup(uuid, group);
    }

    @Override
    public void removeUserFromGroup(@NonNull UUID uuid, @NonNull String group) {
        this.playerVirtualGroupsMap.removePlayerFromGroup(uuid, group);
    }

    @Override
    public Set<String> getGroupsForUser(@NonNull UUID uuid) {
        return this.playerVirtualGroupsMap.getPlayerGroups(uuid);
    }

    // TODO - implement fully?

    @Override
    public void addUserPermissions(@NonNull UUID uuid, @NonNull Set<String> set) {
        //this.plugin.getLogger().warn("addUserPermissions called for " + uuid + " with permissions: " + set, new Exception());
    }

    @Override
    public void removeUserPermissions(@NonNull UUID uuid, @NonNull Set<String> set) {
        //this.plugin.getLogger().warn("removeUserPermissions called for " + uuid + " with permissions: " + set, new Exception());
    }

    @Override
    public void addGroupPermissions(@NonNull String s, @NonNull Set<String> set) {
        //this.plugin.getLogger().warn("addGroupPermissions called for " + s + " with permissions: " + set, new Exception());
    }

    @Override
    public void removeGroupPermissions(@NonNull String s, @NonNull Set<String> set) {
        //this.plugin.getLogger().warn("removeGroupPermissions called for " + s + " with permissions: " + set, new Exception());
    }

    @Override
    public Set<String> getGroupPermissions(@NonNull String s) {
        //this.plugin.getLogger().warn("getGroupPermissions called for " + s, new Exception());
        return Set.of();
    }
}
