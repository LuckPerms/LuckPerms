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

package me.lucko.luckperms.common.node.factory;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.node.model.NodeTypes;

import java.util.Map;

/**
 * Utility class to make Node(Builder) instances from strings or existing Nodes
 */
public final class NodeFactory {
    public static final String DEFAULT_GROUP_NAME = "default";

    public static Node.Builder builder(String s) {
        return new NodeBuilder(s);
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
        return NodeTypes.GROUP_NODE_MARKER + groupName;
    }

    public static String chatMetaNode(ChatMetaType type, int priority, String value) {
        return type == ChatMetaType.PREFIX ? prefixNode(priority, value) : suffixNode(priority, value);
    }

    public static String prefixNode(int priority, String prefix) {
        return NodeTypes.PREFIX_NODE_MARKER + priority + "." + LegacyNodeFactory.escapeCharacters(prefix);
    }

    public static String suffixNode(int priority, String suffix) {
        return NodeTypes.SUFFIX_NODE_MARKER + priority + "." + LegacyNodeFactory.escapeCharacters(suffix);
    }

    public static String metaNode(String key, String value) {
        return NodeTypes.META_NODE_MARKER + LegacyNodeFactory.escapeCharacters(key) + "." + LegacyNodeFactory.escapeCharacters(value);
    }

    public static String weightNode(int weight) {
        return NodeTypes.WEIGHT_NODE_MARKER + weight;
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
        StringBuilder sb = new StringBuilder(32);
        sb.append(type.toString()).append(" ").append(id).append(" ");

        if (node.isGroupNode()) {
            sb.append("parent ");

            if (set) {
                sb.append("add");
            } else {
                sb.append("remove");
            }

            if (node.isTemporary()) {
                sb.append("temp");
            }

            sb.append(" ").append(node.getGroupName());

            if (node.isTemporary() && set) {
                sb.append(" ").append(node.getExpiryUnixTime());
            }

            return appendContextToCommand(sb, node).toString();
        }

        if (node.getValue() && (node.isPrefix() || node.isSuffix())) {
            ChatMetaType chatMetaType = node.isPrefix() ? ChatMetaType.PREFIX : ChatMetaType.SUFFIX;

            sb.append("meta ");

            if (set) {
                sb.append("add");
            } else {
                sb.append("remove");
            }

            if (node.isTemporary()) {
                sb.append("temp");
            }

            sb.append(chatMetaType)
                    .append(" ")
                    .append(chatMetaType.getEntry(node).getKey()) // weight
                    .append(" ");

            String value = chatMetaType.getEntry(node).getValue();
            if (value.contains(" ")) {
                // wrap value in quotes
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }

            if (set && node.isTemporary()) {
                sb.append(" ").append(node.getExpiryUnixTime());
            }

            return appendContextToCommand(sb, node).toString();
        }

        if (node.getValue() && node.isMeta()) {
            sb.append("meta ");

            if (set) {
                sb.append("set");
            } else {
                sb.append("unset");
            }

            if (node.isTemporary()) {
                sb.append("temp");
            }

            sb.append(" ");


            String key = node.getMeta().getKey();
            if (key.contains(" ")) {
                sb.append("\"").append(key).append("\"");
            } else {
                sb.append(key);
            }

            if (set) {
                sb.append(" ");

                String value = node.getMeta().getValue();
                if (value.contains(" ")) {
                    sb.append("\"").append(value).append("\"");
                } else {
                    sb.append(value);
                }

                if (node.isTemporary()) {
                    sb.append(" ").append(node.getExpiryUnixTime());
                }
            }

            return appendContextToCommand(sb, node).toString();
        }

        sb.append("permission ");

        if (set) {
            sb.append("set");
        } else {
            sb.append("unset");
        }

        if (node.isTemporary()) {
            sb.append("temp");
        }

        sb.append(" ");

        String perm = node.getPermission();
        if (perm.contains(" ")) {
            sb.append("\"").append(perm).append("\"");
        } else {
            sb.append(perm);
        }
        if (set) {
            sb.append(" ").append(node.getValue());

            if (node.isTemporary()) {
                sb.append(" ").append(node.getExpiryUnixTime());
            }
        }

        return appendContextToCommand(sb, node).toString();
    }

    private static StringBuilder appendContextToCommand(StringBuilder sb, Node node) {
        if (node.getServer().isPresent()) {
            sb.append(" server=").append(node.getServer().get());
        }
        if (node.getWorld().isPresent()) {
            sb.append(" world=").append(node.getWorld().get());
        }

        ContextSet contexts = node.getContexts();
        for (Map.Entry<String, String> context : contexts.toSet()) {
            sb.append(" ").append(context.getKey()).append("=").append(context.getValue());
        }

        return sb;
    }

    private NodeFactory() {}

}
