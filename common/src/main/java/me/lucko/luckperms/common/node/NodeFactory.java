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

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.references.HolderType;
import me.lucko.luckperms.common.utils.PatternCache;

import java.util.Iterator;
import java.util.Map;

/**
 * Utility class to make Node(Builder) instances from strings or existing Nodes
 */
@UtilityClass
public class NodeFactory {

    // used to split prefix/suffix/meta nodes
    private static final Splitter META_SPLITTER = Splitter.on(PatternCache.compileDelimitedMatcher(".", "\\")).limit(2);

    public static Node.Builder newBuilder(String s) {
        return new NodeBuilder(s);
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

        return new NodeBuilder("meta." + LegacyNodeFactory.escapeCharacters(key) + "." + LegacyNodeFactory.escapeCharacters(value));
    }

    public static Node.Builder makeChatMetaNode(ChatMetaType type, int priority, String s) {
        return type == ChatMetaType.PREFIX ? makePrefixNode(priority, s) : makeSuffixNode(priority, s);
    }

    public static Node.Builder makePrefixNode(int priority, String prefix) {
        return new NodeBuilder("prefix." + priority + "." + LegacyNodeFactory.escapeCharacters(prefix));
    }

    public static Node.Builder makeSuffixNode(int priority, String suffix) {
        return new NodeBuilder("suffix." + priority + "." + LegacyNodeFactory.escapeCharacters(suffix));
    }

    public static String nodeAsCommand(Node node, String id, HolderType type, boolean set) {
        StringBuilder sb = new StringBuilder();
        sb.append(type.toString()).append(" ").append(id).append(" ");

        if (node.isGroupNode()) {
            sb.append(node.isTemporary() ? (set ? "parent addtemp " : "parent removetemp ") : (set ? "parent add " : "parent remove "));
            sb.append(node.getGroupName());

            if (node.isTemporary() && set) {
                sb.append(" ").append(node.getExpiryUnixTime());
            }

            return appendContextToCommand(sb, node).toString();
        }

        if (node.getValuePrimitive() && (node.isPrefix() || node.isSuffix())) {
            ChatMetaType nodeType = node.isPrefix() ? ChatMetaType.PREFIX : ChatMetaType.SUFFIX;
            String typeName = type.name().toLowerCase();

            sb.append(node.isTemporary() ? (set ? "meta addtemp" + typeName + " " : "meta removetemp" + typeName + " ") : (set ? "meta add" + typeName + " " : "meta remove" + typeName + " "));
            sb.append(nodeType.getEntry(node).getKey()).append(" ");

            if (nodeType.getEntry(node).getValue().contains(" ")) {
                sb.append("\"").append(nodeType.getEntry(node).getValue()).append("\"");
            } else {
                sb.append(nodeType.getEntry(node).getValue());
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

        return Maps.immutableEntry(LegacyNodeFactory.unescapeCharacters(key).intern(), LegacyNodeFactory.unescapeCharacters(value).intern());
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
            String v = LegacyNodeFactory.unescapeCharacters(value).intern();
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
