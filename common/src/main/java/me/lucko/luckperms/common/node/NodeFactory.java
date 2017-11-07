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

package me.lucko.luckperms.common.node;

import lombok.experimental.UtilityClass;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.utils.PatternCache;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Utility class to make Node(Builder) instances from serialised strings or existing Nodes
 */
@SuppressWarnings("deprecation")
@UtilityClass
public class NodeFactory {

    // used to split prefix/suffix/meta nodes
    private static final Splitter META_SPLITTER = Splitter.on(PatternCache.compileDelimitedMatcher(".", "\\")).limit(2);

    // legacy node format delimiters
    private static final Pattern LEGACY_SERVER_DELIM = PatternCache.compileDelimitedMatcher("/", "\\");
    private static final Splitter LEGACY_SERVER_SPLITTER = Splitter.on(LEGACY_SERVER_DELIM).limit(2);
    private static final Pattern LEGACY_WORLD_DELIM = PatternCache.compileDelimitedMatcher("-", "\\");
    private static final Splitter LEGACY_WORLD_SPLITTER = Splitter.on(LEGACY_WORLD_DELIM).limit(2);
    private static final Pattern LEGACY_EXPIRY_DELIM = PatternCache.compileDelimitedMatcher("$", "\\");
    private static final Splitter LEGACY_EXPIRY_SPLITTER = Splitter.on(LEGACY_EXPIRY_DELIM).limit(2);

    private static final String[] DELIMS = new String[]{".", "/", "-", "$"};

    // caches the conversion between legacy node strings --> node instances
    private static final LoadingCache<String, Node> LEGACY_SERIALIZATION_CACHE = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(s -> builderFromLegacyString(s, true).build());

    private static final LoadingCache<String, Node> LEGACY_SERIALIZATION_CACHE_NEGATED = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(s -> builderFromLegacyString(s, false).build());

    public static Node fromSerializedNode(String s, Boolean b) {
        try {
            return b ? LEGACY_SERIALIZATION_CACHE.get(s) : LEGACY_SERIALIZATION_CACHE_NEGATED.get(s);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Node.Builder newBuilder(String s) {
        return new NodeBuilder(s, false);
    }

    public static Node.Builder builderFromLegacyString(String s, Boolean b) {
        // if contains /
        if (LEGACY_SERVER_DELIM.matcher(s).find()) {
            // 0=server(+world)   1=node
            Iterator<String> parts = LEGACY_SERVER_SPLITTER.split(s).iterator();
            String parts0 = parts.next();
            String parts1 = parts.next();

            // WORLD SPECIFIC
            // if parts[0] contains -
            if (LEGACY_WORLD_DELIM.matcher(parts0).find()) {
                // 0=server   1=world
                Iterator<String> serverParts = LEGACY_WORLD_SPLITTER.split(parts0).iterator();
                String serverParts0 = serverParts.next();
                String serverParts1 = serverParts.next();

                // if parts[1] contains $
                if (LEGACY_EXPIRY_DELIM.matcher(parts1).find()) {
                    // 0=node   1=expiry
                    Iterator<String> tempParts = LEGACY_EXPIRY_SPLITTER.split(parts1).iterator();
                    String tempParts0 = tempParts.next();
                    String tempParts1 = tempParts.next();

                    return new NodeBuilder(tempParts0, true).setServer(serverParts0).setWorld(serverParts1).setExpiry(Long.parseLong(tempParts1)).setValue(b);
                } else {
                    return new NodeBuilder(parts1, true).setServer(serverParts0).setWorld(serverParts1).setValue(b);
                }
            } else {
                // SERVER BUT NOT WORLD SPECIFIC

                // if parts[1] contains $
                if (LEGACY_EXPIRY_DELIM.matcher(parts1).find()) {
                    // 0=node   1=expiry
                    Iterator<String> tempParts = LEGACY_EXPIRY_SPLITTER.split(parts1).iterator();
                    String tempParts0 = tempParts.next();
                    String tempParts1 = tempParts.next();

                    return new NodeBuilder(tempParts0, true).setServer(parts0).setExpiry(Long.parseLong(tempParts1)).setValue(b);
                } else {
                    return new NodeBuilder(parts1, true).setServer(parts0).setValue(b);
                }
            }
        } else {
            // NOT SERVER SPECIFIC

            // if s contains $
            if (LEGACY_EXPIRY_DELIM.matcher(s).find()) {
                // 0=node   1=expiry
                Iterator<String> tempParts = LEGACY_EXPIRY_SPLITTER.split(s).iterator();
                String tempParts0 = tempParts.next();
                String tempParts1 = tempParts.next();

                return new NodeBuilder(tempParts0, true).setExpiry(Long.parseLong(tempParts1)).setValue(b);
            } else {
                return new NodeBuilder(s, true).setValue(b);
            }
        }
    }

    public static Node.Builder builderFromExisting(Node other) {
        return new NodeBuilder(other);
    }

    public static Node.Builder makeMetaNode(String key, String value) {
        if (key.equalsIgnoreCase("prefix")) {
            return makePrefixNode(100, value);
        }
        if (key.equalsIgnoreCase("suffix")) {
            return makeSuffixNode(100, value);
        }

        return new NodeBuilder("meta." + escapeCharacters(key) + "." + escapeCharacters(value));
    }

    public static Node.Builder makeChatMetaNode(ChatMetaType type, int priority, String s) {
        return type == ChatMetaType.PREFIX ? makePrefixNode(priority, s) : makeSuffixNode(priority, s);
    }

    public static Node.Builder makePrefixNode(int priority, String prefix) {
        return new NodeBuilder("prefix." + priority + "." + escapeCharacters(prefix));
    }

    public static Node.Builder makeSuffixNode(int priority, String suffix) {
        return new NodeBuilder("suffix." + priority + "." + escapeCharacters(suffix));
    }

    public static String nodeAsCommand(Node node, String id, boolean group, boolean set) {
        StringBuilder sb = new StringBuilder();
        sb.append(group ? "group " : "user ").append(id).append(" ");

        if (node.isGroupNode()) {
            sb.append(node.isTemporary() ? (set ? "parent addtemp " : "parent removetemp ") : (set ? "parent add " : "parent remove "));
            sb.append(node.getGroupName());

            if (node.isTemporary() && set) {
                sb.append(" ").append(node.getExpiryUnixTime());
            }

            return appendContextToCommand(sb, node).toString();
        }

        if (node.getValuePrimitive() && (node.isPrefix() || node.isSuffix())) {
            ChatMetaType type = node.isPrefix() ? ChatMetaType.PREFIX : ChatMetaType.SUFFIX;
            String typeName = type.name().toLowerCase();

            sb.append(node.isTemporary() ? (set ? "meta addtemp" + typeName + " " : "meta removetemp" + typeName + " ") : (set ? "meta add" + typeName + " " : "meta remove" + typeName + " "));
            sb.append(type.getEntry(node).getKey()).append(" ");

            if (type.getEntry(node).getValue().contains(" ")) {
                sb.append("\"").append(type.getEntry(node).getValue()).append("\"");
            } else {
                sb.append(type.getEntry(node).getValue());
            }
            if (set && node.isTemporary()) {
                sb.append(" ").append(node.getExpiryUnixTime());
            }

            return appendContextToCommand(sb, node).toString();
        }

        if (node.getValuePrimitive() && node.isMeta()) {
            sb.append(node.isTemporary() ? (set ? "meta settemp " : "meta unsettemp ") : (set ? "meta set " : "meta unset "));

            if (node.getMeta().getKey().contains(" ")) {
                sb.append("\"").append(node.getMeta().getKey()).append("\"");
            } else {
                sb.append(node.getMeta().getKey());
            }

            if (set) {
                sb.append(" ");

                if (node.getMeta().getValue().contains(" ")) {
                    sb.append("\"").append(node.getMeta().getValue()).append("\"");
                } else {
                    sb.append(node.getMeta().getValue());
                }

                if (node.isTemporary()) {
                    sb.append(" ").append(node.getExpiryUnixTime());
                }
            }

            return appendContextToCommand(sb, node).toString();
        }

        sb.append(node.isTemporary() ? (set ? "permission settemp " : "permission unsettemp ") : (set ? "permission set " : "permission unset "));
        if (node.getPermission().contains(" ")) {
            sb.append("\"").append(node.getPermission()).append("\"");
        } else {
            sb.append(node.getPermission());
        }
        if (set) {
            sb.append(" ").append(node.getValuePrimitive());

            if (node.isTemporary()) {
                sb.append(" ").append(node.getExpiryUnixTime());
            }
        }

        return appendContextToCommand(sb, node).toString();
    }

    private static StringBuilder appendContextToCommand(StringBuilder sb, Node node) {
        if (node.isServerSpecific()) {
            sb.append(" server=").append(node.getServer().get());
        }
        if (node.isWorldSpecific()) {
            sb.append(" world=").append(node.getWorld().get());
        }

        ContextSet contexts = node.getContexts();
        for (Map.Entry<String, String> context : contexts.toSet()) {
            sb.append(" ").append(context.getKey()).append("=").append(context.getValue());
        }

        return sb;
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

        return escapeDelimiters(s, DELIMS);
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
        s = unescapeDelimiters(s, DELIMS);

        return s;
    }

    public static String escapeDelimiters(String s, String... delims) {
        if (s == null) {
            return null;
        }

        for (String delim : delims) {
            s = s.replace(delim, "\\" + delim);
        }
        return s;
    }

    public static String unescapeDelimiters(String s, String... delims) {
        if (s == null) {
            return null;
        }

        for (String delim : delims) {
            s = s.replace("\\" + delim, delim);
        }
        return s;
    }

    public static String parseGroupNode(String s) {
        String lower = s.toLowerCase();
        if (!lower.startsWith("group.")) {
            return null;
        }
        return lower.substring("group.".length()).intern();
    }

    public static Map.Entry<String, String> parseMetaNode(String s) {
        if (!s.startsWith("meta.")) {
            return null;
        }

        Iterator<String> metaParts = META_SPLITTER.split(s.substring("meta.".length())).iterator();

        if (!metaParts.hasNext()) return null;
        String key = metaParts.next();

        if (!metaParts.hasNext()) return null;
        String value = metaParts.next();

        return Maps.immutableEntry(unescapeCharacters(key).intern(), unescapeCharacters(value).intern());
    }

    private static Map.Entry<Integer, String> parseChatMetaNode(String type, String s) {
        if (!s.startsWith(type + ".")) {
            return null;
        }

        Iterator<String> metaParts = META_SPLITTER.split(s.substring((type + ".").length())).iterator();

        if (!metaParts.hasNext()) return null;
        String priority = metaParts.next();

        if (!metaParts.hasNext()) return null;
        String value = metaParts.next();

        try {
            int p = Integer.parseInt(priority);
            String v = unescapeCharacters(value).intern();
            return Maps.immutableEntry(p, v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Map.Entry<Integer, String> parsePrefixNode(String s) {
        return parseChatMetaNode("prefix", s);
    }

    public static Map.Entry<Integer, String> parseSuffixNode(String s) {
        return parseChatMetaNode("suffix", s);
    }

    public static Node make(String node) {
        return newBuilder(node).build();
    }

    public static Node make(String node, boolean value) {
        return newBuilder(node).setValue(value).build();
    }

    public static Node make(String node, boolean value, String server) {
        return newBuilder(node).setValue(value).setServer(server).build();
    }

    public static Node make(String node, boolean value, String server, String world) {
        return newBuilder(node).setValue(value).setServer(server).setWorld(world).build();
    }

    public static Node make(String node, String server) {
        return newBuilder(node).setServer(server).build();
    }

    public static Node make(String node, String server, String world) {
        return newBuilder(node).setServer(server).setWorld(world).build();
    }

    public static Node make(String node, boolean value, boolean temporary) {
        return newBuilder(node).setValue(value).setExpiry(temporary ? 10L : 0L).build();
    }

    public static Node make(String node, boolean value, String server, boolean temporary) {
        return newBuilder(node).setValue(value).setServer(server).setExpiry(temporary ? 10L : 0L).build();
    }

    public static Node make(String node, boolean value, String server, String world, boolean temporary) {
        return newBuilder(node).setValue(value).setServer(server).setWorld(world).setExpiry(temporary ? 10L : 0L).build();
    }

    public static Node make(String node, String server, boolean temporary) {
        return newBuilder(node).setServer(server).setExpiry(temporary ? 10L : 0L).build();
    }

    public static Node make(String node, String server, String world, boolean temporary) {
        return newBuilder(node).setServer(server).setWorld(world).setExpiry(temporary ? 10L : 0L).build();
    }

    public static Node make(String node, boolean value, long expireAt) {
        return newBuilder(node).setValue(value).setExpiry(expireAt).build();
    }

    public static Node make(String node, boolean value, String server, long expireAt) {
        return newBuilder(node).setValue(value).setServer(server).setExpiry(expireAt).build();
    }

    public static Node make(String node, boolean value, String server, String world, long expireAt) {
        return newBuilder(node).setValue(value).setServer(server).setWorld(world).setExpiry(expireAt).build();
    }

    public static Node make(Group group, long expireAt) {
        return NodeFactory.make("group." + group.getName(), true, expireAt);
    }

    public static Node make(Group group, String server, long expireAt) {
        return NodeFactory.make("group." + group.getName(), true, server, expireAt);
    }

    public static Node make(Group group, String server, String world, long expireAt) {
        return NodeFactory.make("group." + group.getName(), true, server, world, expireAt);
    }

    public static Node make(Group group) {
        return make("group." + group.getName());
    }

    public static Node make(Group group, boolean temporary) {
        return make("group." + group.getName(), temporary);
    }

    public static Node make(Group group, String server) {
        return make("group." + group.getName(), server);
    }

    public static Node make(Group group, String server, String world) {
        return make("group." + group.getName(), server, world);
    }

    public static Node make(Group group, String server, boolean temporary) {
        return make("group." + group.getName(), server, temporary);
    }

    public static Node make(Group group, String server, String world, boolean temporary) {
        return make("group." + group.getName(), server, world, temporary);
    }

}
