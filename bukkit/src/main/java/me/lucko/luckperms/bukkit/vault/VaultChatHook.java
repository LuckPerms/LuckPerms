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

import lombok.NonNull;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.caching.MetaData;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.core.NodeBuilder;
import me.lucko.luckperms.common.core.PermissionHolder;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import net.milkbowl.vault.chat.Chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static me.lucko.luckperms.api.MetaUtils.escapeCharacters;
import static me.lucko.luckperms.api.MetaUtils.unescapeCharacters;

/**
 * Provides the Vault Chat service through the use of normal permission nodes.
 *
 * Prefixes / Suffixes:
 * Normal inheritance rules apply.
 * Permission Nodes = prefix.priority.value OR suffix.priority.value
 * If a user/group has / inherits multiple prefixes and suffixes, the one with the highest priority is the one that
 * will apply.
 *
 * Meta:
 * Normal inheritance rules DO NOT apply.
 * Permission Nodes = meta.node.value
 *
 * Node that special characters used within LuckPerms are escaped:
 * See {@link me.lucko.luckperms.api.MetaUtils#unescapeCharacters(String)}
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

    private void setMeta(PermissionHolder holder, String world, String node, String value) {
        String finalWorld = perms.isIgnoreWorld() ? null : world;
        if (holder == null) return;
        if (node.equals("")) return;

        perms.log("Setting meta: '" + node + "' for " + holder.getObjectName() + " on world " + world + ", server " + perms.getServer());

        perms.getScheduler().scheduleTask(() -> {
            String k = escapeCharacters(node);
            String v = escapeCharacters(value);

            List<Node> toRemove = holder.getNodes().stream()
                    .filter(n -> n.isMeta() && n.getMeta().getKey().equals(k))
                    .collect(Collectors.toList());

            toRemove.forEach(n -> {
                try {
                    holder.unsetPermission(n);
                } catch (ObjectLacksException ignored) {}
            });

            Node.Builder metaNode = new NodeBuilder("meta." + k + "." + v).setValue(true);
            if (!perms.getServer().equalsIgnoreCase("global")) {
                metaNode.setServer(perms.getServer());
            }
            if (finalWorld != null && !finalWorld.equals("")) {
                metaNode.setServer(perms.getServer()).setWorld(finalWorld);
            }

            try {
                holder.setPermission(metaNode.build());
            } catch (ObjectAlreadyHasException ignored) {}

            perms.save(holder);
        });
    }

    private void setChatMeta(boolean prefix, PermissionHolder holder, String value, String world) {
        String finalWorld = perms.isIgnoreWorld() ? null : world;
        if (holder == null) return;
        if (value.equals("")) return;

        perms.log("Setting " + (prefix ? "prefix" : "suffix") + " for " + holder.getObjectName() + " on world " + world + ", server " + perms.getServer());

        perms.getScheduler().scheduleTask(() -> {
            Node.Builder node = new NodeBuilder(prefix ? "prefix" : "suffix" + ".1000." + escapeCharacters(value));
            node.setValue(true);
            if (!perms.getServer().equalsIgnoreCase("global")) {
                node.setServer(perms.getServer());
            }

            if (finalWorld != null && !finalWorld.equals("")) {
                node.setServer(perms.getServer()).setWorld(finalWorld);
            }

            try {
                holder.setPermission(node.build());
            } catch (ObjectAlreadyHasException ignored) {}

            perms.save(holder);
        });
    }

    private String getUserMeta(User user, String world, String node, String defaultValue) {
        if (user == null) return defaultValue;
        world = perms.isIgnoreWorld() ? null : world;
        node = escapeCharacters(node);

        perms.log("Getting meta: '" + node + "' for user " + user.getName() + " on world " + world + ", server " + perms.getServer());

        if (user.getUserData() == null) {
            return defaultValue;
        }

        return unescapeCharacters(user.getUserData().getMetaData(perms.createContext(perms.getServer(), world)).getMeta().getOrDefault(node, defaultValue));
    }

    private String getUserChatMeta(boolean prefix, User user, String world) {
        if (user == null) return "";
        world = perms.isIgnoreWorld() ? null : world;

        perms.log("Getting " + (prefix ? "prefix" : "suffix") + " for user " + user.getName() + " on world " + world + ", server " + perms.getServer());

        if (user.getUserData() == null) {
            return "";
        }

        MetaData data = user.getUserData().getMetaData(perms.createContext(perms.getServer(), world));
        String v = prefix ? data.getPrefix() : data.getSuffix();
        return v == null ? "" : unescapeCharacters(v);
    }

    private String getGroupMeta(Group group, String world, String node, String defaultValue) {
        world = perms.isIgnoreWorld() ? null : world;
        if (group == null) return defaultValue;
        if (node.equals("")) return defaultValue;
        node = escapeCharacters(node);

        perms.log("Getting meta: '" + node + "' for group " + group.getName() + " on world " + world + ", server " + perms.getServer());

        for (Node n : group.getPermissions(true)) {
            if (!n.getValue()) {
                continue;
            }

            if (!n.isMeta()) {
                continue;
            }

            if (!n.shouldApplyOnServer(perms.getServer(), perms.isIncludeGlobal(), false)) {
                continue;
            }

            if (!n.shouldApplyOnWorld(world, perms.isIncludeGlobal(), false)) {
                continue;
            }

            Map.Entry<String, String> meta = n.getMeta();
            if (meta.getKey().equalsIgnoreCase(node)) {
                return unescapeCharacters(meta.getValue());
            }
        }

        return defaultValue;
    }

    private String getGroupChatMeta(boolean prefix, Group group, String world) {
        world = perms.isIgnoreWorld() ? null : world;
        if (group == null) return "";

        perms.log("Getting " + (prefix ? "prefix" : "suffix") + " for group " + group + " on world " + world + ", server " + perms.getServer());

        int priority = Integer.MIN_VALUE;
        String meta = null;

        Map<String, String> context = new HashMap<>();
        context.put("server", perms.getServer());
        if (world != null) {
            context.put("world", world);
        }

        for (Node n : group.getAllNodes(null, new Contexts(ContextSet.fromMap(context), perms.isIncludeGlobal(), true, true, true, true, false))) {
            if (!n.getValue()) {
                continue;
            }

            if (prefix ? !n.isPrefix() : !n.isSuffix()) {
                continue;
            }

            if (!n.shouldApplyOnServer(perms.getServer(), perms.isIncludeGlobal(), false)) {
                continue;
            }

            if (!n.shouldApplyOnWorld(world, perms.isIncludeGlobal(), false)) {
                continue;
            }

            Map.Entry<Integer, String> value = prefix ? n.getPrefix() : n.getSuffix();
            if (value.getKey() > priority) {
                meta = value.getValue();
                priority = value.getKey();
            }
        }

        return meta == null ? "" : unescapeCharacters(meta);
    }

    public String getPlayerPrefix(String world, @NonNull String player) {
        final User user = perms.getPlugin().getUserManager().get(player);
        return getUserChatMeta(true, user, world);
    }

    public void setPlayerPrefix(String world, @NonNull String player, @NonNull String prefix) {
        final User user = perms.getPlugin().getUserManager().get(player);
        setChatMeta(true, user, prefix, world);
    }

    public String getPlayerSuffix(String world, @NonNull String player) {
        final User user = perms.getPlugin().getUserManager().get(player);
        return getUserChatMeta(false, user, world);
    }

    public void setPlayerSuffix(String world, @NonNull String player, @NonNull String suffix) {
        final User user = perms.getPlugin().getUserManager().get(player);
        setChatMeta(false, user, suffix, world);
    }

    public String getGroupPrefix(String world, @NonNull String group) {
        final Group g = perms.getPlugin().getGroupManager().get(group);
        return getGroupChatMeta(false, g, world);
    }

    public void setGroupPrefix(String world, @NonNull String group, @NonNull String prefix) {
        final Group g = perms.getPlugin().getGroupManager().get(group);
        setChatMeta(true, g, prefix, world);
    }

    public String getGroupSuffix(String world, @NonNull String group) {
        final Group g = perms.getPlugin().getGroupManager().get(group);
        return getGroupChatMeta(false, g, world);
    }

    public void setGroupSuffix(String world, @NonNull String group, @NonNull String suffix) {
        final Group g = perms.getPlugin().getGroupManager().get(group);
        setChatMeta(false, g, suffix, world);
    }

    public int getPlayerInfoInteger(String world, @NonNull String player, @NonNull String node, int defaultValue) {
        final User user = perms.getPlugin().getUserManager().get(player);
        try {
            return Integer.parseInt(getUserMeta(user, world, node, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void setPlayerInfoInteger(String world, @NonNull String player, @NonNull String node, int value) {
        final User user = perms.getPlugin().getUserManager().get(player);
        setMeta(user, world, node, String.valueOf(value));
    }

    public int getGroupInfoInteger(String world, @NonNull String group, @NonNull String node, int defaultValue) {
        final Group g = perms.getPlugin().getGroupManager().get(group);
        try {
            return Integer.parseInt(getGroupMeta(g, world, node, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void setGroupInfoInteger(String world, @NonNull String group, @NonNull String node, int value) {
        final Group g = perms.getPlugin().getGroupManager().get(group);
        setMeta(g, world, node, String.valueOf(value));
    }

    public double getPlayerInfoDouble(String world, @NonNull String player, @NonNull String node, double defaultValue) {
        final User user = perms.getPlugin().getUserManager().get(player);
        try {
            return Double.parseDouble(getUserMeta(user, world, node, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void setPlayerInfoDouble(String world, @NonNull String player, @NonNull String node, double value) {
        final User user = perms.getPlugin().getUserManager().get(player);
        setMeta(user, world, node, String.valueOf(value));
    }

    public double getGroupInfoDouble(String world, @NonNull String group, @NonNull String node, double defaultValue) {
        final Group g = perms.getPlugin().getGroupManager().get(group);
        try {
            return Double.parseDouble(getGroupMeta(g, world, node, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void setGroupInfoDouble(String world, @NonNull String group, @NonNull String node, double value) {
        final Group g = perms.getPlugin().getGroupManager().get(group);
        setMeta(g, world, node, String.valueOf(value));
    }

    public boolean getPlayerInfoBoolean(String world, @NonNull String player, @NonNull String node, boolean defaultValue) {
        final User user = perms.getPlugin().getUserManager().get(player);
        String s = getUserMeta(user, world, node, String.valueOf(defaultValue));
        if (!s.equalsIgnoreCase("true") && !s.equalsIgnoreCase("false")) {
            return defaultValue;
        }
        return Boolean.parseBoolean(s);
    }

    public void setPlayerInfoBoolean(String world, @NonNull String player, @NonNull String node, boolean value) {
        final User user = perms.getPlugin().getUserManager().get(player);
        setMeta(user, world, node, String.valueOf(value));
    }

    public boolean getGroupInfoBoolean(String world, @NonNull String group, @NonNull String node, boolean defaultValue) {
        final Group g = perms.getPlugin().getGroupManager().get(group);
        String s = getGroupMeta(g, world, node, String.valueOf(defaultValue));
        if (!s.equalsIgnoreCase("true") && !s.equalsIgnoreCase("false")) {
            return defaultValue;
        }
        return Boolean.parseBoolean(s);
    }

    public void setGroupInfoBoolean(String world, @NonNull String group, @NonNull String node, boolean value) {
        final Group g = perms.getPlugin().getGroupManager().get(group);
        setMeta(g, world, node, String.valueOf(value));
    }

    public String getPlayerInfoString(String world, @NonNull String player, @NonNull String node, String defaultValue) {
        final User user = perms.getPlugin().getUserManager().get(player);
        return getUserMeta(user, world, node, defaultValue);
    }

    public void setPlayerInfoString(String world, @NonNull String player, @NonNull String node, String value) {
        final User user = perms.getPlugin().getUserManager().get(player);
        setMeta(user, world, node, value);
    }

    public String getGroupInfoString(String world, @NonNull String group, @NonNull String node, String defaultValue) {
        final Group g = perms.getPlugin().getGroupManager().get(group);
        return getGroupMeta(g, world, node, defaultValue);
    }

    public void setGroupInfoString(String world, @NonNull String group, @NonNull String node, String value) {
        final Group g = perms.getPlugin().getGroupManager().get(group);
        setMeta(g, world, node, value);
    }

}
