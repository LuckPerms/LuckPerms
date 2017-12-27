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

import com.google.common.base.Preconditions;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.caching.type.PermissionCache;
import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.verbose.CheckOrigin;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
public class VaultPermissionHook extends AbstractVaultPermission {

    // the plugin instance
    private final LPBukkitPlugin plugin;

    // an executor for Vault modifications.
    private final ExecutorService executor;

    public VaultPermissionHook(LPBukkitPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor();
        this.worldMappingFunction = world -> isIgnoreWorld() ? null : world;
    }

    @Override
    public String getName() {
        return "LuckPerms";
    }

    // override this check to delegate to Player#hasPermission
    @Override
    public boolean has(Player player, String permission) {
        return player.hasPermission(permission);
    }

    // override this check to delegate to Player#hasPermission
    @Override
    public boolean playerHas(Player player, String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public String[] getGroups() {
        return plugin.getGroupManager().getAll().values().stream()
                .map(g -> g.getDisplayName().orElse(g.getName()))
                .toArray(String[]::new);
    }

    @Override
    public boolean hasPermission(String world, UUID uuid, String permission) {
        Preconditions.checkNotNull(uuid, "uuid");
        Preconditions.checkNotNull(permission, "permission");

        User user = getUser(uuid);
        if (user == null) {
            return false;
        }

        Contexts contexts = contextForLookup(user, world);
        PermissionCache permissionData = user.getCachedData().getPermissionData(contexts);

        Tristate result = permissionData.getPermissionValue(permission, CheckOrigin.INTERNAL);
        if (log()) {
            logMsg("#hasPermission: %s - %s - %s - %s", user.getFriendlyName(), contexts.getContexts().toMultimap(), permission, result);
        }
        return result.asBoolean();
    }

    @Override
    public boolean playerAddPermission(String world, UUID uuid, String permission) {
        Preconditions.checkNotNull(uuid, "uuid");
        Preconditions.checkNotNull(permission, "permission");

        User user = getUser(uuid);
        if (user == null) {
            return false;
        }

        holderAddPermission(user, permission, world);
        return true;
    }

    @Override
    public boolean playerRemovePermission(String world, UUID uuid, String permission) {
        Preconditions.checkNotNull(uuid, "uuid");
        Preconditions.checkNotNull(permission, "permission");

        User user = getUser(uuid);
        if (user == null) {
            return false;
        }

        holderRemovePermission(user, permission, world);
        return true;
    }

    @Override
    public boolean playerInGroup(String world, UUID uuid, String group) {
        Preconditions.checkNotNull(uuid, "uuid");
        Preconditions.checkNotNull(group, "group");
        return hasPermission(world, uuid, NodeFactory.groupNode(rewriteGroupName(group)));
    }

    @Override
    public boolean playerAddGroup(String world, UUID uuid, String group) {
        Preconditions.checkNotNull(uuid, "uuid");
        Preconditions.checkNotNull(group, "group");
        return checkGroupExists(group) && playerAddPermission(world, uuid, NodeFactory.groupNode(rewriteGroupName(group)));
    }

    @Override
    public boolean playerRemoveGroup(String world, UUID uuid, String group) {
        Preconditions.checkNotNull(uuid, "uuid");
        Preconditions.checkNotNull(group, "group");
        return checkGroupExists(group) && playerRemovePermission(world, uuid, NodeFactory.groupNode(rewriteGroupName(group)));
    }

    @Override
    public String[] playerGetGroups(String world, UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid");

        User user = getUser(uuid);
        if (user == null) {
            return new String[0];
        }

        ContextSet contexts = contextForLookup(user, world).getContexts();

        String[] ret = user.getEnduringNodes().values().stream()
                .filter(Node::isGroupNode)
                .filter(n -> n.shouldApplyWithContext(contexts))
                .map(n -> {
                    Group group = plugin.getGroupManager().getIfLoaded(n.getGroupName());
                    if (group != null) {
                        return group.getDisplayName().orElse(group.getName());
                    }
                    return n.getGroupName();
                })
                .toArray(String[]::new);

        if (log()) {
            logMsg("#playerGetGroups: %s - %s - %s", user.getFriendlyName(), contexts, Arrays.toString(ret));
        }

        return ret;
    }

    @Override
    public String playerPrimaryGroup(String world, UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid");

        User user = getUser(uuid);
        if (user == null) {
            return null;
        }

        String value = user.getPrimaryGroup().getValue();
        Group group = getGroup(value);
        if (group != null) {
            value = group.getDisplayName().orElse(group.getName());
        }

        if (log()) {
            logMsg("#playerPrimaryGroup: %s - %s - %s", user.getFriendlyName(), world, value);
        }

        return value;
    }

    @Override
    public boolean groupHasPermission(String world, String name, String permission) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(permission, "permission");

        Group group = getGroup(name);
        if (group == null) {
            return false;
        }

        Contexts contexts = contextForLookup(null, world);
        PermissionCache permissionData = group.getCachedData().getPermissionData(contexts);

        Tristate result = permissionData.getPermissionValue(permission, CheckOrigin.INTERNAL);
        if (log()) {
            logMsg("#groupHasPermission: %s - %s - %s - %s", group.getName(), contexts.getContexts().toMultimap(), permission, result);
        }
        return result.asBoolean();
    }

    @Override
    public boolean groupAddPermission(String world, String name, String permission) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(permission, "permission");

        Group group = getGroup(name);
        if (group == null) {
            return false;
        }

        holderAddPermission(group, permission, world);
        return true;
    }

    @Override
    public boolean groupRemovePermission(String world, String name, String permission) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(permission, "permission");

        Group group = getGroup(name);
        if (group == null) {
            return false;
        }

        holderRemovePermission(group, permission, world);
        return true;
    }

    // utility methods for getting user and group instances

    private User getUser(UUID uuid) {
        return plugin.getUserManager().getIfLoaded(plugin.getUuidCache().getUUID(uuid));
    }

    private Group getGroup(String name) {
        return plugin.getGroupManager().getByDisplayName(name);
    }

    private boolean checkGroupExists(String group) {
        return plugin.getGroupManager().getByDisplayName(group) != null;
    }

    private String rewriteGroupName(String name) {
        Group group = plugin.getGroupManager().getByDisplayName(name);
        if (group != null) {
            return group.getName();
        }
        return name;
    }

    // logging
    private boolean log() {
        return plugin.getConfiguration().get(ConfigKeys.VAULT_DEBUG);
    }
    private void logMsg(String format, Object... args) {
        plugin.getLog().info("[VAULT-PERMS] " + String.format(format, args)
                .replace(CommandManager.SECTION_CHAR, '$')
                .replace(CommandManager.AMPERSAND_CHAR, '$')
        );
    }

    // utility method for getting a contexts instance for a given vault lookup.
    Contexts contextForLookup(User user, String world) {
        MutableContextSet context;

        Player player = user == null ? null : plugin.getPlayer(user);
        if (player != null) {
            context = plugin.getContextManager().getApplicableContext(player).mutableCopy();
        } else {
            context = plugin.getContextManager().getStaticContext().mutableCopy();
        }

        // if world is null, we want to do a lookup in the players current context
        // if world is not null, we want to do a lookup in that specific world
        if (world != null && !world.isEmpty()) {
            // remove already accumulated worlds
            context.removeAll(Contexts.WORLD_KEY);
            // add the vault world
            context.add(Contexts.WORLD_KEY, world.toLowerCase());
        }

        // if we're using a special vault server
        if (useVaultServer()) {
            // remove the normal server context from the set
            context.remove(Contexts.SERVER_KEY, getServer());

            // add the vault specific server
            if (!getVaultServer().equals("global")) {
                context.add(Contexts.SERVER_KEY, getVaultServer());
            }
        }

        return new Contexts(context, isIncludeGlobal(), true, true, true, true, false);
    }

    // utility methods for modifying the state of PermissionHolders

    private void holderAddPermission(PermissionHolder holder, String permission, String world) {
        Preconditions.checkNotNull(permission, "permission is null");
        Preconditions.checkArgument(!permission.isEmpty(), "permission is an empty string");

        if (log()) {
            logMsg("#holderAddPermission: %s - %s - %s", holder.getFriendlyName(), permission, world);
        }

        executor.execute(() -> {
            if (holder.setPermission(NodeFactory.make(permission, true, getVaultServer(), world)).asBoolean()) {
                holderSave(holder);
            }
        });
    }

    private void holderRemovePermission(PermissionHolder holder, String permission, String world) {
        Preconditions.checkNotNull(permission, "permission is null");
        Preconditions.checkArgument(!permission.isEmpty(), "permission is an empty string");

        if (log()) {
            logMsg("#holderRemovePermission: %s - %s - %s", holder.getFriendlyName(), permission, world);
        }

        executor.execute(() -> {
            if (holder.unsetPermission(NodeFactory.make(permission, getVaultServer(), world)).asBoolean()) {
                holderSave(holder);
            }
        });
    }

    void holderSave(PermissionHolder holder) {
        if (holder.getType().isUser()) {
            User u = (User) holder;
            plugin.getStorage().saveUser(u).thenRunAsync(() -> u.getRefreshBuffer().request(), plugin.getScheduler().async());
        }
        if (holder.getType().isGroup()) {
            Group g = (Group) holder;
            plugin.getStorage().saveGroup(g).thenRunAsync(() -> plugin.getUpdateTaskBuffer().request(), plugin.getScheduler().async());
        }
    }

    // helper methods to just pull values from the config.

    String getServer() {
        return plugin.getConfiguration().get(ConfigKeys.SERVER);
    }

    String getVaultServer() {
        return plugin.getConfiguration().get(ConfigKeys.VAULT_SERVER);
    }

    boolean isIncludeGlobal() {
        return plugin.getConfiguration().get(ConfigKeys.VAULT_INCLUDING_GLOBAL);
    }

    boolean isIgnoreWorld() {
        return plugin.getConfiguration().get(ConfigKeys.VAULT_IGNORE_WORLD);
    }

    private boolean useVaultServer() {
        return plugin.getConfiguration().get(ConfigKeys.USE_VAULT_SERVER);
    }
}
