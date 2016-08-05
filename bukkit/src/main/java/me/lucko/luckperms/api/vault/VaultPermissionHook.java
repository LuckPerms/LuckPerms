package me.lucko.luckperms.api.vault;

import lombok.NonNull;
import lombok.Setter;
import me.lucko.luckperms.LPBukkitPlugin;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.PermissionHolder;
import net.milkbowl.vault.permission.Permission;

/**
 *
 */
class VaultPermissionHook extends Permission {

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

    private void objectSave(PermissionHolder t) {
        if (t instanceof User) {
            ((User) t).refreshPermissions();
            plugin.getDatastore().saveUser(((User) t), aBoolean -> {});
        }
        if (t instanceof Group) {
            plugin.getDatastore().saveGroup(((Group) t), aBoolean -> plugin.runUpdateTask());
        }
    }

    @Override
    public boolean playerHas(String world, @NonNull String player, @NonNull String permission) {
        return objectHas(world, plugin.getUserManager().getUser(player), permission);
    }

    @Override
    public boolean playerAdd(String world, @NonNull String player, @NonNull String permission) {
        final User user = plugin.getUserManager().getUser(player);
        return objectAdd(world, user, permission);
    }

    @Override
    public boolean playerRemove(String world, @NonNull String player, @NonNull String permission) {
        final User user = plugin.getUserManager().getUser(player);
        return objectRemove(world, user, permission);
    }

    @Override
    public boolean groupHas(String world, @NonNull String groupName, @NonNull String permission) {
        final Group group = plugin.getGroupManager().getGroup(groupName);
        return objectHas(world, group, permission);
    }

    @Override
    public boolean groupAdd(String world, @NonNull String groupName, @NonNull String permission) {
        final Group group = plugin.getGroupManager().getGroup(groupName);
        return objectAdd(world, group, permission);
    }

    @Override
    public boolean groupRemove(String world, @NonNull String groupName, @NonNull String permission) {
        final Group group = plugin.getGroupManager().getGroup(groupName);
        return objectRemove(world, group, permission);
    }

    @Override
    public boolean playerInGroup(String world, @NonNull String player, @NonNull String group) {
        final User user = plugin.getUserManager().getUser(player);
        if (user == null) return false;

        final Group group1 = plugin.getGroupManager().getGroup(group);
        if (group1 == null) return false;

        if (world != null && !world.equals("")) {
            return user.isInGroup(group1, "global", world);
        } else {
            return user.isInGroup(group1);
        }
    }

    @Override
    public boolean playerAddGroup(String world, @NonNull String player, @NonNull String groupName) {
        final User user = plugin.getUserManager().getUser(player);
        if (user == null) return false;

        final Group group = plugin.getGroupManager().getGroup(groupName);
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
        final User user = plugin.getUserManager().getUser(player);
        if (user == null) return false;

        final Group group = plugin.getGroupManager().getGroup(groupName);
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
        final User user = plugin.getUserManager().getUser(player);
        return (user == null) ? new String[0] :
                world != null && !world.equals("") ? user.getLocalGroups("global", world).toArray(new String[0]) :
                        user.getGroupNames().toArray(new String[0]);
    }

    @Override
    public String getPrimaryGroup(String world, @NonNull String player) {
        final User user = plugin.getUserManager().getUser(player);
        return (user == null) ? null : user.getPrimaryGroup();
    }

    @Override
    public String[] getGroups() {
        return plugin.getGroupManager().getGroups().keySet().toArray(new String[0]);
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }
}
