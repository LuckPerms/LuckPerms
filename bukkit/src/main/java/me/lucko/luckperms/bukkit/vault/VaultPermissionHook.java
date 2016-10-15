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

package me.lucko.luckperms.bukkit.vault;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.core.PermissionHolder;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import net.milkbowl.vault.permission.Permission;

import java.util.HashMap;
import java.util.Map;

/**
 * The LuckPerms Vault Permission implementation
 * Most lookups are cached.
 */
public class VaultPermissionHook extends Permission {

    @Getter
    @Setter
    private LPBukkitPlugin plugin;

    @Getter
    private VaultScheduler scheduler;

    @Getter
    @Setter
    private String server = "global";

    @Getter
    @Setter
    private boolean includeGlobal = true;

    @Getter
    @Setter
    private boolean ignoreWorld = false;

    public void setup() {
        scheduler = new VaultScheduler(plugin);
    }

    public void log(String s) {
        if (plugin.getConfiguration().isDebugPermissionChecks()) {
            plugin.getLog().info("[VAULT] " + s);
        }
    }

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

    /**
     * Generic method to add a permission to a holder
     * @param world the world to add in
     * @param holder the holder to add the permission to
     * @param permission the permission to add
     */
    private void add(String world, PermissionHolder holder, String permission) {
        try {
            if (world != null && !world.equals("")) {
                holder.setPermission(permission, true, server, world);
            } else {
                holder.setPermission(permission, true, server);
            }
        } catch (ObjectAlreadyHasException ignored) {}

        save(holder);
    }

    /**
     * Generic method to remove a permission from a holder
     * @param world the world to remove in
     * @param holder the holder to remove the permission from
     * @param permission the permission to remove
     */
    private void remove(String world, PermissionHolder holder, String permission) {
        try {
            if (world != null && !world.equals("")) {
                holder.unsetPermission(permission, server, world);
            } else {
                holder.unsetPermission(permission, server);
            }
        } catch (ObjectLacksException ignored) {}

        save(holder);
    }

    /**
     * Utility method for saving a user or group
     * @param holder the holder instance
     */
    void save(PermissionHolder holder) {
        if (holder instanceof User) {
            ((User) holder).refreshPermissions();
            plugin.getDatastore().saveUser(((User) holder));
        }
        if (holder instanceof Group) {
            plugin.getDatastore().saveGroup(((Group) holder));
            plugin.runUpdateTask();
        }
    }

    Contexts createContext(String server, String world) {
        Map<String, String> context = new HashMap<>();
        if (world != null && !world.equals("")) {
            context.put("world", world);
        }
        context.put("server", server);
        return new Contexts(context, isIncludeGlobal(), true, true, true, true);
    }

    @Override
    public boolean playerHas(String world, @NonNull String player, @NonNull String permission) {
        world = ignoreWorld ? null : world; // Correct world value
        log("Checking if player " + player + " has permission: " + permission + " on world " + world + ", server " + server);

        User user = plugin.getUserManager().get(player);
        if (user == null) return false;

        if (user.getUserData() == null) {
            return false;
        }

        // Effectively fallback to the standard Bukkit #hasPermission check.
        return user.getUserData().getPermissionData(createContext(server, world)).getPermissionValue(permission).asBoolean();
    }

    @Override
    public boolean playerAdd(String world, @NonNull String player, @NonNull String permission) {
        String finalWorld = ignoreWorld ? null : world; // Correct world value
        log("Adding permission to player " + player + ": '" + permission + "' on world " + finalWorld + ", server " + server);

        final User user = plugin.getUserManager().get(player);
        if (user == null) return false;

        scheduler.scheduleTask(() -> add(finalWorld, user, permission));
        return true;
    }

    @Override
    public boolean playerRemove(String world, @NonNull String player, @NonNull String permission) {
        String finalWorld = ignoreWorld ? null : world; // Correct world value
        log("Removing permission from player " + player + ": '" + permission + "' on world " + finalWorld + ", server " + server);

        final User user = plugin.getUserManager().get(player);
        if (user == null) return false;

        scheduler.scheduleTask(() -> remove(finalWorld, user, permission));
        return true;
    }

    @Override
    public boolean groupHas(String world, @NonNull String groupName, @NonNull String permission) {
        world = ignoreWorld ? null : world; // Correct world value
        log("Checking if group " + groupName + " has permission: " + permission + " on world " + world + ", server " + server);

        final Group group = plugin.getGroupManager().get(groupName);
        if (group == null) return false;

        // This is a nasty call. Groups aren't cached. :(
        Map<String, Boolean> permissions = group.exportNodes(createContext(server, world), true);

        return permissions.containsKey(permission) && permissions.get(permission);
    }

    @Override
    public boolean groupAdd(String world, @NonNull String groupName, @NonNull String permission) {
        String finalWorld = ignoreWorld ? null : world; // Correct world value
        log("Adding permission to group " + groupName + ": '" + permission + "' on world " + finalWorld + ", server " + server);

        final Group group = plugin.getGroupManager().get(groupName);
        if (group == null) return false;

        scheduler.scheduleTask(() -> add(finalWorld, group, permission));
        return true;
    }

    @Override
    public boolean groupRemove(String world, @NonNull String groupName, @NonNull String permission) {
        String finalWorld = ignoreWorld ? null : world; // Correct world value
        log("Removing permission from group " + groupName + ": '" + permission + "' on world " + finalWorld + ", server " + server);

        final Group group = plugin.getGroupManager().get(groupName);
        if (group == null) return false;

        scheduler.scheduleTask(() -> remove(finalWorld, group, permission));
        return true;
    }

    @Override
    public boolean playerInGroup(String world, @NonNull String player, @NonNull String group) {
        String finalWorld = ignoreWorld ? null : world; // Correct world value
        log("Checking if player " + player + " is in group: " + group + " on world " + finalWorld + ", server " + server);

        final User user = plugin.getUserManager().get(player);
        if (user == null) return false;

        return user.getNodes().stream()
                .filter(Node::isGroupNode)
                .filter(n -> n.shouldApplyOnServer(server, isIncludeGlobal(), false))
                .filter(n -> n.shouldApplyOnWorld(finalWorld, true, false))
                .map(Node::getGroupName)
                .filter(s -> s.equalsIgnoreCase(group))
                .findAny().isPresent();
    }

    @Override
    public boolean playerAddGroup(String world, @NonNull String player, @NonNull String groupName) {
        String finalWorld = ignoreWorld ? null : world; // Correct world value
        log("Adding player " + player + " to group: '" + groupName + "' on world " + finalWorld + ", server " + server);

        final User user = plugin.getUserManager().get(player);
        if (user == null) return false;

        final Group group = plugin.getGroupManager().get(groupName);
        if (group == null) return false;

        scheduler.scheduleTask(() -> {
            try {
                if (finalWorld != null && !finalWorld.equals("")) {
                    user.setInheritGroup(group, server, finalWorld);
                } else {
                    user.setInheritGroup(group, server);
                }
            } catch (ObjectAlreadyHasException ignored) {}
            save(user);
        });
        return true;
    }

    @Override
    public boolean playerRemoveGroup(String world, @NonNull String player, @NonNull String groupName) {
        String finalWorld = ignoreWorld ? null : world; // Correct world value
        log("Removing player " + player + " from group: '" + groupName + "' on world " + finalWorld + ", server " + server);

        final User user = plugin.getUserManager().get(player);
        if (user == null) return false;

        final Group group = plugin.getGroupManager().get(groupName);
        if (group == null) return false;

        scheduler.scheduleTask(() -> {
            try {
                if (finalWorld != null && !finalWorld.equals("")) {
                    user.unsetInheritGroup(group, server, finalWorld);
                } else {
                    user.unsetInheritGroup(group, server);
                }
            } catch (ObjectLacksException ignored) {}
            save(user);
        });
        return true;
    }

    @Override
    public String[] getPlayerGroups(String world, @NonNull String player) {
        String finalWorld = ignoreWorld ? null : world; // Correct world value
        log("Getting groups of player: " + player + ", on world " + finalWorld + ", server " + server);

        User user = plugin.getUserManager().get(player);
        if (user == null) return new String[0];

        return user.getNodes().stream()
                .filter(Node::isGroupNode)
                .filter(n -> n.shouldApplyOnServer(server, isIncludeGlobal(), false))
                .filter(n -> n.shouldApplyOnWorld(finalWorld, true, false))
                .map(Node::getGroupName)
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
