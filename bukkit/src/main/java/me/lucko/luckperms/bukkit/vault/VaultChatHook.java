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
import com.google.common.base.Strings;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.caching.type.MetaAccumulator;
import me.lucko.luckperms.common.caching.type.MetaCache;
import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeFactory;

import net.milkbowl.vault.chat.Chat;

import java.util.UUID;

/**
 * An implementation of the Vault {@link Chat} API using LuckPerms.
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

    public String getName() {
        return "LuckPerms";
    }

    @Override
    public String getPlayerPrefix(String world, UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid");
        User user = getUser(uuid);
        if (user == null) {
            return null;
        }
        Contexts contexts = permissionHook.contextForLookup(user, world);
        MetaCache metaData = user.getCachedData().getMetaData(contexts);
        String ret = metaData.getPrefix();
        if (log()) {
            logMsg("#getPlayerPrefix: %s - %s - %s", user.getFriendlyName(), contexts.getContexts().toMultimap(), ret);
        }
        return Strings.nullToEmpty(ret);
    }

    @Override
    public String getPlayerSuffix(String world, UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid");
        User user = getUser(uuid);
        if (user == null) {
            return null;
        }
        Contexts contexts = permissionHook.contextForLookup(user, world);
        MetaCache metaData = user.getCachedData().getMetaData(contexts);
        String ret = metaData.getSuffix();
        if (log()) {
            logMsg("#getPlayerSuffix: %s - %s - %s", user.getFriendlyName(), contexts.getContexts().toMultimap(), ret);
        }
        return Strings.nullToEmpty(ret);
    }

    @Override
    public void setPlayerPrefix(String world, UUID uuid, String prefix) {
        Preconditions.checkNotNull(uuid, "uuid");
        User user = getUser(uuid);
        if (user == null) {
            return;
        }
        setChatMeta(user, ChatMetaType.PREFIX, prefix, world);
    }

    @Override
    public void setPlayerSuffix(String world, UUID uuid, String suffix) {
        Preconditions.checkNotNull(uuid, "uuid");
        User user = getUser(uuid);
        if (user == null) {
            return;
        }
        setChatMeta(user, ChatMetaType.SUFFIX, suffix, world);
    }

    @Override
    public String getPlayerInfo(String world, UUID uuid, String key) {
        Preconditions.checkNotNull(uuid, "uuid");
        Preconditions.checkNotNull(key, "key");
        User user = getUser(uuid);
        if (user == null) {
            return null;
        }
        Contexts contexts = permissionHook.contextForLookup(user, world);
        MetaCache metaData = user.getCachedData().getMetaData(contexts);
        String ret = metaData.getMeta().get(key);
        if (log()) {
            logMsg("#getPlayerInfo: %s - %s - %s - %s", user.getFriendlyName(), contexts.getContexts().toMultimap(), key, ret);
        }
        return ret;
    }

    @Override
    public void setPlayerInfo(String world, UUID uuid, String key, Object value) {
        Preconditions.checkNotNull(uuid, "uuid");
        Preconditions.checkNotNull(key, "key");
        User user = getUser(uuid);
        if (user == null) {
            return;
        }
        setMeta(user, key, value, world);
    }

    @Override
    public String getGroupsPrefix(String world, String name) {
        Preconditions.checkNotNull(name, "name");
        Group group = getGroup(name);
        if (group == null) {
            return null;
        }
        Contexts contexts = permissionHook.contextForLookup(null, world);
        MetaCache metaData = group.getCachedData().getMetaData(contexts);
        String ret = metaData.getPrefix();
        if (log()) {
            logMsg("#getGroupPrefix: %s - %s - %s", group.getName(), contexts.getContexts().toMultimap(), ret);
        }
        return Strings.nullToEmpty(ret);
    }

    @Override
    public String getGroupsSuffix(String world, String name) {
        Preconditions.checkNotNull(name, "name");
        Group group = getGroup(name);
        if (group == null) {
            return null;
        }
        Contexts contexts = permissionHook.contextForLookup(null, world);
        MetaCache metaData = group.getCachedData().getMetaData(contexts);
        String ret = metaData.getSuffix();
        if (log()) {
            logMsg("#getGroupSuffix: %s - %s - %s", group.getName(), contexts.getContexts().toMultimap(), ret);
        }
        return Strings.nullToEmpty(ret);
    }

    @Override
    public void setGroupsPrefix(String world, String name, String prefix) {
        Preconditions.checkNotNull(name, "name");
        Group group = getGroup(name);
        if (group == null) {
            return;
        }
        setChatMeta(group, ChatMetaType.PREFIX, prefix, world);
    }

    @Override
    public void setGroupsSuffix(String world, String name, String suffix) {
        Preconditions.checkNotNull(name, "name");
        Group group = getGroup(name);
        if (group == null) {
            return;
        }
        setChatMeta(group, ChatMetaType.SUFFIX, suffix, world);
    }

    @Override
    public String getGroupInfo(String world, String name, String key) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(key, "key");
        Group group = getGroup(name);
        if (group == null) {
            return null;
        }
        Contexts contexts = permissionHook.contextForLookup(null, world);
        MetaCache metaData = group.getCachedData().getMetaData(contexts);
        String ret = metaData.getMeta().get(key);
        if (log()) {
            logMsg("#getGroupInfo: %s - %s - %s - %s", group.getName(), contexts.getContexts().toMultimap(), key, ret);
        }
        return ret;
    }

    @Override
    public void setGroupInfo(String world, String name, String key, Object value) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(key, "key");
        Group group = getGroup(name);
        if (group == null) {
            return;
        }
        setMeta(group, key, value, world);
    }

    // utility methods for getting user and group instances

    private User getUser(UUID uuid) {
        return plugin.getUserManager().getIfLoaded(plugin.getUuidCache().getUUID(uuid));
    }

    private Group getGroup(String name) {
        return plugin.getGroupManager().getByDisplayName(name);
    }

    // logging
    private boolean log() {
        return plugin.getConfiguration().get(ConfigKeys.VAULT_DEBUG);
    }
    private void logMsg(String format, Object... args) {
        plugin.getLog().info("[VAULT-CHAT] " + String.format(format, args)
                .replace(CommandManager.SECTION_CHAR, '$')
                .replace(CommandManager.AMPERSAND_CHAR, '$')
        );
    }

    private void setChatMeta(PermissionHolder holder, ChatMetaType type, String value, String world) {
        if (log()) {
            logMsg("#setChatMeta: %s - %s - %s - %s", holder.getFriendlyName(), type, value, world);
        }

        permissionHook.getExecutor().execute(() -> {
            // remove all prefixes/suffixes directly set on the user/group
            holder.removeIf(type::matches);

            if (value == null) {
                permissionHook.holderSave(holder);
                return;
            }

            // find the max inherited priority & add 10
            MetaAccumulator metaAccumulator = holder.accumulateMeta(null, null, createContextForWorldSet(world));
            int priority = (type == ChatMetaType.PREFIX ? metaAccumulator.getPrefixes() : metaAccumulator.getSuffixes()).keySet().stream()
                    .mapToInt(e -> e).max().orElse(0) + 10;

            Node.Builder chatMetaNode = NodeFactory.buildChatMetaNode(type, priority, value);
            chatMetaNode.setServer(permissionHook.getVaultServer());
            chatMetaNode.setWorld(world);

            holder.setPermission(chatMetaNode.build());
            permissionHook.holderSave(holder);
        });
    }

    private void setMeta(PermissionHolder holder, String key, Object value, String world) {
        if (log()) {
            logMsg("#setMeta: %s - %s - %s - %s", holder.getFriendlyName(), key, value, world);
        }

        permissionHook.getExecutor().execute(() -> {
            holder.removeIf(n -> n.isMeta() && n.getMeta().getKey().equals(key));

            if (value == null) {
                permissionHook.holderSave(holder);
                return;
            }

            Node.Builder metaNode;
            if (key.equalsIgnoreCase(NodeFactory.PREFIX_KEY) || key.equalsIgnoreCase(NodeFactory.SUFFIX_KEY)) {
                metaNode = NodeFactory.buildChatMetaNode(ChatMetaType.valueOf(key.toUpperCase()), 100, value.toString());
            } else {
                metaNode = NodeFactory.buildMetaNode(key, value.toString());
            }

            metaNode.setServer(permissionHook.getVaultServer());
            metaNode.setWorld(world);

            holder.setPermission(metaNode.build());
            permissionHook.holderSave(holder);
        });
    }

    private Contexts createContextForWorldSet(String world) {
        ImmutableContextSet.Builder context = ImmutableContextSet.builder();
        if (world != null && !world.equals("") && !world.equalsIgnoreCase("global")) {
            context.add(Contexts.WORLD_KEY, world.toLowerCase());
        }
        context.add(Contexts.SERVER_KEY, permissionHook.getVaultServer());
        return new Contexts(context.build(), permissionHook.isIncludeGlobal(), true, true, true, true, false);
    }
}
