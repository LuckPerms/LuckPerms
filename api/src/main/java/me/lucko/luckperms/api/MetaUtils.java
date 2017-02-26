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

package me.lucko.luckperms.api;

import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A collection of utilities to help retrieve meta values for {@link PermissionHolder}s
 *
 * @since 2.7
 */
public class MetaUtils {

    private static String escapeDelimiters(String s, String... delims) {
        for (String delim : delims) {
            s = s.replace(delim, "\\" + delim);
        }
        return s;
    }

    private static String unescapeDelimiters(String s, String... delims) {
        for (String delim : delims) {
            s = s.replace("\\" + delim, delim);
        }
        return s;
    }

    /**
     * Escapes special characters used within LuckPerms, so the string can be saved without issues
     *
     * @param s the string to escape
     * @return an escaped string
     * @throws NullPointerException if the string is null
     */
    public static String escapeCharacters(String s) {
        if (s == null) {
            throw new NullPointerException();
        }

        return escapeDelimiters(s, ".", "/", "-", "$");
    }

    /**
     * Unescapes special characters used within LuckPerms, the inverse of {@link #escapeCharacters(String)}
     *
     * @param s the string to unescape
     * @return an unescaped string
     * @throws NullPointerException if the string is null
     */
    public static String unescapeCharacters(String s) {
        if (s == null) {
            throw new NullPointerException();
        }

        s = s.replace("{SEP}", ".");
        s = s.replace("{FSEP}", "/");
        s = s.replace("{DSEP}", "$");
        s = unescapeDelimiters(s, ".", "/", "-", "$");

        return s;
    }

    /**
     * Sets a meta value on a holder
     *
     * @param holder the holder to apply the meta node to
     * @param server the server to apply the meta on, can be null
     * @param world  the world to apply the meta on, can be null
     * @param node   the meta node
     * @param value  the meta value
     * @throws NullPointerException     if the holder, node or value is null
     * @throws IllegalArgumentException if the node or value is empty
     */
    public static void setMeta(PermissionHolder holder, String server, String world, String node, String value) {
        if (holder == null) {
            throw new NullPointerException("holder");
        }

        if (node == null) {
            throw new NullPointerException("node");
        }

        if (value == null) {
            throw new NullPointerException("value");
        }

        if (node.equals("")) {
            throw new IllegalArgumentException("node is empty");
        }

        if (value.equals("")) {
            throw new IllegalArgumentException("value is empty");
        }

        if (server == null || server.equals("")) {
            server = "global";
        }

        node = escapeCharacters(node);
        value = escapeCharacters(value);

        Set<Node> toRemove = new HashSet<>();
        for (Node n : holder.getEnduringPermissions()) {
            if (n.isMeta() && n.getMeta().getKey().equals(node)) {
                toRemove.add(n);
            }
        }

        for (Node n : toRemove) {
            try {
                holder.unsetPermission(n);
            } catch (ObjectLacksException ignored) {
            }
        }

        Node.Builder metaNode = LuckPerms.getApi().buildNode("meta." + node + "." + value).setValue(true);
        if (!server.equalsIgnoreCase("global")) {
            metaNode.setServer(server);
        }
        if (world != null && !world.equals("")) {
            metaNode.setServer(server).setWorld(world);
        }

        try {
            holder.setPermission(metaNode.build());
        } catch (ObjectAlreadyHasException ignored) {}
    }

    /**
     * Gets a meta value for the holder
     *
     * @param holder        the holder to get the meta from
     * @param server        the server to retrieve the meta on, can be null
     * @param world         the world to retrieve the meta on, can be null
     * @param node          the node to get
     * @param defaultValue  the default value to return if the node is not set
     * @param includeGlobal if global nodes should be considered when retrieving the meta
     * @return a meta string, or the default value if the user does not have the meta node
     * @throws NullPointerException     if the holder or node is null
     * @throws IllegalArgumentException if the node is empty
     */
    public static String getMeta(PermissionHolder holder, String server, String world, String node, String defaultValue, boolean includeGlobal) {
        if (holder == null) {
            throw new NullPointerException("holder");
        }

        if (server == null || server.equals("")) {
            server = "global";
        }

        if (node == null) {
            throw new NullPointerException("node");
        }

        if (node.equals("")) {
            throw new IllegalArgumentException("node is empty");
        }

        node = escapeCharacters(node);

        for (Node n : holder.getPermissions()) {
            if (!n.getValue()) {
                continue;
            }

            if (!n.isMeta()) {
                continue;
            }

            if (!server.equalsIgnoreCase("global")) {
                if (!n.shouldApplyOnServer(server, includeGlobal, false)) {
                    continue;
                }
            }

            if (!n.shouldApplyOnWorld(world, includeGlobal, false)) {
                continue;
            }

            Map.Entry<String, String> meta = n.getMeta();
            if (meta.getKey().equalsIgnoreCase(node)) {
                return unescapeCharacters(meta.getValue());
            }
        }

        return defaultValue;
    }

    private static void setChatMeta(boolean prefix, PermissionHolder holder, String value, int priority, String server, String world) {
        if (holder == null) {
            throw new NullPointerException("holder");
        }

        if (value == null || value.equals("")) {
            throw new IllegalArgumentException("value is null/empty");
        }

        Node.Builder node = LuckPerms.getApi().buildNode(prefix ? "prefix" : "suffix" + "." + priority + "." + escapeCharacters(value));
        node.setValue(true);
        if (!server.equalsIgnoreCase("global")) {
            node.setServer(server);
        }
        if (world != null && !world.equals("")) {
            node.setServer(server).setWorld(world);
        }

        try {
            holder.setPermission(node.build());
        } catch (ObjectAlreadyHasException ignored) {}
    }

    /**
     * Adds a prefix to a holder on a specific server and world
     *
     * @param holder   the holder to set the prefix for
     * @param prefix   the prefix value
     * @param priority the priority to set the prefix at
     * @param server   the server to set the prefix on, can be null
     * @param world    the world to set the prefix on, can be null
     * @throws NullPointerException     if the holder is null
     * @throws IllegalArgumentException if the prefix is null or empty
     */
    public static void setPrefix(PermissionHolder holder, String prefix, int priority, String server, String world) {
        setChatMeta(true, holder, prefix, priority, server, world);
    }

    /**
     * Adds a suffix to a holder on a specific server and world
     *
     * @param holder   the holder to set the suffix for
     * @param suffix   the suffix value
     * @param priority the priority to set the suffix at
     * @param server   the server to set the suffix on, can be null
     * @param world    the world to set the suffix on, can be null
     * @throws NullPointerException     if the holder is null
     * @throws IllegalArgumentException if the suffix is null or empty
     */
    public static void setSuffix(PermissionHolder holder, String suffix, int priority, String server, String world) {
        setChatMeta(false, holder, suffix, priority, server, world);
    }

    private static String getChatMeta(boolean prefix, PermissionHolder holder, String server, String world, boolean includeGlobal) {
        if (holder == null) {
            throw new NullPointerException("holder");
        }
        if (server == null) {
            server = "global";
        }

        int priority = Integer.MIN_VALUE;
        String meta = null;
        for (Node n : holder.getAllNodes(Contexts.allowAll())) {
            if (!n.getValue()) {
                continue;
            }

            if (!server.equalsIgnoreCase("global")) {
                if (!n.shouldApplyOnServer(server, includeGlobal, false)) {
                    continue;
                }
            }

            if (!n.shouldApplyOnWorld(world, includeGlobal, false)) {
                continue;
            }

            if (prefix ? !n.isPrefix() : !n.isSuffix()) {
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

    /**
     * Returns a holders highest priority prefix, if they have one
     *
     * @param holder        the holder
     * @param server        the server to retrieve the prefix on
     * @param world         the world to retrieve the prefix on
     * @param includeGlobal if global nodes should be considered when retrieving the prefix
     * @return a prefix string, if the holder has one, or an empty string if not.
     * @throws NullPointerException if the holder is null
     */
    public static String getPrefix(PermissionHolder holder, String server, String world, boolean includeGlobal) {
        return getChatMeta(true, holder, server, world, includeGlobal);
    }

    /**
     * Returns a holders highest priority suffix, if they have one
     *
     * @param holder        the holder
     * @param server        the server to retrieve the suffix on
     * @param world         the world to retrieve the suffix on
     * @param includeGlobal if global nodes should be considered when retrieving the suffix
     * @return a suffix string, if the holder has one, or an empty string if not.
     * @throws NullPointerException if the holder is null
     */
    public static String getSuffix(PermissionHolder holder, String server, String world, boolean includeGlobal) {
        return getChatMeta(false, holder, server, world, includeGlobal);
    }

    private MetaUtils() {
    }

}
