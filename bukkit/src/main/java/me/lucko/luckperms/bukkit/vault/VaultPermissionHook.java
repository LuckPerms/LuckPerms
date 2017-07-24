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

package me.lucko.luckperms.bukkit.vault;

import lombok.Getter;
import lombok.NonNull;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.caching.PermissionData;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.utils.ExtractedContexts;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An implementation of the Vault {@link Permission} API using LuckPerms.
 *
 * Methods which change the state of data objects are likely to return immediately.
 *
 * LuckPerms is a multithreaded permissions plugin, and some actions require considerable
 * time to execute. (database queries, re-population of caches, etc) In these cases, the
 * methods will return immediately and the change will be executed asynchronously.
 *
 * Users of the Vault API expect these methods to be "main thread friendly", so unfortunately,
 * we have to favour so called "performance" for consistency. The Vault API really wasn't designed
 * with database backed permission plugins in mind. :(
 *
 * The methods which query offline players will explicitly FAIL if the corresponding player is not online.
 * We cannot risk blocking the main thread to load in their data. Again, this is due to crap Vault
 * design. There is nothing I can do about it.
 */
@Getter
public class VaultPermissionHook extends Permission {

    // the plugin instance
    private LPBukkitPlugin plugin;

    // an executor for Vault modifications.
    private VaultExecutor executor;

    public VaultPermissionHook(LPBukkitPlugin plugin) {
        this.plugin = plugin;
        super.plugin = JavaPlugin.getProvidingPlugin(Permission.class);
        this.executor = new VaultExecutor(plugin);
    }

    @Override
    public String getName() {
        return "LuckPerms";
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public boolean hasSuperPermsCompat() {
        return true;
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public String[] getGroups() {
        return plugin.getGroupManager().getAll().keySet().toArray(new String[0]);
    }

    @Override
    public boolean has(@NonNull CommandSender sender, @NonNull String permission) {
        return sender.hasPermission(permission);
    }

    @Override
    public boolean has(@NonNull Player player, @NonNull String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public boolean playerHas(String world, @NonNull String player, @NonNull String permission) {
        return playerHas(world, Bukkit.getPlayerExact(player), permission);
    }

    @Override
    public boolean playerHas(String world, @NonNull OfflinePlayer player, @NonNull String permission) {
        return playerHas(world, player.getPlayer(), permission);
    }

    private boolean playerHas(String world, Player player, String permission) {
        world = correctWorld(world);
        log("Checking if player " + player + " has permission: " + permission + " on world " + world + ", server " + getServer());

        if (player == null) {
            return false;
        }

        User user = plugin.getUserManager().getIfLoaded(plugin.getUuidCache().getUUID(player.getUniqueId()));
        if (user == null) {
            return false;
        }

        // Effectively fallback to the standard Bukkit #hasPermission check.
        return user.getUserData().getPermissionData(createContextForWorldLookup(player, world)).getPermissionValue(permission).asBoolean();
    }

    @Override
    public boolean playerAdd(String world, @NonNull String player, @NonNull String permission) {
        return playerAdd(world, Bukkit.getPlayerExact(player), permission);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean playerAdd(World world, @NonNull String player, @NonNull String permission) {
        return playerAdd(world == null ? null : world.getName(), Bukkit.getPlayerExact(player), permission);
    }

    @Override
    public boolean playerAdd(String world, @NonNull OfflinePlayer player, @NonNull String permission) {
        return playerAdd(world, player.getPlayer(), permission);
    }

    private boolean playerAdd(String world, Player player, String permission) {
        world = correctWorld(world);
        log("Adding permission to player " + player + ": '" + permission + "' on world " + world + ", server " + getServer());

        if (player == null) {
            return false;
        }

        final User user = plugin.getUserManager().getIfLoaded(plugin.getUuidCache().getUUID(player.getUniqueId()));
        if (user == null) {
            return false;
        }

        holderAddPermission(user, permission, world);
        return true;
    }

    @Override
    public boolean playerRemove(String world, @NonNull String player, @NonNull String permission) {
        return playerRemove(world, Bukkit.getPlayerExact(player), permission);
    }

    @Override
    public boolean playerRemove(String world, @NonNull OfflinePlayer player, @NonNull String permission) {
        return playerRemove(world, player.getPlayer(), permission);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean playerRemove(World world, @NonNull String player, @NonNull String permission) {
        return playerRemove(world == null ? null : world.getName(), Bukkit.getPlayerExact(player), permission);
    }

    private boolean playerRemove(String world, Player player, String permission) {
        world = correctWorld(world);
        log("Removing permission from player " + player + ": '" + permission + "' on world " + world + ", server " + getServer());

        if (player == null) {
            return false;
        }

        final User user = plugin.getUserManager().getIfLoaded(plugin.getUuidCache().getUUID(player.getUniqueId()));
        if (user == null) {
            return false;
        }

        holderRemovePermission(user, permission, world);
        return true;
    }

    @Override
    public boolean groupHas(String world, @NonNull String groupName, @NonNull String permission) {
        world = correctWorld(world);
        log("Checking if group " + groupName + " has permission: " + permission + " on world " + world + ", server " + getServer());

        final Group group = plugin.getGroupManager().getIfLoaded(groupName);
        if (group == null) return false;

        // This is a nasty call. Groups aren't cached. :(
        Map<String, Boolean> permissions = group.exportNodes(ExtractedContexts.generate(createContextForWorldLookup(world)), true);
        return permissions.containsKey(permission.toLowerCase()) && permissions.get(permission.toLowerCase());
    }

    @Override
    public boolean groupAdd(String world, @NonNull String groupName, @NonNull String permission) {
        world = correctWorld(world);
        log("Adding permission to group " + groupName + ": '" + permission + "' on world " + world + ", server " + getServer());

        final Group group = plugin.getGroupManager().getIfLoaded(groupName);
        if (group == null) return false;

        holderAddPermission(group, permission, world);
        return true;
    }

    @Override
    public boolean groupRemove(String world, @NonNull String groupName, @NonNull String permission) {
        world = correctWorld(world);
        log("Removing permission from group " + groupName + ": '" + permission + "' on world " + world + ", server " + getServer());

        final Group group = plugin.getGroupManager().getIfLoaded(groupName);
        if (group == null) return false;

        holderRemovePermission(group, permission, world);
        return true;
    }

    @Override
    public boolean playerInGroup(String world, String player, @NonNull String group) {
        return playerHas(world, player, "group." + group);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean playerInGroup(World world, String player, @NonNull String group) {
        return playerHas(world, player, "group." + group);
    }

    @Override
    public boolean playerInGroup(String world, OfflinePlayer player, @NonNull String group) {
        return playerHas(world, player, "group." + group);
    }

    @Override
    public boolean playerInGroup(Player player, @NonNull String group) {
        return playerHas(player, "group." + group);
    }

    private boolean checkGroupExists(String group) {
        return plugin.getGroupManager().isLoaded(group);
    }

    @Override
    public boolean playerAddGroup(String world, String player, @NonNull String group) {
        return checkGroupExists(group) && playerAdd(world, player, "group." + group);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean playerAddGroup(World world, String player, @NonNull String group) {
        return checkGroupExists(group) && playerAdd(world, player, "group." + group);
    }

    @Override
    public boolean playerAddGroup(String world, OfflinePlayer player, @NonNull String group) {
        return checkGroupExists(group) && playerAdd(world, player, "group." + group);
    }

    @Override
    public boolean playerAddGroup(Player player, @NonNull String group) {
        return checkGroupExists(group) && playerAdd(player, "group." + group);
    }

    @Override
    public boolean playerRemoveGroup(String world, String player, @NonNull String group) {
        return checkGroupExists(group) && playerRemove(world, player, "group." + group);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean playerRemoveGroup(World world, String player, @NonNull String group) {
        return checkGroupExists(group) && playerRemove(world, player, "group." + group);
    }

    @Override
    public boolean playerRemoveGroup(String world, OfflinePlayer player, @NonNull String group) {
        return checkGroupExists(group) && playerRemove(world, player, "group." + group);
    }

    @Override
    public boolean playerRemoveGroup(Player player, @NonNull String group) {
        return checkGroupExists(group) && playerRemove(player, "group." + group);
    }

    @Override
    public String[] getPlayerGroups(String world, @NonNull String player) {
        return getPlayerGroups(world, Bukkit.getPlayerExact(player));
    }

    @SuppressWarnings("deprecation")
    @Override
    public String[] getPlayerGroups(World world, @NonNull String player) {
        return getPlayerGroups(world == null ? null : world.getName(), Bukkit.getPlayerExact(player));
    }

    @Override
    public String[] getPlayerGroups(String world, @NonNull OfflinePlayer player) {
        return getPlayerGroups(world, player.getPlayer());
    }

    private String[] getPlayerGroups(String world, Player player) {
        world = correctWorld(world);
        log("Getting groups of player: " + player + ", on world " + world + ", server " + getServer());

        if (player == null) {
            return new String[0];
        }

        User user = plugin.getUserManager().getIfLoaded(plugin.getUuidCache().getUUID(player.getUniqueId()));
        if (user == null) {
            return new String[0];
        }

        String w = world; // screw effectively final
        return user.getNodes().values().stream()
                .filter(Node::isGroupNode)
                .filter(n -> n.shouldApplyWithContext(createContextForWorldLookup(plugin.getPlayer(user), w).getContexts()))
                .map(Node::getGroupName)
                .toArray(String[]::new);
    }

    @Override
    public String getPrimaryGroup(String world, @NonNull String player) {
        return getPrimaryGroup(world, Bukkit.getPlayerExact(player));
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getPrimaryGroup(World world, @NonNull String player) {
        return getPrimaryGroup(world == null ? null : world.getName(), Bukkit.getPlayerExact(player));
    }

    @Override
    public String getPrimaryGroup(String world, @NonNull OfflinePlayer player) {
        return getPrimaryGroup(world, player.getPlayer());
    }

    private String getPrimaryGroup(String world, Player player) {
        world = correctWorld(world);
        log("Getting primary group of player: " + player);

        if (player == null) {
            return null;
        }

        final User user = plugin.getUserManager().getIfLoaded(plugin.getUuidCache().getUUID(player.getUniqueId()));

        if (user == null) {
            return null;
        }

        // nothing special, just return the value.
        if (!isPgo()) {
            String g = user.getPrimaryGroup().getValue();
            return plugin.getConfiguration().get(ConfigKeys.GROUP_NAME_REWRITES).getOrDefault(g, g);
        }

        // we need to do the complex PGO checking. (it's been enabled in the config.)
        if (isPgoCheckInherited()) {
            // we can just check the cached data
            PermissionData data = user.getUserData().getPermissionData(createContextForWorldLookup(plugin.getPlayer(user), world));
            for (Map.Entry<String, Boolean> e : data.getImmutableBacking().entrySet()) {
                if (!e.getValue()) continue;
                if (!e.getKey().toLowerCase().startsWith("vault.primarygroup.")) continue;

                String group = e.getKey().substring("vault.primarygroup.".length());
                if (isPgoCheckExists()) {
                    if (!plugin.getGroupManager().isLoaded(group)) {
                        continue;
                    }
                }

                if (isPgoCheckMemberOf()) {
                    if (data.getPermissionValue("group." + group) != Tristate.TRUE) {
                        continue;
                    }
                }

                return group;
            }
        } else {
            // we need to check the users permissions only
            for (Node node : user.mergePermissionsToList()) {
                if (!node.getValue()) continue;
                if (!node.getPermission().toLowerCase().startsWith("vault.primarygroup.")) continue;
                if (!node.shouldApplyOnServer(getServer(), isIncludeGlobal(), false)) continue;
                if (!node.shouldApplyOnWorld(world, true, false)) continue;

                String group = node.getPermission().substring("vault.primarygroup.".length());
                if (isPgoCheckExists()) {
                    if (!plugin.getGroupManager().isLoaded(group)) {
                        continue;
                    }
                }

                if (isPgoCheckMemberOf()) {
                    String finalWorld = world;
                    List<String> localGroups = user.mergePermissionsToList().stream()
                            .filter(Node::isGroupNode)
                            .filter(n -> n.shouldApplyOnWorld(finalWorld, isIncludeGlobal(), true))
                            .filter(n -> n.shouldApplyOnServer(getServer(), isIncludeGlobal(), true))
                            .map(Node::getGroupName)
                            .collect(Collectors.toList());

                    if (!localGroups.contains(group.toLowerCase())) {
                        continue;
                    }
                }

                return group;
            }
        }

        // Fallback
        String g = user.getPrimaryGroup().getValue();
        return plugin.getConfiguration().get(ConfigKeys.GROUP_NAME_REWRITES).getOrDefault(g, g);
    }

    public void log(String s) {
        if (plugin.getConfiguration().get(ConfigKeys.VAULT_DEBUG)) {
            plugin.getLog().info("[VAULT] " + s);
        }
    }

    String correctWorld(String world) {
        return isIgnoreWorld() ? null : world;
    }

    // utility methods for modifying the state of PermissionHolders

    private void holderAddPermission(PermissionHolder holder, String permission, String world) {
        executor.execute(() -> {
            DataMutateResult result;
            if (world != null && !world.equals("") && !world.equalsIgnoreCase("global")) {
                result = holder.setPermission(NodeFactory.make(permission, true, getServer(), world));
            } else {
                result = holder.setPermission(NodeFactory.make(permission, true, getServer()));
            }

            if (result.asBoolean()) {
                holderSave(holder);
            }
        });
    }

    private void holderRemovePermission(PermissionHolder holder, String permission, String world) {
        executor.execute(() -> {
            DataMutateResult result;
            if (world != null && !world.equals("") && !world.equalsIgnoreCase("global")) {
                result = holder.unsetPermission(NodeFactory.make(permission, getServer(), world));
            } else {
                result = holder.unsetPermission(NodeFactory.make(permission, getServer()));
            }

            if (result.asBoolean()) {
                holderSave(holder);
            }
        });
    }

    public void holderSave(PermissionHolder holder) {
        if (holder instanceof User) {
            User u = (User) holder;
            plugin.getStorage().saveUser(u).thenRunAsync(() -> u.getRefreshBuffer().request(), plugin.getScheduler().async());
        }
        if (holder instanceof Group) {
            Group g = (Group) holder;
            plugin.getStorage().saveGroup(g).thenRunAsync(() -> plugin.getUpdateTaskBuffer().request(), plugin.getScheduler().async());
        }
    }

    // helper methods to build Contexts instances for different world/server combinations

    public Contexts createContextForWorldSet(String world) {
        MutableContextSet context = MutableContextSet.create();
        if (world != null && !world.equals("") && !world.equalsIgnoreCase("global")) {
            context.add("world", world.toLowerCase());
        }
        context.add("server", getServer());
        return new Contexts(context, isIncludeGlobal(), true, true, true, true, false);
    }

    public Contexts createContextForWorldLookup(String world) {
        MutableContextSet context = MutableContextSet.create();
        if (world != null && !world.equals("") && !world.equalsIgnoreCase("global")) {
            context.add("world", world.toLowerCase());
        }
        context.add("server", getServer());
        context.addAll(plugin.getConfiguration().getContextsFile().getStaticContexts());
        return new Contexts(context, isIncludeGlobal(), true, true, true, true, false);
    }

    public Contexts createContextForWorldLookup(@NonNull Player player, String world) {
        MutableContextSet context = MutableContextSet.create();

        // use player context
        ImmutableContextSet applicableContext = plugin.getContextManager().getApplicableContext(player);
        context.addAll(applicableContext);

        // worlds & servers get set depending on the config setting
        context.removeAll("world");
        context.removeAll("server");

        // add the vault settings
        if (world != null && !world.isEmpty() && !world.equalsIgnoreCase("global")) {
            context.add("world", world.toLowerCase());
        }
        context.add("server", getServer());

        return new Contexts(context, isIncludeGlobal(), true, true, true, true, false);
    }

    // helper methods to just pull values from the config.

    String getServer() {
        return plugin.getConfiguration().get(ConfigKeys.VAULT_SERVER);
    }

    boolean isIncludeGlobal() {
        return plugin.getConfiguration().get(ConfigKeys.VAULT_INCLUDING_GLOBAL);
    }

    boolean isIgnoreWorld() {
        return plugin.getConfiguration().get(ConfigKeys.VAULT_IGNORE_WORLD);
    }

    private boolean isPgo() {
        return plugin.getConfiguration().get(ConfigKeys.VAULT_PRIMARY_GROUP_OVERRIDES);
    }

    private boolean isPgoCheckInherited() {
        return plugin.getConfiguration().get(ConfigKeys.VAULT_PRIMARY_GROUP_OVERRIDES_CHECK_INHERITED);
    }

    private boolean isPgoCheckExists() {
        return plugin.getConfiguration().get(ConfigKeys.VAULT_PRIMARY_GROUP_OVERRIDES_CHECK_EXISTS);
    }

    private boolean isPgoCheckMemberOf() {
        return plugin.getConfiguration().get(ConfigKeys.VAULT_PRIMARY_GROUP_OVERRIDES_CHECK_MEMBER_OF);
    }
}
