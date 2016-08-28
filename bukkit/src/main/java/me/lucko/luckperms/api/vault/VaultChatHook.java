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

package me.lucko.luckperms.api.vault;

import lombok.NonNull;
import lombok.Setter;
import me.lucko.luckperms.LPBukkitPlugin;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.core.PermissionHolder;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.users.User;
import net.milkbowl.vault.chat.Chat;

import java.util.Map;

import static me.lucko.luckperms.utils.ArgumentChecker.escapeCharacters;
import static me.lucko.luckperms.utils.ArgumentChecker.unescapeCharacters;

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
 * See {@link me.lucko.luckperms.utils.ArgumentChecker#escapeCharacters(String)}
 *
 * Registered on normal priority so other plugins can override.
 */
public class VaultChatHook extends Chat {

    @Setter
    private LPBukkitPlugin plugin;
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

    private void saveMeta(PermissionHolder holder, String world, String node, String value) {
        if (holder == null) return;
        if (node.equals("")) return;
        node = escapeCharacters(node);
        value = escapeCharacters(value);

        if (world == null || world.equals("")) {
            try {
                holder.setPermission("meta." + node + "." + value, true);
            } catch (ObjectAlreadyHasException ignored) {}

        } else {
            try {
                holder.setPermission("meta." + node + "." + value, true, "global", world);
            } catch (ObjectAlreadyHasException ignored) {}
        }
        
        perms.objectSave(holder);
    }

    private static int getMeta(PermissionHolder holder, String world, String node, int defaultValue) {
        if (holder == null) return defaultValue;
        if (node.equals("")) return defaultValue;
        node = escapeCharacters(node);

        for (Node n : holder.getPermissions()) {
            if (!n.isMeta()) {
                continue;
            }

            if (!n.shouldApplyOnWorld(world, true, false)) {
                continue;
            }

            Map.Entry<String, String> meta = n.getMeta();
            if (meta.getKey().equalsIgnoreCase(node)) {

                try {
                    return Integer.parseInt(unescapeCharacters(meta.getValue()));
                } catch (Throwable t) {
                    return defaultValue;
                }

            }
        }

        return defaultValue;
    }

    private static double getMeta(PermissionHolder holder, String world, String node, double defaultValue) {
        if (holder == null) return defaultValue;
        if (node.equals("")) return defaultValue;
        node = escapeCharacters(node);

        for (Node n : holder.getPermissions()) {
            if (!n.isMeta()) {
                continue;
            }

            if (!n.shouldApplyOnWorld(world, true, false)) {
                continue;
            }

            Map.Entry<String, String> meta = n.getMeta();
            if (meta.getKey().equalsIgnoreCase(node)) {

                try {
                    return Double.parseDouble(unescapeCharacters(meta.getValue()));
                } catch (Throwable t) {
                    return defaultValue;
                }

            }
        }

        return defaultValue;
    }

    private static boolean getMeta(PermissionHolder holder, String world, String node, boolean defaultValue) {
        if (holder == null) return defaultValue;
        if (node.equals("")) return defaultValue;
        node = escapeCharacters(node);

        for (Node n : holder.getPermissions()) {
            if (!n.isMeta()) {
                continue;
            }

            if (!n.shouldApplyOnWorld(world, true, false)) {
                continue;
            }

            Map.Entry<String, String> meta = n.getMeta();
            if (meta.getKey().equalsIgnoreCase(node)) {

                try {
                    return Boolean.parseBoolean(unescapeCharacters(meta.getValue()));
                } catch (Throwable t) {
                    return defaultValue;
                }

            }
        }

        return defaultValue;
    }

    private static String getMeta(PermissionHolder holder, String world, String node, String defaultValue) {
        if (holder == null) return defaultValue;
        if (node.equals("")) return defaultValue;
        node = escapeCharacters(node);

        for (Node n : holder.getPermissions()) {
            if (!n.getValue()) {
                continue;
            }

            if (!n.isMeta()) {
                continue;
            }

            if (!n.shouldApplyOnWorld(world, true, false)) {
                continue;
            }

            Map.Entry<String, String> meta = n.getMeta();
            if (meta.getKey().equalsIgnoreCase(node)) {

                try {
                    return unescapeCharacters(meta.getValue());
                } catch (Throwable t) {
                    return defaultValue;
                }

            }
        }

        return defaultValue;
    }

    private static String getChatMeta(boolean prefix, PermissionHolder holder, String world) {
        if (holder == null) return "";

        int priority = -1000;
        String meta = null;

        for (Node n : holder.getAllNodes(null)) {
            if (!n.getValue()) {
                continue;
            }

            if (!n.shouldApplyOnWorld(world, true, false)) {
                continue;
            }

            if (prefix) {
                if (!n.isPrefix()) {
                    continue;
                }

                Map.Entry<Integer, String> prefixValue = n.getPrefix();
                if (prefixValue.getKey() > priority) {
                    meta = prefixValue.getValue();
                    priority = prefixValue.getKey();
                }
            } else {
                if (!n.isSuffix()) {
                    continue;
                }

                Map.Entry<Integer, String> suffixValue = n.getSuffix();
                if (suffixValue.getKey() > priority) {
                    meta = suffixValue.getValue();
                    priority = suffixValue.getKey();
                }
            }
        }

        return meta == null ? "" : unescapeCharacters(meta);
    }

    public String getPlayerPrefix(String world, @NonNull String player) {
        final User user = plugin.getUserManager().get(player);
        return getChatMeta(true, user, world);
    }

    public void setPlayerPrefix(String world, @NonNull String player, @NonNull String prefix) {
        final User user = plugin.getUserManager().get(player);
        if (user == null) return;

        if (prefix.equals("")) return;

        try {
            user.setPermission("prefix.1000." + escapeCharacters(prefix), true);
        } catch (ObjectAlreadyHasException ignored) {}

        perms.objectSave(user);
    }

    public String getPlayerSuffix(String world, @NonNull String player) {
        final User user = plugin.getUserManager().get(player);
        return getChatMeta(false, user, world);
    }

    public void setPlayerSuffix(String world, @NonNull String player, @NonNull String suffix) {
        final User user = plugin.getUserManager().get(player);
        if (user == null) return;

        if (suffix.equals("")) return;

        try {
            user.setPermission("suffix.1000." + escapeCharacters(suffix), true);
        } catch (ObjectAlreadyHasException ignored) {}

        perms.objectSave(user);
    }

    public String getGroupPrefix(String world, @NonNull String group) {
        final Group g = plugin.getGroupManager().get(group);
        return getChatMeta(false, g, world);
    }

    public void setGroupPrefix(String world, @NonNull String group, @NonNull String prefix) {
        final Group g = plugin.getGroupManager().get(group);
        if (g == null) return;

        if (prefix.equals("")) return;

        try {
            g.setPermission("prefix.1000." + escapeCharacters(prefix), true);
        } catch (ObjectAlreadyHasException ignored) {}

        perms.objectSave(g);
    }

    public String getGroupSuffix(String world, @NonNull String group) {
        final Group g = plugin.getGroupManager().get(group);
        return getChatMeta(false, g, world);
    }

    public void setGroupSuffix(String world, @NonNull String group, @NonNull String suffix) {
        final Group g = plugin.getGroupManager().get(group);
        if (g == null) return;

        if (suffix.equals("")) return;

        try {
            g.setPermission("suffix.1000." + escapeCharacters(suffix), true);
        } catch (ObjectAlreadyHasException ignored) {}

        perms.objectSave(g);
    }

    public int getPlayerInfoInteger(String world, @NonNull String player, @NonNull String node, int defaultValue) {
        final User user = plugin.getUserManager().get(player);
        return getMeta(user, world, node, defaultValue);
    }

    public void setPlayerInfoInteger(String world, @NonNull String player, @NonNull String node, int value) {
        final User user = plugin.getUserManager().get(player);
        saveMeta(user, world, node, String.valueOf(value));
    }

    public int getGroupInfoInteger(String world, @NonNull String group, @NonNull String node, int defaultValue) {
        final Group g = plugin.getGroupManager().get(group);
        return getMeta(g, world, node, defaultValue);
    }

    public void setGroupInfoInteger(String world, @NonNull String group, @NonNull String node, int value) {
        final Group g = plugin.getGroupManager().get(group);
        saveMeta(g, world, node, String.valueOf(value));
    }

    public double getPlayerInfoDouble(String world, @NonNull String player, @NonNull String node, double defaultValue) {
        final User user = plugin.getUserManager().get(player);
        return getMeta(user, world, node, defaultValue);
    }

    public void setPlayerInfoDouble(String world, @NonNull String player, @NonNull String node, double value) {
        final User user = plugin.getUserManager().get(player);
        saveMeta(user, world, node, String.valueOf(value));
    }

    public double getGroupInfoDouble(String world, @NonNull String group, @NonNull String node, double defaultValue) {
        final Group g = plugin.getGroupManager().get(group);
        return getMeta(g, world, node, defaultValue);
    }

    public void setGroupInfoDouble(String world, @NonNull String group, @NonNull String node, double value) {
        final Group g = plugin.getGroupManager().get(group);
        saveMeta(g, world, node, String.valueOf(value));
    }

    public boolean getPlayerInfoBoolean(String world, @NonNull String player, @NonNull String node, boolean defaultValue) {
        final User user = plugin.getUserManager().get(player);
        return getMeta(user, world, node, defaultValue);
    }

    public void setPlayerInfoBoolean(String world, @NonNull String player, @NonNull String node, boolean value) {
        final User user = plugin.getUserManager().get(player);
        saveMeta(user, world, node, String.valueOf(value));
    }

    public boolean getGroupInfoBoolean(String world, @NonNull String group, @NonNull String node, boolean defaultValue) {
        final Group g = plugin.getGroupManager().get(group);
        return getMeta(g, world, node, defaultValue);
    }

    public void setGroupInfoBoolean(String world, @NonNull String group, @NonNull String node, boolean value) {
        final Group g = plugin.getGroupManager().get(group);
        saveMeta(g, world, node, String.valueOf(value));
    }

    public String getPlayerInfoString(String world, @NonNull String player, @NonNull String node, String defaultValue) {
        final User user = plugin.getUserManager().get(player);
        return getMeta(user, world, node, defaultValue);
    }

    public void setPlayerInfoString(String world, @NonNull String player, @NonNull String node, String value) {
        final User user = plugin.getUserManager().get(player);
        saveMeta(user, world, node, value);
    }

    public String getGroupInfoString(String world, @NonNull String group, @NonNull String node, String defaultValue) {
        final Group g = plugin.getGroupManager().get(group);
        return getMeta(g, world, node, defaultValue);
    }

    public void setGroupInfoString(String world, @NonNull String group, @NonNull String node, String value) {
        final Group g = plugin.getGroupManager().get(group);
        saveMeta(g, world, node, value);
    }

}
