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

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.caching.type.MetaAccumulator;
import me.lucko.luckperms.common.caching.type.MetaCache;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.factory.NodeFactory;
import me.lucko.luckperms.common.node.model.NodeTypes;

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
public class VaultChatHook extends AbstractVaultChat {

    // the plugin instance
    private final LPBukkitPlugin plugin;

    // the vault permission implementation
    private final VaultPermissionHook permissionHook;

    VaultChatHook(LPBukkitPlugin plugin, VaultPermissionHook permissionHook) {
        super(permissionHook);
        this.plugin = plugin;
        this.permissionHook = permissionHook;
        this.worldMappingFunction = world -> permissionHook.isIgnoreWorld() ? null : world;
    }

    @Override
    public String getName() {
        return "LuckPerms";
    }

    @Override
    public String getUserChatPrefix(String world, UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        User user = this.permissionHook.lookupUser(uuid);
        Contexts contexts = this.permissionHook.contextForLookup(user, world);
        MetaCache metaData = user.getCachedData().getMetaData(contexts);
        String ret = metaData.getPrefix();
        if (log()) {
            logMsg("#getUserChatPrefix: %s - %s - %s", user.getPlainDisplayName(), contexts.getContexts().toMultimap(), ret);
        }
        return Strings.nullToEmpty(ret);
    }

    @Override
    public String getUserChatSuffix(String world, UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        User user = this.permissionHook.lookupUser(uuid);
        Contexts contexts = this.permissionHook.contextForLookup(user, world);
        MetaCache metaData = user.getCachedData().getMetaData(contexts);
        String ret = metaData.getSuffix();
        if (log()) {
            logMsg("#getUserChatSuffix: %s - %s - %s", user.getPlainDisplayName(), contexts.getContexts().toMultimap(), ret);
        }
        return Strings.nullToEmpty(ret);
    }

    @Override
    public void setUserChatPrefix(String world, UUID uuid, String prefix) {
        Objects.requireNonNull(uuid, "uuid");

        User user = this.permissionHook.lookupUser(uuid);
        setChatMeta(user, ChatMetaType.PREFIX, prefix, world);
    }

    @Override
    public void setUserChatSuffix(String world, UUID uuid, String suffix) {
        Objects.requireNonNull(uuid, "uuid");

        User user = this.permissionHook.lookupUser(uuid);
        setChatMeta(user, ChatMetaType.SUFFIX, suffix, world);
    }

    @Override
    public String getUserMeta(String world, UUID uuid, String key) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(key, "key");

        User user = this.permissionHook.lookupUser(uuid);
        Contexts contexts = this.permissionHook.contextForLookup(user, world);
        MetaCache metaData = user.getCachedData().getMetaData(contexts);
        String ret = metaData.getMeta().get(key);
        if (log()) {
            logMsg("#getUserMeta: %s - %s - %s - %s", user.getPlainDisplayName(), contexts.getContexts().toMultimap(), key, ret);
        }
        return ret;
    }

    @Override
    public void setUserMeta(String world, UUID uuid, String key, Object value) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(key, "key");

        User user = this.permissionHook.lookupUser(uuid);
        setMeta(user, key, value, world);
    }

    @Override
    public String getGroupChatPrefix(String world, String name) {
        Objects.requireNonNull(name, "name");
        Group group = getGroup(name);
        if (group == null) {
            return null;
        }
        Contexts contexts = this.permissionHook.contextForLookup(null, world);
        MetaCache metaData = group.getCachedData().getMetaData(contexts);
        String ret = metaData.getPrefix();
        if (log()) {
            logMsg("#getGroupPrefix: %s - %s - %s", group.getName(), contexts.getContexts().toMultimap(), ret);
        }
        return Strings.nullToEmpty(ret);
    }

    @Override
    public String getGroupChatSuffix(String world, String name) {
        Objects.requireNonNull(name, "name");
        Group group = getGroup(name);
        if (group == null) {
            return null;
        }
        Contexts contexts = this.permissionHook.contextForLookup(null, world);
        MetaCache metaData = group.getCachedData().getMetaData(contexts);
        String ret = metaData.getSuffix();
        if (log()) {
            logMsg("#getGroupSuffix: %s - %s - %s", group.getName(), contexts.getContexts().toMultimap(), ret);
        }
        return Strings.nullToEmpty(ret);
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
        Group group = getGroup(name);
        if (group == null) {
            return null;
        }
        Contexts contexts = this.permissionHook.contextForLookup(null, world);
        MetaCache metaData = group.getCachedData().getMetaData(contexts);
        String ret = metaData.getMeta().get(key);
        if (log()) {
            logMsg("#getGroupMeta: %s - %s - %s - %s", group.getName(), contexts.getContexts().toMultimap(), key, ret);
        }
        return ret;
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

    // logging
    private boolean log() {
        return this.plugin.getConfiguration().get(ConfigKeys.VAULT_DEBUG);
    }
    private void logMsg(String format, Object... args) {
        this.plugin.getLogger().info("[VAULT-CHAT] " + String.format(format, args)
                .replace(CommandManager.SECTION_CHAR, '$')
                .replace(CommandManager.AMPERSAND_CHAR, '$')
        );
    }

    private void setChatMeta(PermissionHolder holder, ChatMetaType type, String value, String world) {
        if (log()) {
            logMsg("#setChatMeta: %s - %s - %s - %s", holder.getPlainDisplayName(), type, value, world);
        }

        // remove all prefixes/suffixes directly set on the user/group
        holder.removeIf(type::matches);

        if (value == null) {
            this.permissionHook.holderSave(holder);
            return;
        }

        // find the max inherited priority & add 10
        MetaAccumulator metaAccumulator = holder.accumulateMeta(null, createContextForWorldSet(world));
        metaAccumulator.complete();
        int priority = metaAccumulator.getChatMeta(type).keySet().stream().mapToInt(e -> e).max().orElse(0) + 10;

        Node.Builder chatMetaNode = NodeFactory.buildChatMetaNode(type, priority, value);
        chatMetaNode.setServer(this.permissionHook.getVaultServer());
        chatMetaNode.setWorld(world);

        holder.setPermission(chatMetaNode.build()); // assume success
        this.permissionHook.holderSave(holder);
    }

    private void setMeta(PermissionHolder holder, String key, Object value, String world) {
        if (log()) {
            logMsg("#setMeta: %s - %s - %s - %s", holder.getPlainDisplayName(), key, value, world);
        }

        holder.removeIf(n -> n.isMeta() && n.getMeta().getKey().equals(key));

        if (value == null) {
            this.permissionHook.holderSave(holder);
            return;
        }

        Node.Builder metaNode;
        if (key.equalsIgnoreCase(NodeTypes.PREFIX_KEY) || key.equalsIgnoreCase(NodeTypes.SUFFIX_KEY)) {
            metaNode = NodeFactory.buildChatMetaNode(ChatMetaType.valueOf(key.toUpperCase()), 100, value.toString());
        } else {
            metaNode = NodeFactory.buildMetaNode(key, value.toString());
        }

        metaNode.setServer(this.permissionHook.getVaultServer());
        metaNode.setWorld(world);

        holder.setPermission(metaNode.build()); // assume success
        this.permissionHook.holderSave(holder);
    }

    private Contexts createContextForWorldSet(String world) {
        ImmutableContextSet.Builder context = ImmutableContextSet.builder();
        if (world != null && !world.equals("") && !world.equalsIgnoreCase("global")) {
            context.add(Contexts.WORLD_KEY, world.toLowerCase());
        }
        context.add(Contexts.SERVER_KEY, this.permissionHook.getVaultServer());
        return Contexts.of(context.build(), this.permissionHook.isIncludeGlobal(), true, true, true, true, false);
    }
}
