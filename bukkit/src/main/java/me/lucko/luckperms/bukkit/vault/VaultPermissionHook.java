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

import com.google.common.base.Preconditions;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.caching.type.PermissionCache;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.factory.NodeFactory;
import me.lucko.luckperms.common.verbose.CheckOrigin;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
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

    public LPBukkitPlugin getPlugin() {
        return this.plugin;
    }

    public ExecutorService getExecutor() {
        return this.executor;
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
        return this.plugin.getGroupManager().getAll().values().stream()
                .map(g -> g.getDisplayName().orElse(g.getName()))
                .toArray(String[]::new);
    }

    @Override
    public boolean userHasPermission(String world, UUID uuid, String permission) {
        if (uuid == null) {
            return false;
        }
        Objects.requireNonNull(permission, "permission");

        User user = getUser(uuid);
        if (user == null) {
            return false;
        }

        Contexts contexts = contextForLookup(user, world);
        PermissionCache permissionData = user.getCachedData().getPermissionData(contexts);

        Tristate result = permissionData.getPermissionValue(permission, CheckOrigin.INTERNAL);
        if (log()) {
            logMsg("#userHasPermission: %s - %s - %s - %s", user.getFriendlyName(), contexts.getContexts().toMultimap(), permission, result);
        }
        return result.asBoolean();
    }

    @Override
    public boolean userAddPermission(String world, UUID uuid, String permission) {
        if (uuid == null) {
            return false;
        }
        Objects.requireNonNull(permission, "permission");

        User user = getUser(uuid);
        if (user == null) {
            return false;
        }

        holderAddPermission(user, permission, world);
        return true;
    }

    @Override
    public boolean userRemovePermission(String world, UUID uuid, String permission) {
        if (uuid == null) {
            return false;
        }
        Objects.requireNonNull(permission, "permission");

        User user = getUser(uuid);
        if (user == null) {
            return false;
        }

        holderRemovePermission(user, permission, world);
        return true;
    }

    @Override
    public boolean userInGroup(String world, UUID uuid, String group) {
        if (uuid == null) {
            return false;
        }
        Objects.requireNonNull(group, "group");
        return userHasPermission(world, uuid, NodeFactory.groupNode(rewriteGroupName(group)));
    }

    @Override
    public boolean userAddGroup(String world, UUID uuid, String group) {
        if (uuid == null) {
            return false;
        }
        Objects.requireNonNull(group, "group");
        return checkGroupExists(group) && userAddPermission(world, uuid, NodeFactory.groupNode(rewriteGroupName(group)));
    }

    @Override
    public boolean userRemoveGroup(String world, UUID uuid, String group) {
        if (uuid == null) {
            return false;
        }
        Objects.requireNonNull(group, "group");
        return checkGroupExists(group) && userRemovePermission(world, uuid, NodeFactory.groupNode(rewriteGroupName(group)));
    }

    @Override
    public String[] userGetGroups(String world, UUID uuid) {
        if (uuid == null) {
            return new String[0];
        }

        User user = getUser(uuid);
        if (user == null) {
            return new String[0];
        }

        ContextSet contexts = contextForLookup(user, world).getContexts();

        String[] ret = user.enduringData().immutable().values().stream()
                .filter(Node::isGroupNode)
                .filter(n -> n.shouldApplyWithContext(contexts))
                .map(n -> {
                    Group group = this.plugin.getGroupManager().getIfLoaded(n.getGroupName());
                    if (group != null) {
                        return group.getDisplayName().orElse(group.getName());
                    }
                    return n.getGroupName();
                })
                .toArray(String[]::new);

        if (log()) {
            logMsg("#userGetGroups: %s - %s - %s", user.getFriendlyName(), contexts, Arrays.toString(ret));
        }

        return ret;
    }

    @Override
    public String userGetPrimaryGroup(String world, UUID uuid) {
        if (uuid == null) {
            return null;
        }

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
            logMsg("#userGetPrimaryGroup: %s - %s - %s", user.getFriendlyName(), world, value);
        }

        return value;
    }

    @Override
    public boolean groupHasPermission(String world, String name, String permission) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(permission, "permission");

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
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(permission, "permission");

        Group group = getGroup(name);
        if (group == null) {
            return false;
        }

        holderAddPermission(group, permission, world);
        return true;
    }

    @Override
    public boolean groupRemovePermission(String world, String name, String permission) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(permission, "permission");

        Group group = getGroup(name);
        if (group == null) {
            return false;
        }

        holderRemovePermission(group, permission, world);
        return true;
    }

    // utility methods for getting user and group instances

    private User getUser(UUID uuid) {
        return this.plugin.getUserManager().getIfLoaded(uuid);
    }

    private Group getGroup(String name) {
        return this.plugin.getGroupManager().getByDisplayName(name);
    }

    private boolean checkGroupExists(String group) {
        return this.plugin.getGroupManager().getByDisplayName(group) != null;
    }

    private String rewriteGroupName(String name) {
        Group group = this.plugin.getGroupManager().getByDisplayName(name);
        if (group != null) {
            return group.getName();
        }
        return name;
    }

    // logging
    private boolean log() {
        return this.plugin.getConfiguration().get(ConfigKeys.VAULT_DEBUG);
    }
    private void logMsg(String format, Object... args) {
        this.plugin.getLogger().info("[VAULT-PERMS] " + String.format(format, args)
                .replace(CommandManager.SECTION_CHAR, '$')
                .replace(CommandManager.AMPERSAND_CHAR, '$')
        );
    }

    // utility method for getting a contexts instance for a given vault lookup.
    Contexts contextForLookup(User user, String world) {
        MutableContextSet context;

        Player player = Optional.ofNullable(user).flatMap(u -> this.plugin.getBootstrap().getPlayer(u.getUuid())).orElse(null);
        if (player != null) {
            context = this.plugin.getContextManager().getApplicableContext(player).mutableCopy();
        } else {
            context = this.plugin.getContextManager().getStaticContext().mutableCopy();
        }

        String playerWorld = player == null ? null : player.getWorld().getName();

        // if world is null, we want to do a lookup in the players current context
        // if world is not null, we want to do a lookup in that specific world
        if (world != null && !world.isEmpty() && !world.equalsIgnoreCase(playerWorld)) {
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

        return Contexts.of(context, isIncludeGlobal(), true, true, true, true, false);
    }

    // utility methods for modifying the state of PermissionHolders

    private void holderAddPermission(PermissionHolder holder, String permission, String world) {
        Objects.requireNonNull(permission, "permission is null");
        Preconditions.checkArgument(!permission.isEmpty(), "permission is an empty string");

        if (log()) {
            logMsg("#holderAddPermission: %s - %s - %s", holder.getFriendlyName(), permission, world);
        }

        this.executor.execute(() -> {
            if (holder.setPermission(NodeFactory.make(permission, true, getVaultServer(), world)).asBoolean()) {
                holderSave(holder);
            }
        });
    }

    private void holderRemovePermission(PermissionHolder holder, String permission, String world) {
        Objects.requireNonNull(permission, "permission is null");
        Preconditions.checkArgument(!permission.isEmpty(), "permission is an empty string");

        if (log()) {
            logMsg("#holderRemovePermission: %s - %s - %s", holder.getFriendlyName(), permission, world);
        }

        this.executor.execute(() -> {
            if (holder.unsetPermission(NodeFactory.make(permission, getVaultServer(), world)).asBoolean()) {
                holderSave(holder);
            }
        });
    }

    void holderSave(PermissionHolder holder) {
        if (holder.getType().isUser()) {
            User u = (User) holder;
            this.plugin.getStorage().saveUser(u);
        }
        if (holder.getType().isGroup()) {
            Group g = (Group) holder;
            this.plugin.getStorage().saveGroup(g).thenRunAsync(() -> this.plugin.getUpdateTaskBuffer().request(), this.plugin.getBootstrap().getScheduler().async());
        }
    }

    // helper methods to just pull values from the config.

    String getServer() {
        return this.plugin.getConfiguration().get(ConfigKeys.SERVER);
    }

    String getVaultServer() {
        return this.plugin.getConfiguration().get(ConfigKeys.VAULT_SERVER);
    }

    boolean isIncludeGlobal() {
        return this.plugin.getConfiguration().get(ConfigKeys.VAULT_INCLUDING_GLOBAL);
    }

    boolean isIgnoreWorld() {
        return this.plugin.getConfiguration().get(ConfigKeys.VAULT_IGNORE_WORLD);
    }

    private boolean useVaultServer() {
        return this.plugin.getConfiguration().get(ConfigKeys.USE_VAULT_SERVER);
    }
}
