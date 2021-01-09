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

import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.bukkit.context.BukkitContextManager;
import me.lucko.luckperms.common.cacheddata.type.MetaCache;
import me.lucko.luckperms.common.cacheddata.type.PermissionCache;
import me.lucko.luckperms.common.calculator.processor.MapProcessor;
import me.lucko.luckperms.common.calculator.result.TristateResult;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.query.QueryOptionsImpl;
import me.lucko.luckperms.common.util.UniqueIdType;
import me.lucko.luckperms.common.util.Uuids;
import me.lucko.luckperms.common.verbose.event.MetaCheckEvent;
import me.lucko.luckperms.common.verbose.event.PermissionCheckEvent;

import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.QueryOptions;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * An implementation of the Vault {@link Permission} API using LuckPerms.
 *
 * LuckPerms is a multithreaded permissions plugin, and some actions require considerable
 * time to execute. (database queries, re-population of caches, etc) In these cases, the
 * operations required to make the edit apply will be processed immediately, but the process
 * of saving the change to the plugin storage will happen in the background.
 *
 * Methods that have to query data from the database will throw exceptions when called
 * from the main thread. Users of the Vault API expect these methods to be "main thread friendly",
 * which they simply cannot be, as LP utilises databases for data storage. Server admins
 * willing to take the risk of lagging their server can disable these exceptions in the config file.
 */
public class LuckPermsVaultPermission extends AbstractVaultPermission {

    // the plugin instance
    private final LPBukkitPlugin plugin;

    public LuckPermsVaultPermission(LPBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "LuckPerms";
    }

    @Override
    protected String convertWorld(String world) {
        return isIgnoreWorld() ? null : super.convertWorld(world);
    }

    @Override
    public UUID lookupUuid(String player) {
        Objects.requireNonNull(player, "player");

        // are they online?
        Player onlinePlayer = this.plugin.getBootstrap().getServer().getPlayerExact(player);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId();
        }

        // account for plugins that for some reason think it's valid to pass the uuid as a string.
        UUID uuid = Uuids.parse(player);
        if (uuid != null) {
            return uuid;
        }

        // are we on the main thread?
        if (!this.plugin.getBootstrap().isServerStarting() && this.plugin.getBootstrap().getServer().isPrimaryThread() && !this.plugin.getConfiguration().get(ConfigKeys.VAULT_UNSAFE_LOOKUPS)) {
            throw new RuntimeException(
                    "The operation to lookup a UUID for '" + player + "' was cancelled by LuckPerms. This is NOT a bug. \n" +
                    "The lookup request was made on the main server thread. It is not safe to execute a request to \n" +
                    "load username data from the database in this context. \n" +
                    "If you are a plugin author, please either make your request asynchronously, \n" +
                    "or provide an 'OfflinePlayer' object with the UUID already populated. \n" +
                    "Alternatively, server admins can disable this catch by setting 'vault-unsafe-lookups' to true \n" +
                    "in the LP config, but should consider the consequences (lag) before doing so."
            );
        }

        // lookup a username from the database
        uuid = this.plugin.lookupUniqueId(player).orElse(null);

        // unable to find a user, throw an exception
        if (uuid == null) {
            throw new IllegalArgumentException("Unable to find a UUID for player '" + player + "'.");
        }

        return uuid;
    }

    public PermissionHolder lookupUser(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        // loaded already?
        User user = this.plugin.getUserManager().getIfLoaded(uuid);
        if (user != null) {
            return user;
        }

        // is it an npc?
        if (UniqueIdType.determineType(uuid, this.plugin).getType().equals("npc")) {
            String npcGroupName = this.plugin.getConfiguration().get(ConfigKeys.VAULT_NPC_GROUP);
            Group npcGroup = this.plugin.getGroupManager().getIfLoaded(npcGroupName);
            if (npcGroup == null) {
                npcGroup = this.plugin.getGroupManager().getIfLoaded(GroupManager.DEFAULT_GROUP_NAME);
                if (npcGroup == null) {
                    throw new IllegalStateException("unable to get default group");
                }
            }
            return npcGroup;
        }

        // are we on the main thread?
        if (!this.plugin.getBootstrap().isServerStarting() && this.plugin.getBootstrap().getServer().isPrimaryThread() && !this.plugin.getConfiguration().get(ConfigKeys.VAULT_UNSAFE_LOOKUPS)) {
            throw new RuntimeException(
                    "The operation to load user data for '" + uuid + "' was cancelled by LuckPerms. This is NOT a bug. \n" +
                    "The lookup request was made on the main server thread. It is not safe to execute a request to \n" +
                    "load data for offline players from the database in this context. \n" +
                    "If you are a plugin author, please consider making your request asynchronously. \n" +
                    "Alternatively, server admins can disable this catch by setting 'vault-unsafe-lookups' to true \n" +
                    "in the LP config, but should consider the consequences (lag) before doing so."
            );
        }

        // load an instance from the DB
        return this.plugin.getStorage().loadUser(uuid, null).join();
    }

    @Override
    public String[] getGroups() {
        return this.plugin.getGroupManager().getAll().values().stream()
                .map(this::groupName)
                .toArray(String[]::new);
    }

    @Override
    public boolean userHasPermission(String world, UUID uuid, String permission) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(permission, "permission");

        PermissionHolder user = lookupUser(uuid);
        QueryOptions queryOptions = getQueryOptions(uuid, world);
        PermissionCache permissionData = user.getCachedData().getPermissionData(queryOptions);
        return permissionData.checkPermission(permission, PermissionCheckEvent.Origin.THIRD_PARTY_API).result().asBoolean();
    }

    @Override
    public boolean userAddPermission(String world, UUID uuid, String permission) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(permission, "permission");

        PermissionHolder user = lookupUser(uuid);
        if (user instanceof Group) {
            throw new UnsupportedOperationException("Unable to modify the permissions of NPC players");
        }
        return holderAddPermission(user, permission, world);
    }

    @Override
    public boolean userRemovePermission(String world, UUID uuid, String permission) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(permission, "permission");

        PermissionHolder user = lookupUser(uuid);
        if (user instanceof Group) {
            throw new UnsupportedOperationException("Unable to modify the permissions of NPC players");
        }
        return holderRemovePermission(user, permission, world);
    }

    @Override
    public boolean userInGroup(String world, UUID uuid, String group) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(group, "group");

        PermissionHolder user = lookupUser(uuid);
        QueryOptions queryOptions = getQueryOptions(uuid, world);
        PermissionCache permissionData = user.getCachedData().getPermissionData(queryOptions);

        TristateResult result = permissionData.checkPermission(Inheritance.key(rewriteGroupName(group)), PermissionCheckEvent.Origin.THIRD_PARTY_API);
        return result.processorClass() == MapProcessor.class && result.result().asBoolean();
    }

    @Override
    public boolean userAddGroup(String world, UUID uuid, String group) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(group, "group");
        return checkGroupExists(group) && userAddPermission(world, uuid, Inheritance.key(rewriteGroupName(group)));
    }

    @Override
    public boolean userRemoveGroup(String world, UUID uuid, String group) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(group, "group");
        return checkGroupExists(group) && userRemovePermission(world, uuid, Inheritance.key(rewriteGroupName(group)));
    }

    @Override
    public String[] userGetGroups(String world, UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        PermissionHolder user = lookupUser(uuid);
        QueryOptions queryOptions = getQueryOptions(uuid, world);

        return user.getOwnNodes(NodeType.INHERITANCE, queryOptions).stream()
                .map(n -> {
                    Group group = this.plugin.getGroupManager().getIfLoaded(n.getGroupName());
                    return group != null ? groupName(group) : n.getGroupName();
                })
                .toArray(String[]::new);
    }

    @Override
    public String userGetPrimaryGroup(String world, UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        PermissionHolder user = lookupUser(uuid);
        if (user instanceof Group) { // npc
            return this.plugin.getConfiguration().get(ConfigKeys.VAULT_NPC_GROUP);
        }

        QueryOptions queryOptions = getQueryOptions(uuid, world);
        MetaCache metaData = user.getCachedData().getMetaData(queryOptions);
        String value = metaData.getPrimaryGroup(MetaCheckEvent.Origin.THIRD_PARTY_API);

        Group group = getGroup(value);
        return group != null ? groupName(group) : value;
    }

    @Override
    public boolean groupHasPermission(String world, String name, String permission) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(permission, "permission");

        Group group = getGroup(name);
        if (group == null) {
            return false;
        }

        QueryOptions queryOptions = getQueryOptions(null, world);
        PermissionCache permissionData = group.getCachedData().getPermissionData(queryOptions);
        return permissionData.checkPermission(permission, PermissionCheckEvent.Origin.THIRD_PARTY_API).result().asBoolean();
    }

    @Override
    public boolean groupAddPermission(String world, String name, String permission) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(permission, "permission");

        Group group = getGroup(name);
        if (group == null) {
            return false;
        }

        return holderAddPermission(group, permission, world);
    }

    @Override
    public boolean groupRemovePermission(String world, String name, String permission) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(permission, "permission");

        Group group = getGroup(name);
        if (group == null) {
            return false;
        }

        return holderRemovePermission(group, permission, world);
    }

    // utility methods for getting user and group instances

    private Group getGroup(String name) {
        return this.plugin.getGroupManager().getByDisplayName(name);
    }

    private String groupName(Group group) {
        if (this.plugin.getConfiguration().get(ConfigKeys.VAULT_GROUP_USE_DISPLAYNAMES)) {
            return group.getPlainDisplayName();
        } else {
            return group.getName();
        }
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

    // utility method for getting a contexts instance for a given vault lookup.
    QueryOptions getQueryOptions(@Nullable UUID uuid, @Nullable String world) {
        MutableContextSet context;

        Player player = Optional.ofNullable(uuid).flatMap(u -> this.plugin.getBootstrap().getPlayer(u)).orElse(null);
        if (player != null) {
            context = this.plugin.getContextManager().getContext(player).mutableCopy();
        } else {
            context = this.plugin.getContextManager().getStaticContext().mutableCopy();
        }

        String playerWorld = player == null ? null : player.getWorld().getName();

        // if world is null, we want to do a lookup in the players current context
        // if world is not null, we want to do a lookup in that specific world
        if (world != null && !world.isEmpty() && !world.equalsIgnoreCase(playerWorld)) {
            // remove already accumulated worlds
            context.removeAll(DefaultContextKeys.WORLD_KEY);
            // add the vault world
            context.add(DefaultContextKeys.WORLD_KEY, world.toLowerCase());
        }

        // if we're using a special vault server
        if (useVaultServer()) {
            // remove the normal server context from the set
            context.remove(DefaultContextKeys.SERVER_KEY, getServer());

            // add the vault specific server
            if (!getVaultServer().equals("global")) {
                context.add(DefaultContextKeys.SERVER_KEY, getVaultServer());
            }
        }

        boolean op = false;
        if (player != null) {
            op = player.isOp();
        } else if (uuid != null && UniqueIdType.determineType(uuid, this.plugin).getType().equals("npc")) {
            op = this.plugin.getConfiguration().get(ConfigKeys.VAULT_NPC_OP_STATUS);
        }

        QueryOptions.Builder builder = QueryOptionsImpl.DEFAULT_CONTEXTUAL.toBuilder();
        builder.context(context);
        builder.flag(Flag.INCLUDE_NODES_WITHOUT_SERVER_CONTEXT, isIncludeGlobal());
        if (op) {
            builder.option(BukkitContextManager.OP_OPTION, true);
        }
        return builder.build();
    }

    // utility methods for modifying the state of PermissionHolders

    private boolean holderAddPermission(PermissionHolder holder, String permission, String world) {
        Objects.requireNonNull(permission, "permission is null");
        Preconditions.checkArgument(!permission.isEmpty(), "permission is an empty string");

        Node node = NodeBuilders.determineMostApplicable(permission)
                .withContext(DefaultContextKeys.SERVER_KEY, getVaultServer())
                .withContext(DefaultContextKeys.WORLD_KEY, world == null ? "global" : world)
                .build();

        if (holder.setNode(DataType.NORMAL, node, true).wasSuccessful()) {
            return holderSave(holder);
        }
        return false;
    }

    private boolean holderRemovePermission(PermissionHolder holder, String permission, String world) {
        Objects.requireNonNull(permission, "permission is null");
        Preconditions.checkArgument(!permission.isEmpty(), "permission is an empty string");

        Node node = NodeBuilders.determineMostApplicable(permission)
                .withContext(DefaultContextKeys.SERVER_KEY, getVaultServer())
                .withContext(DefaultContextKeys.WORLD_KEY, world == null ? "global" : world)
                .build();

        if (holder.unsetNode(DataType.NORMAL, node).wasSuccessful()) {
            return holderSave(holder);
        }
        return false;
    }

    boolean holderSave(PermissionHolder holder) {
        if (holder.getType() == HolderType.USER) {
            User u = (User) holder;

            // we don't need to join this call - the save operation
            // can happen in the background.
            this.plugin.getStorage().saveUser(u);
        } else if (holder.getType() == HolderType.GROUP) {
            Group g = (Group) holder;

            // invalidate caches - they have potentially been affected by
            // this change.
            this.plugin.getGroupManager().invalidateAllGroupCaches();
            this.plugin.getUserManager().invalidateAllUserCaches();

            // we don't need to join this call - the save operation
            // can happen in the background.
            this.plugin.getStorage().saveGroup(g);
        }
        return true;
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
