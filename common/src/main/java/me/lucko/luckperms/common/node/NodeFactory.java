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
    public static final String DEFAULT_GROUP_NAME = "default";

    public static final String PREFIX_KEY = "prefix";
    public static final String SUFFIX_KEY = "suffix";
    public static final String META_KEY = "meta";
    public static final String WEIGHT_KEY = "weight";

    private static final String GROUP_NODE_MARKER = "group.";
    private static final String PREFIX_NODE_MARKER = PREFIX_KEY + ".";
    private static final String SUFFIX_NODE_MARKER = SUFFIX_KEY + ".";
    private static final String META_NODE_MARKER = META_KEY + ".";
    private static final String WEIGHT_NODE_MARKER = WEIGHT_KEY + ".";

    // used to split prefix/suffix/meta nodes
    private static final Splitter META_SPLITTER = Splitter.on(PatternCache.compileDelimitedMatcher(".", "\\")).limit(2);

    public static Node.Builder builder(String s) {
        return new NodeBuilder(s);
    }

    public static Node.Builder builder(Node other) {
        return new NodeBuilder(other);
    }

    public static Node.Builder buildGroupNode(String groupName) {
        return new NodeBuilder(groupNode(groupName));
    }

    public static Node.Builder buildGroupNode(Group group) {
        return new NodeBuilder(groupNode(group.getName()));
    }

    public static Node.Builder buildMetaNode(String key, String value) {
        return new NodeBuilder(metaNode(key, value));
    }

    public static Node.Builder buildChatMetaNode(ChatMetaType type, int priority, String s) {
        return type == ChatMetaType.PREFIX ? buildPrefixNode(priority, s) : buildSuffixNode(priority, s);
    }

    public static Node.Builder buildPrefixNode(int priority, String prefix) {
        return new NodeBuilder(prefixNode(priority, prefix));
    }

    public static Node.Builder buildSuffixNode(int priority, String suffix) {
        return new NodeBuilder(suffixNode(priority, suffix));
    }

    public static Node.Builder buildWeightNode(int weight) {
        return new NodeBuilder(weightNode(weight));
    }

    public static String groupNode(String groupName) {
        return GROUP_NODE_MARKER + groupName;
    }

    public static String prefixNode(int priority, String prefix) {
        return PREFIX_NODE_MARKER + priority + "." + LegacyNodeFactory.escapeCharacters(prefix);
    }

    public static String suffixNode(int priority, String suffix) {
        return SUFFIX_NODE_MARKER + priority + "." + LegacyNodeFactory.escapeCharacters(suffix);
    }

    public static String metaNode(String key, String value) {
        return META_NODE_MARKER + LegacyNodeFactory.escapeCharacters(key) + "." + LegacyNodeFactory.escapeCharacters(value);
    }

    public static String weightNode(int weight) {
        return WEIGHT_NODE_MARKER + weight;
    }

    public static Node make(String node) {
        return builder(node).build();
    }

    public static Node make(String node, boolean value) {
        return builder(node).setValue(value).build();
    }

    public static Node make(String node, boolean value, String server) {
        return builder(node).setValue(value).setServer(server).build();
    }

    public static Node make(String node, boolean value, String server, String world) {
        return builder(node).setValue(value).setServer(server).setWorld(world).build();
    }

    public static Node make(String node, String server) {
        return builder(node).setServer(server).build();
    }

    public static Node make(String node, String server, String world) {
        return builder(node).setServer(server).setWorld(world).build();
    }

    public static Node make(String node, boolean value, boolean temporary) {
        return builder(node).setValue(value).setExpiry(temporary ? 10L : 0L).build();
    }

    public static Node make(String node, boolean value, String server, boolean temporary) {
        return builder(node).setValue(value).setServer(server).setExpiry(temporary ? 10L : 0L).build();
    }

    public static Node make(String node, boolean value, String server, String world, boolean temporary) {
        return builder(node).setValue(value).setServer(server).setWorld(world).setExpiry(temporary ? 10L : 0L).build();
    }

    public static Node make(String node, String server, boolean temporary) {
        return builder(node).setServer(server).setExpiry(temporary ? 10L : 0L).build();
    }

    public static Node make(String node, String server, String world, boolean temporary) {
        return builder(node).setServer(server).setWorld(world).setExpiry(temporary ? 10L : 0L).build();
    }

    public static Node make(String node, boolean value, long expireAt) {
        return builder(node).setValue(value).setExpiry(expireAt).build();
    }

    public static Node make(String node, boolean value, String server, long expireAt) {
        return builder(node).setValue(value).setServer(server).setExpiry(expireAt).build();
    }

    public static Node make(String node, boolean value, String server, String world, long expireAt) {
        return builder(node).setValue(value).setServer(server).setWorld(world).setExpiry(expireAt).build();
    }

    public static Node make(Group group, long expireAt) {
        return NodeFactory.make(groupNode(group.getName()), true, expireAt);
    }

    public static Node make(Group group, String server, long expireAt) {
        return NodeFactory.make(groupNode(group.getName()), true, server, expireAt);
    }

    public static Node make(Group group, String server, String world, long expireAt) {
        return NodeFactory.make(groupNode(group.getName()), true, server, world, expireAt);
    }

    public static Node make(Group group) {
        return make(groupNode(group.getName()));
    }

    public static Node make(Group group, boolean temporary) {
        return make(groupNode(group.getName()), temporary);
    }

    public static Node make(Group group, String server) {
        return make(groupNode(group.getName()), server);
    }

    public static Node make(Group group, String server, String world) {
        return make(groupNode(group.getName()), server, world);
    }

    public static Node make(Group group, String server, boolean temporary) {
        return make(groupNode(group.getName()), server, temporary);
    }

    public static Node make(Group group, String server, String world, boolean temporary) {
        return make(groupNode(group.getName()), server, world, temporary);
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
        if (!lower.startsWith(GROUP_NODE_MARKER)) {
            return null;
        }
        return lower.substring(GROUP_NODE_MARKER.length()).intern();
    }

    public static Map.Entry<String, String> parseMetaNode(String s) {
        if (!s.startsWith(META_NODE_MARKER)) {
            return null;
        }

        Iterator<String> metaParts = META_SPLITTER.split(s.substring(META_NODE_MARKER.length())).iterator();

        if (!metaParts.hasNext()) return null;
        String key = metaParts.next();

        if (!metaParts.hasNext()) return null;
        String value = metaParts.next();

        return Maps.immutableEntry(LegacyNodeFactory.unescapeCharacters(key).intern(), LegacyNodeFactory.unescapeCharacters(value).intern());
    }

    private static Map.Entry<Integer, String> parseChatMetaNode(String marker, String s) {
        if (!s.startsWith(marker)) {
            return null;
        }

        Iterator<String> metaParts = META_SPLITTER.split(s.substring(marker.length())).iterator();

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
        return parseChatMetaNode(PREFIX_NODE_MARKER, s);
    }

    public static Map.Entry<Integer, String> parseSuffixNode(String s) {
        return parseChatMetaNode(SUFFIX_NODE_MARKER, s);
    }

    public static Integer parseWeightNode(String s) {
        String lower = s.toLowerCase();
        if (!lower.startsWith(WEIGHT_NODE_MARKER)) {
            return null;
        }
        String i = lower.substring(WEIGHT_NODE_MARKER.length());
        try {
            return Integer.parseInt(i);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
