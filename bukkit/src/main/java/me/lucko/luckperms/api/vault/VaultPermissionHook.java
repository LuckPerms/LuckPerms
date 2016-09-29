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

package me.lucko.luckperms.api.vault;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import me.lucko.luckperms.LPBukkitPlugin;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.api.vault.cache.ContextData;
import me.lucko.luckperms.api.vault.cache.VaultUserCache;
import me.lucko.luckperms.api.vault.cache.VaultUserManager;
import me.lucko.luckperms.contexts.Contexts;
import me.lucko.luckperms.core.PermissionHolder;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.users.User;
import net.milkbowl.vault.permission.Permission;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class VaultPermissionHook extends Permission {

    @Getter
    @Setter
    private LPBukkitPlugin plugin;

    @Getter
    private VaultUserManager vaultUserManager;

    @Getter
    @Setter
    private String server = "global";

    @Getter
    @Setter
    private boolean includeGlobal = true;

    @Override
    public String getName() {
        return "LuckPerms";
    }

    @Override
    public boolean isEnabled() {
        return plugin.getDatastore().isAcceptingLogins();
    }

    @Override
    public boolean hasSuperPermsCompat() {
        return true;
    }

    public void setup() {
        vaultUserManager = new VaultUserManager(plugin, this);
    }

    public void log(String s) {
        if (plugin.getConfiguration().isDebugPermissionChecks()) {
            plugin.getLog().info("[VAULT] " + s);
        }
    }

    private boolean objectHas(String world, Group group, String permission) {
        if (group == null) return false;

        Map<String, String> context = new HashMap<>();
        if (world != null && !world.equals("")) {
            context.put("world", world);
        }
        context.put("server", server);

        Map<String, Boolean> toApply = group.exportNodes(
                new Contexts(context, includeGlobal, includeGlobal, true, true, true),
                Collections.emptyList(), true
        );

        return toApply.containsKey(permission) && toApply.get(permission);
    }

    private boolean objectAdd(String world, PermissionHolder object, String permission) {
        if (object == null) return false;

        try {
            if (world != null && !world.equals("")) {
                object.setPermission(permission, true, server, world);
            } else {
                object.setPermission(permission, true, server);
            }
        } catch (ObjectAlreadyHasException ignored) {}

        objectSave(object);
        return true;
    }

    private boolean objectRemove(String world, PermissionHolder object, String permission) {
        if (object == null) return false;

        try {
            if (world != null && !world.equals("")) {
                object.unsetPermission(permission, server, world);
            } else {
                object.unsetPermission(permission, server);
            }
        } catch (ObjectLacksException ignored) {}

        objectSave(object);
        return true;
    }

    void objectSave(PermissionHolder t) {
        if (t instanceof User) {
            ((User) t).refreshPermissions();
            plugin.getDatastore().saveUser(((User) t), Callback.empty());
        }
        if (t instanceof Group) {
            plugin.getDatastore().saveGroup(((Group) t), c -> plugin.runUpdateTask());
        }
    }

    @Override
    public boolean playerHas(String world, @NonNull String player, @NonNull String permission) {
        log("Checking if player " + player + " has permission: " + permission + " on world " + world + ", server " + server);
        User user = plugin.getUserManager().get(player);
        if (user == null) return false;

        if (!vaultUserManager.containsUser(user.getUuid())) {
            return false;
        }

        VaultUserCache vaultUser = vaultUserManager.getUser(user.getUuid());
        Map<String, String> context = new HashMap<>();
        context.put("server", server);
        if (world != null) {
            context.put("world", world);
        }

        return vaultUser.hasPermission(context, permission);
    }

    @Override
    public boolean playerAdd(String world, @NonNull String player, @NonNull String permission) {
        log("Adding permission to player " + player + ": '" + permission + "' on world " + world + ", server " + server);
        final User user = plugin.getUserManager().get(player);
        return objectAdd(world, user, permission);
    }

    @Override
    public boolean playerRemove(String world, @NonNull String player, @NonNull String permission) {
        log("Removing permission from player " + player + ": '" + permission + "' on world " + world + ", server " + server);
        final User user = plugin.getUserManager().get(player);
        return objectRemove(world, user, permission);
    }

    @Override
    public boolean groupHas(String world, @NonNull String groupName, @NonNull String permission) {
        log("Checking if group " + groupName + " has permission: " + permission + " on world " + world + ", server " + server);
        final Group group = plugin.getGroupManager().get(groupName);
        return objectHas(world, group, permission);
    }

    @Override
    public boolean groupAdd(String world, @NonNull String groupName, @NonNull String permission) {
        log("Adding permission to group " + groupName + ": '" + permission + "' on world " + world + ", server " + server);
        final Group group = plugin.getGroupManager().get(groupName);
        return objectAdd(world, group, permission);
    }

    @Override
    public boolean groupRemove(String world, @NonNull String groupName, @NonNull String permission) {
        log("Removing permission from group " + groupName + ": '" + permission + "' on world " + world + ", server " + server);
        final Group group = plugin.getGroupManager().get(groupName);
        return objectRemove(world, group, permission);
    }

    @Override
    public boolean playerInGroup(String world, @NonNull String player, @NonNull String group) {
        log("Checking if player " + player + " is in group: " + group + " on world " + world + ", server " + server);
        return playerHas(world, player, "group." + group);
    }

    @Override
    public boolean playerAddGroup(String world, @NonNull String player, @NonNull String groupName) {
        log("Adding player " + player + " to group: '" + groupName + "' on world " + world + ", server " + server);
        final User user = plugin.getUserManager().get(player);
        if (user == null) return false;

        final Group group = plugin.getGroupManager().get(groupName);
        if (group == null) return false;

        try {
            if (world != null && !world.equals("")) {
                user.addGroup(group, server, world);
            } else {
                user.addGroup(group, server);
            }
        } catch (ObjectAlreadyHasException ignored) {}
        objectSave(user);
        return true;
    }

    @Override
    public boolean playerRemoveGroup(String world, @NonNull String player, @NonNull String groupName) {
        log("Removing player " + player + " from group: '" + groupName + "' on world " + world + ", server " + server);
        final User user = plugin.getUserManager().get(player);
        if (user == null) return false;

        final Group group = plugin.getGroupManager().get(groupName);
        if (group == null) return false;

        try {
            if (world != null && !world.equals("")) {
                user.removeGroup(group, server, world);
            } else {
                user.removeGroup(group, server);
            }
        } catch (ObjectLacksException ignored) {}
        objectSave(user);
        return true;
    }

    @Override
    public String[] getPlayerGroups(String world, @NonNull String player) {
        log("Getting groups of player: " + player + ", on world " + world + ", server " + server);
        User user = plugin.getUserManager().get(player);
        if (user == null) return new String[0];

        if (!vaultUserManager.containsUser(user.getUuid())) {
            return new String[0];
        }

        VaultUserCache vaultUser = vaultUserManager.getUser(user.getUuid());
        Map<String, String> context = new HashMap<>();
        context.put("server", server);
        if (world != null) {
            context.put("world", world);
        }

        ContextData cd = vaultUser.getContextData().computeIfAbsent(context, map -> vaultUser.calculatePermissions(map, false));
        return cd.getPermissionCache().entrySet().stream()
                .filter(Map.Entry::getValue)
                .filter(e -> e.getKey().startsWith("group."))
                .map(e -> e.getKey().substring("group.".length()))
                .toArray(String[]::new);
    }

    @Override
    public String getPrimaryGroup(String world, @NonNull String player) {
        log("Getting primary group of player: " + player);
        final User user = plugin.getUserManager().get(player);
        return (user == null) ? null : user.getPrimaryGroup();
    }

    @Override
    public String[] getGroups() {
        return plugin.getGroupManager().getAll().keySet().toArray(new String[0]);
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }
}
