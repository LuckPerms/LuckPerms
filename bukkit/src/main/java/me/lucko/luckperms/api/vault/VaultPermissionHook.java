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

import lombok.NonNull;
import lombok.Setter;
import me.lucko.luckperms.LPBukkitPlugin;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.core.PermissionHolder;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.users.User;
import net.milkbowl.vault.permission.Permission;

public class VaultPermissionHook extends Permission {

    @Setter
    private LPBukkitPlugin plugin;

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

    private boolean objectHas(String world, PermissionHolder object, String permission) {
        if (object == null) return false;

        if (world != null && !world.equals("")) {
            return object.hasPermission(permission, true, "global", world);
        } else {
            return object.hasPermission(permission, true);
        }
    }

    private boolean objectAdd(String world, PermissionHolder object, String permission) {
        if (object == null) return false;

        try {
            if (world != null && !world.equals("")) {
                object.setPermission(permission, true, "global", world);
            } else {
                object.setPermission(permission, true);
            }
        } catch (ObjectAlreadyHasException ignored) {}

        objectSave(object);
        return true;
    }

    private boolean objectRemove(String world, PermissionHolder object, String permission) {
        if (object == null) return false;

        try {
            if (world != null && !world.equals("")) {
                object.unsetPermission(permission, "global", world);
            } else {
                object.unsetPermission(permission);
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
        return objectHas(world, plugin.getUserManager().get(player), permission);
    }

    @Override
    public boolean playerAdd(String world, @NonNull String player, @NonNull String permission) {
        final User user = plugin.getUserManager().get(player);
        return objectAdd(world, user, permission);
    }

    @Override
    public boolean playerRemove(String world, @NonNull String player, @NonNull String permission) {
        final User user = plugin.getUserManager().get(player);
        return objectRemove(world, user, permission);
    }

    @Override
    public boolean groupHas(String world, @NonNull String groupName, @NonNull String permission) {
        final Group group = plugin.getGroupManager().get(groupName);
        return objectHas(world, group, permission);
    }

    @Override
    public boolean groupAdd(String world, @NonNull String groupName, @NonNull String permission) {
        final Group group = plugin.getGroupManager().get(groupName);
        return objectAdd(world, group, permission);
    }

    @Override
    public boolean groupRemove(String world, @NonNull String groupName, @NonNull String permission) {
        final Group group = plugin.getGroupManager().get(groupName);
        return objectRemove(world, group, permission);
    }

    @Override
    public boolean playerInGroup(String world, @NonNull String player, @NonNull String group) {
        final User user = plugin.getUserManager().get(player);
        if (user == null) return false;

        final Group group1 = plugin.getGroupManager().get(group);
        if (group1 == null) return false;

        if (world != null && !world.equals("")) {
            return user.isInGroup(group1, "global", world);
        } else {
            return user.isInGroup(group1);
        }
    }

    @Override
    public boolean playerAddGroup(String world, @NonNull String player, @NonNull String groupName) {
        final User user = plugin.getUserManager().get(player);
        if (user == null) return false;

        final Group group = plugin.getGroupManager().get(groupName);
        if (group == null) return false;

        try {
            if (world != null && !world.equals("")) {
                user.addGroup(group, "global", world);
            } else {
                user.addGroup(group);
            }
        } catch (ObjectAlreadyHasException ignored) {}
        objectSave(user);
        return true;
    }

    @Override
    public boolean playerRemoveGroup(String world, @NonNull String player, @NonNull String groupName) {
        final User user = plugin.getUserManager().get(player);
        if (user == null) return false;

        final Group group = plugin.getGroupManager().get(groupName);
        if (group == null) return false;

        try {
            if (world != null && !world.equals("")) {
                user.removeGroup(group, "global", world);
            } else {
                user.removeGroup(group);
            }
        } catch (ObjectLacksException ignored) {}
        objectSave(user);
        return true;
    }

    @Override
    public String[] getPlayerGroups(String world, @NonNull String player) {
        final User user = plugin.getUserManager().get(player);
        return (user == null) ? new String[0] :
                world != null && !world.equals("") ? user.getGroups("global", world, true).toArray(new String[0]) :
                        user.getGroupNames().toArray(new String[0]);
    }

    @Override
    public String getPrimaryGroup(String world, @NonNull String player) {
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
