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

import lombok.NonNull;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.caching.MetaData;
import me.lucko.luckperms.common.caching.type.MetaAccumulator;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeFactory;

import net.milkbowl.vault.chat.Chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
public class VaultChatHook extends Chat {
    private final VaultPermissionHook perms;

    VaultChatHook(VaultPermissionHook perms) {
        super(perms);
        this.perms = perms;
    }

    public String getName() {
        return perms.getName();
    }

    public boolean isEnabled() {
        return perms.isEnabled();
    }
    
    private User getUser(String username) {
        Player player = Bukkit.getPlayerExact(username);
        return player == null ? null : perms.getPlugin().getUserManager().getIfLoaded(perms.getPlugin().getUuidCache().getUUID(player.getUniqueId()));
    }

    @Override
    public String getPlayerPrefix(String world, @NonNull String player) {
        final User user = getUser(player);
        return getHolderChatMeta(user, ChatMetaType.PREFIX, world);
    }

    @Override
    public void setPlayerPrefix(String world, @NonNull String player, @NonNull String prefix) {
        final User user = getUser(player);
        setChatMeta(user, ChatMetaType.PREFIX, prefix, world);
    }

    @Override
    public String getPlayerSuffix(String world, @NonNull String player) {
        final User user = getUser(player);
        return getHolderChatMeta(user, ChatMetaType.SUFFIX, world);
    }

    @Override
    public void setPlayerSuffix(String world, @NonNull String player, @NonNull String suffix) {
        final User user = getUser(player);
        setChatMeta(user, ChatMetaType.SUFFIX, suffix, world);
    }

    @Override
    public String getGroupPrefix(String world, @NonNull String group) {
        final Group g = perms.getPlugin().getGroupManager().getByDisplayName(group);
        return getHolderChatMeta(g, ChatMetaType.PREFIX, world);
    }

    @Override
    public void setGroupPrefix(String world, @NonNull String group, @NonNull String prefix) {
        final Group g = perms.getPlugin().getGroupManager().getByDisplayName(group);
        setChatMeta(g, ChatMetaType.PREFIX, prefix, world);
    }

    @Override
    public String getGroupSuffix(String world, @NonNull String group) {
        final Group g = perms.getPlugin().getGroupManager().getByDisplayName(group);
        return getHolderChatMeta(g, ChatMetaType.SUFFIX, world);
    }

    @Override
    public void setGroupSuffix(String world, @NonNull String group, @NonNull String suffix) {
        final Group g = perms.getPlugin().getGroupManager().getByDisplayName(group);
        setChatMeta(g, ChatMetaType.SUFFIX, suffix, world);
    }

    @Override
    public int getPlayerInfoInteger(String world, @NonNull String player, @NonNull String node, int defaultValue) {
        final User user = getUser(player);
        try {
            return Integer.parseInt(getHolderMeta(user, node, world, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void setPlayerInfoInteger(String world, @NonNull String player, @NonNull String node, int value) {
        final User user = getUser(player);
        setMeta(user, node, String.valueOf(value), world);
    }

    @Override
    public int getGroupInfoInteger(String world, @NonNull String group, @NonNull String node, int defaultValue) {
        final Group g = perms.getPlugin().getGroupManager().getByDisplayName(group);
        try {
            return Integer.parseInt(getHolderMeta(g, node, world, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void setGroupInfoInteger(String world, @NonNull String group, @NonNull String node, int value) {
        final Group g = perms.getPlugin().getGroupManager().getByDisplayName(group);
        setMeta(g, node, String.valueOf(value), world);
    }

    @Override
    public double getPlayerInfoDouble(String world, @NonNull String player, @NonNull String node, double defaultValue) {
        final User user = getUser(player);
        try {
            return Double.parseDouble(getHolderMeta(user, node, world, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void setPlayerInfoDouble(String world, @NonNull String player, @NonNull String node, double value) {
        final User user = getUser(player);
        setMeta(user, node, String.valueOf(value), world);
    }

    @Override
    public double getGroupInfoDouble(String world, @NonNull String group, @NonNull String node, double defaultValue) {
        final Group g = perms.getPlugin().getGroupManager().getByDisplayName(group);
        try {
            return Double.parseDouble(getHolderMeta(g, node, world, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void setGroupInfoDouble(String world, @NonNull String group, @NonNull String node, double value) {
        final Group g = perms.getPlugin().getGroupManager().getByDisplayName(group);
        setMeta(g, node, String.valueOf(value), world);
    }

    @Override
    public boolean getPlayerInfoBoolean(String world, @NonNull String player, @NonNull String node, boolean defaultValue) {
        final User user = getUser(player);
        String s = getHolderMeta(user, node, world, String.valueOf(defaultValue));
        if (!s.equalsIgnoreCase("true") && !s.equalsIgnoreCase("false")) {
            return defaultValue;
        }
        return Boolean.parseBoolean(s);
    }

    @Override
    public void setPlayerInfoBoolean(String world, @NonNull String player, @NonNull String node, boolean value) {
        final User user = getUser(player);
        setMeta(user, node, String.valueOf(value), world);
    }

    @Override
    public boolean getGroupInfoBoolean(String world, @NonNull String group, @NonNull String node, boolean defaultValue) {
        final Group g = perms.getPlugin().getGroupManager().getByDisplayName(group);
        String s = getHolderMeta(g, node, world, String.valueOf(defaultValue));
        if (!s.equalsIgnoreCase("true") && !s.equalsIgnoreCase("false")) {
            return defaultValue;
        }
        return Boolean.parseBoolean(s);
    }

    @Override
    public void setGroupInfoBoolean(String world, @NonNull String group, @NonNull String node, boolean value) {
        final Group g = perms.getPlugin().getGroupManager().getByDisplayName(group);
        setMeta(g, node, String.valueOf(value), world);
    }

    @Override
    public String getPlayerInfoString(String world, @NonNull String player, @NonNull String node, String defaultValue) {
        final User user = getUser(player);
        return getHolderMeta(user, node, world, defaultValue);
    }

    @Override
    public void setPlayerInfoString(String world, @NonNull String player, @NonNull String node, String value) {
        final User user = getUser(player);
        setMeta(user, node, value, world);
    }

    @Override
    public String getGroupInfoString(String world, @NonNull String group, @NonNull String node, String defaultValue) {
        final Group g = perms.getPlugin().getGroupManager().getByDisplayName(group);
        return getHolderMeta(g, node, world, defaultValue);
    }

    @Override
    public void setGroupInfoString(String world, @NonNull String group, @NonNull String node, String value) {
        final Group g = perms.getPlugin().getGroupManager().getByDisplayName(group);
        setMeta(g, node, value, world);
    }

    private void setMeta(PermissionHolder holder, String key, String value, String world) {
        if (holder == null || key.isEmpty()) {
            return;
        }

        String finalWorld = perms.correctWorld(world);
        perms.log("Setting meta: '" + key + "' for " + holder.getObjectName() + " on world " + world + ", server " + perms.getVaultServer());

        perms.getExecutor().execute(() -> {
            holder.removeIf(n -> n.isMeta() && n.getMeta().getKey().equals(key));

            Node.Builder metaNode = NodeFactory.makeMetaNode(key, value).setValue(true);
            metaNode.setServer(perms.getVaultServer());
            metaNode.setWorld(finalWorld);

            holder.setPermission(metaNode.build());
            perms.holderSave(holder);
        });
    }

    private void setChatMeta(PermissionHolder holder, ChatMetaType type, String value, String world) {
        if (holder == null || value.equals("")) {
            return;
        }

        String finalWorld = perms.correctWorld(world);
        perms.log("Setting " + type.name().toLowerCase() + " for " + holder.getObjectName() + " on world " + world + ", server " + perms.getVaultServer());

        perms.getExecutor().execute(() -> {
            // remove all prefixes/suffixes directly set on the user/group
            holder.removeIf(type::matches);

            // find the max inherited priority & add 10
            MetaAccumulator metaAccumulator = holder.accumulateMeta(null, null, perms.createContextForWorldSet(finalWorld));
            int priority = (type == ChatMetaType.PREFIX ? metaAccumulator.getPrefixes() : metaAccumulator.getSuffixes()).keySet().stream()
                    .mapToInt(e -> e).max().orElse(0) + 10;

            Node.Builder chatMetaNode = NodeFactory.makeChatMetaNode(type, priority, value);
            chatMetaNode.setServer(perms.getVaultServer());
            chatMetaNode.setWorld(finalWorld);

            holder.setPermission(chatMetaNode.build());
            perms.holderSave(holder);
        });
    }

    private String getHolderMeta(PermissionHolder holder, String node, String world, String defaultValue) {
        if (holder == null) {
            return defaultValue;
        }

        world = perms.correctWorld(world);

        Contexts contexts;
        if (holder.getType().isUser()) {
            contexts = perms.createContextForWorldLookup(perms.getPlugin().getPlayer((User) holder), world);
        } else {
            contexts = perms.createContextForWorldLookup(world);
        }

        perms.log("Getting meta: '" + node + "' for holder " + holder.getFriendlyName() + " in contexts " + contexts);

        String ret = holder.getCachedData().getMetaData(contexts).getMeta().get(node);
        return ret != null ? ret : defaultValue;
    }

    private String getHolderChatMeta(PermissionHolder holder, ChatMetaType type, String world) {
        if (holder == null) {
            return "";
        }

        world = perms.correctWorld(world);

        Contexts contexts;
        if (holder.getType().isUser()) {
            contexts = perms.createContextForWorldLookup(perms.getPlugin().getPlayer((User) holder), world);
        } else {
            contexts = perms.createContextForWorldLookup(world);
        }

        perms.log("Getting " + type.name().toLowerCase() + " for holder " + holder.getFriendlyName() + " in contexts " + contexts);

        MetaData data = holder.getCachedData().getMetaData(contexts);
        String ret = type == ChatMetaType.PREFIX ? data.getPrefix() : data.getSuffix();
        return ret != null ? ret : "";
    }

}
