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

import com.google.common.base.Strings;

import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.cacheddata.type.MetaAccumulator;
import me.lucko.luckperms.common.cacheddata.type.MetaCache;
import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.Suffix;
import me.lucko.luckperms.common.query.QueryOptionsImpl;
import me.lucko.luckperms.common.verbose.event.MetaCheckEvent;

import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.QueryOptions;
import net.milkbowl.vault.chat.Chat;

import java.util.Objects;
import java.util.UUID;

/**
 * An implementation of the Vault {@link Chat} API using LuckPerms.
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
public class LuckPermsVaultChat extends AbstractVaultChat {

    // the plugin instance
    private final LPBukkitPlugin plugin;

    // the vault permission implementation
    private final LuckPermsVaultPermission vaultPermission;

    LuckPermsVaultChat(LPBukkitPlugin plugin, LuckPermsVaultPermission vaultPermission) {
        super(vaultPermission);
        this.plugin = plugin;
        this.vaultPermission = vaultPermission;
    }

    @Override
    public String getName() {
        return "LuckPerms";
    }

    @Override
    protected String convertWorld(String world) {
        return this.vaultPermission.isIgnoreWorld() ? null : super.convertWorld(world);
    }

    @Override
    public String getUserChatPrefix(String world, UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        PermissionHolder user = this.vaultPermission.lookupUser(uuid);
        QueryOptions queryOptions = this.vaultPermission.getQueryOptions(uuid, world);
        MetaCache metaData = user.getCachedData().getMetaData(queryOptions);
        return Strings.nullToEmpty(metaData.getPrefix(MetaCheckEvent.Origin.THIRD_PARTY_API));
    }

    @Override
    public String getUserChatSuffix(String world, UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        PermissionHolder user = this.vaultPermission.lookupUser(uuid);
        QueryOptions queryOptions = this.vaultPermission.getQueryOptions(uuid, world);
        MetaCache metaData = user.getCachedData().getMetaData(queryOptions);
        return Strings.nullToEmpty(metaData.getSuffix(MetaCheckEvent.Origin.THIRD_PARTY_API));
    }

    @Override
    public void setUserChatPrefix(String world, UUID uuid, String prefix) {
        Objects.requireNonNull(uuid, "uuid");

        PermissionHolder user = this.vaultPermission.lookupUser(uuid);
        if (user instanceof Group) {
            throw new UnsupportedOperationException("Unable to modify the permissions of NPC players");
        }
        setChatMeta(user, ChatMetaType.PREFIX, prefix, world);
    }

    @Override
    public void setUserChatSuffix(String world, UUID uuid, String suffix) {
        Objects.requireNonNull(uuid, "uuid");

        PermissionHolder user = this.vaultPermission.lookupUser(uuid);
        if (user instanceof Group) {
            throw new UnsupportedOperationException("Unable to modify the permissions of NPC players");
        }
        setChatMeta(user, ChatMetaType.SUFFIX, suffix, world);
    }

    @Override
    public String getUserMeta(String world, UUID uuid, String key) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(key, "key");

        PermissionHolder user = this.vaultPermission.lookupUser(uuid);
        QueryOptions queryOptions = this.vaultPermission.getQueryOptions(uuid, world);
        MetaCache metaData = user.getCachedData().getMetaData(queryOptions);
        return metaData.getMetaValue(key, MetaCheckEvent.Origin.THIRD_PARTY_API);
    }

    @Override
    public void setUserMeta(String world, UUID uuid, String key, Object value) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(key, "key");

        PermissionHolder user = this.vaultPermission.lookupUser(uuid);
        if (user instanceof Group) {
            throw new UnsupportedOperationException("Unable to modify the permissions of NPC players");
        }
        setMeta(user, key, value, world);
    }

    @Override
    public String getGroupChatPrefix(String world, String name) {
        Objects.requireNonNull(name, "name");
        MetaCache metaData = getGroupMetaCache(name, world);
        if (metaData == null) {
            return null;
        }
        return Strings.nullToEmpty(metaData.getPrefix(MetaCheckEvent.Origin.THIRD_PARTY_API));
    }

    @Override
    public String getGroupChatSuffix(String world, String name) {
        Objects.requireNonNull(name, "name");
        MetaCache metaData = getGroupMetaCache(name, world);
        if (metaData == null) {
            return null;
        }
        return Strings.nullToEmpty(metaData.getSuffix(MetaCheckEvent.Origin.THIRD_PARTY_API));
    }

    @Override
    public void setGroupChatPrefix(String world, String name, String prefix) {
        Objects.requireNonNull(name, "name");
        Group group = getGroup(name);
        if (group == null) {
            return;
        }
        setChatMeta(group, ChatMetaType.PREFIX, prefix, world);
    }

    @Override
    public void setGroupChatSuffix(String world, String name, String suffix) {
        Objects.requireNonNull(name, "name");
        Group group = getGroup(name);
        if (group == null) {
            return;
        }
        setChatMeta(group, ChatMetaType.SUFFIX, suffix, world);
    }

    @Override
    public String getGroupMeta(String world, String name, String key) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(key, "key");
        MetaCache metaData = getGroupMetaCache(name, world);
        if (metaData == null) {
            return null;
        }
        return metaData.getMetaValue(key, MetaCheckEvent.Origin.THIRD_PARTY_API);
    }

    @Override
    public void setGroupMeta(String world, String name, String key, Object value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(key, "key");
        Group group = getGroup(name);
        if (group == null) {
            return;
        }
        setMeta(group, key, value, world);
    }

    // utility methods for getting user and group instances

    private Group getGroup(String name) {
        return this.plugin.getGroupManager().getByDisplayName(name);
    }

    private MetaCache getGroupMetaCache(String name, String world) {
        Group group = getGroup(name);
        if (group == null) {
            return null;
        }
        QueryOptions queryOptions = this.vaultPermission.getQueryOptions(null, world);
        return group.getCachedData().getMetaData(queryOptions);
    }

    private void setChatMeta(PermissionHolder holder, ChatMetaType type, String value, String world) {
        // remove all prefixes/suffixes directly set on the user/group
        holder.removeIf(DataType.NORMAL, null, type.nodeType()::matches, false);

        if (value == null) {
            this.vaultPermission.holderSave(holder);
            return;
        }

        // find the max inherited priority & add 10
        MetaAccumulator metaAccumulator = holder.accumulateMeta(createQueryOptionsForWorldSet(world));
        int priority = metaAccumulator.getChatMeta(type).keySet().stream().mapToInt(e -> e).max().orElse(0) + 10;

        Node node = type.builder(value, priority)
                .withContext(DefaultContextKeys.SERVER_KEY, this.vaultPermission.getVaultServer())
                .withContext(DefaultContextKeys.WORLD_KEY, world == null ? "global" : world).build();

        holder.setNode(DataType.NORMAL, node, true);
        this.vaultPermission.holderSave(holder);
    }

    private void setMeta(PermissionHolder holder, String key, Object value, String world) {
        if (key.equalsIgnoreCase(Prefix.NODE_KEY) || key.equalsIgnoreCase(Suffix.NODE_KEY)) {
            setChatMeta(holder, ChatMetaType.valueOf(key.toUpperCase()), value == null ? null : value.toString(), world);
            return;
        }

        holder.removeIf(DataType.NORMAL, null, NodeType.META.predicate(n -> n.getMetaKey().equals(key)), false);

        if (value == null) {
            this.vaultPermission.holderSave(holder);
            return;
        }

        Node node = Meta.builder(key, value.toString())
                .withContext(DefaultContextKeys.SERVER_KEY, this.vaultPermission.getVaultServer())
                .withContext(DefaultContextKeys.WORLD_KEY, world == null ? "global" : world)
                .build();

        holder.setNode(DataType.NORMAL, node, true);
        this.vaultPermission.holderSave(holder);
    }

    private QueryOptions createQueryOptionsForWorldSet(String world) {
        ImmutableContextSet.Builder context = new ImmutableContextSetImpl.BuilderImpl();
        if (world != null && !world.isEmpty() && !world.equalsIgnoreCase("global")) {
            context.add(DefaultContextKeys.WORLD_KEY, world.toLowerCase());
        }
        context.add(DefaultContextKeys.SERVER_KEY, this.vaultPermission.getVaultServer());

        QueryOptions.Builder builder = QueryOptionsImpl.DEFAULT_CONTEXTUAL.toBuilder();
        builder.context(context.build());
        builder.flag(Flag.INCLUDE_NODES_WITHOUT_SERVER_CONTEXT, this.vaultPermission.isIncludeGlobal());
        return builder.build();
    }
}
