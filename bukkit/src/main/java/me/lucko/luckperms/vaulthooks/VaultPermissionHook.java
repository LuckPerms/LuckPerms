package me.lucko.luckperms.vaulthooks;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.LPBukkitPlugin;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksPermissionException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.users.User;
import net.milkbowl.vault.permission.Permission;

@AllArgsConstructor
public class VaultPermissionHook extends Permission {
    private final LPBukkitPlugin plugin;

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
        // Idk???
        return true;
    }

    @Override
    public boolean playerHas(String world, String player, String permission) {
        final User user = plugin.getUserManager().getUser(player);
        return user != null && user.hasPermission(permission, true);
    }

    @Override
    public boolean playerAdd(String world, String player, String permission) {
        final User user = plugin.getUserManager().getUser(player);
        if (user == null) return false;

        try {
            user.setPermission(permission, true);
        } catch (ObjectAlreadyHasException ignored) {}
        plugin.getUserManager().saveUser(user, plugin.getDatastore());
        return true;
    }

    @Override
    public boolean playerRemove(String world, String player, String permission) {
        final User user = plugin.getUserManager().getUser(player);
        if (user == null) return false;

        try {
            user.unsetPermission(permission);
        } catch (ObjectLacksPermissionException ignored) {}
        plugin.getUserManager().saveUser(user, plugin.getDatastore());
        return true;
    }

    @Override
    public boolean groupHas(String world, String groupName, String permission) {
        final Group group = plugin.getGroupManager().getGroup(groupName);
        return group != null && group.hasPermission(permission, true);
    }

    @Override
    public boolean groupAdd(String world, String groupName, String permission) {
        final Group group = plugin.getGroupManager().getGroup(groupName);
        if (group == null) return false;

        try {
            group.setPermission(permission, true);
        } catch (ObjectAlreadyHasException ignored) {}
        plugin.runUpdateTask();
        return true;
    }

    @Override
    public boolean groupRemove(String world, String groupName, String permission) {
        final Group group = plugin.getGroupManager().getGroup(groupName);
        if (group == null) return false;

        try {
            group.unsetPermission(permission);
        } catch (ObjectLacksPermissionException ignored) {}
        plugin.runUpdateTask();
        return true;
    }

    @Override
    public boolean playerInGroup(String world, String player, String group) {
        final User user = plugin.getUserManager().getUser(player);
        if (user == null) return false;

        final Group group1 = plugin.getGroupManager().getGroup(group);
        return group1 != null && user.isInGroup(group1);
    }

    @Override
    public boolean playerAddGroup(String world, String player, String groupName) {
        final User user = plugin.getUserManager().getUser(player);
        if (user == null) return false;

        final Group group = plugin.getGroupManager().getGroup(groupName);
        if (group == null) return false;

        try {
            user.addGroup(group);
        } catch (ObjectAlreadyHasException ignored) {}
        plugin.getUserManager().saveUser(user, plugin.getDatastore());
        return true;
    }

    @Override
    public boolean playerRemoveGroup(String world, String player, String groupName) {
        final User user = plugin.getUserManager().getUser(player);
        if (user == null) return false;

        final Group group = plugin.getGroupManager().getGroup(groupName);
        if (group == null) return false;

        try {
            user.removeGroup(group);
        } catch (ObjectLacksPermissionException ignored) {}
        plugin.getUserManager().saveUser(user, plugin.getDatastore());
        return true;
    }

    @Override
    public String[] getPlayerGroups(String world, String player) {
        final User user = plugin.getUserManager().getUser(player);
        return (user == null) ? new String[0] : user.getGroupNames().toArray(new String[0]);
    }

    @Override
    public String getPrimaryGroup(String world, String player) {
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
