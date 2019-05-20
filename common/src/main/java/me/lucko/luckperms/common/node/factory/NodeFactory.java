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

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.DefaultContextKeys;
import me.lucko.luckperms.api.node.ChatMetaType;
import me.lucko.luckperms.api.node.Node;
import me.lucko.luckperms.api.node.NodeBuilder;
import me.lucko.luckperms.api.node.types.ChatMetaNode;
import me.lucko.luckperms.api.node.types.InheritanceNode;
import me.lucko.luckperms.api.node.types.MetaNode;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;

import java.util.Map;

/**
 * Utility class to make Node(Builder) instances from strings or existing Nodes
 */
public final class NodeFactory {
    private NodeFactory() {}

    public static final String DEFAULT_GROUP_NAME = "default";

    public static NodeBuilder<?, ?> builder(String s) {
        return NodeTypes.newBuilder(s);
    }

    public static NodeBuilder<?, ?> buildGroupNode(String groupName) {
        return builder(groupNode(groupName));
    }

    public static NodeBuilder<?, ?> buildGroupNode(Group group) {
        return builder(groupNode(group.getName()));
    }

    public static NodeBuilder<?, ?> buildMetaNode(String key, String value) {
        return builder(metaNode(key, value));
    }

    public static NodeBuilder<?, ?> buildChatMetaNode(ChatMetaType type, int priority, String s) {
        return type == ChatMetaType.PREFIX ? buildPrefixNode(priority, s) : buildSuffixNode(priority, s);
    }

    public static NodeBuilder<?, ?> buildPrefixNode(int priority, String prefix) {
        return builder(prefixNode(priority, prefix));
    }

    public static NodeBuilder<?, ?> buildSuffixNode(int priority, String suffix) {
        return builder(suffixNode(priority, suffix));
    }

    public static NodeBuilder<?, ?> buildWeightNode(int weight) {
        return builder(weightNode(weight));
    }

    public static String groupNode(String groupName) {
        return NodeTypes.GROUP_NODE_MARKER + groupName;
    }

    public static String displayName(String displayName) {
        return NodeTypes.DISPLAY_NAME_NODE_MARKER + displayName;
    }

    public static String chatMetaNode(ChatMetaType type, int priority, String value) {
        return type == ChatMetaType.PREFIX ? prefixNode(priority, value) : suffixNode(priority, value);
    }

    public static String prefixNode(int priority, String prefix) {
        return NodeTypes.PREFIX_NODE_MARKER + priority + "." + Delimiters.escapeCharacters(prefix);
    }

    public static String suffixNode(int priority, String suffix) {
        return NodeTypes.SUFFIX_NODE_MARKER + priority + "." + Delimiters.escapeCharacters(suffix);
    }

    public static String metaNode(String key, String value) {
        return NodeTypes.META_NODE_MARKER + Delimiters.escapeCharacters(key) + "." + Delimiters.escapeCharacters(value);
    }

    public static String weightNode(int weight) {
        return NodeTypes.WEIGHT_NODE_MARKER + weight;
    }

    public static String regexNode(String pattern) {
        return NodeTypes.REGEX_MARKER_1 + pattern;
    }

    public static Node make(String node) {
        return builder(node).build();
    }

    public static Node make(String node, boolean value) {
        return builder(node).value(value).build();
    }

    public static Node make(String node, boolean value, String server) {
        return builder(node).value(value).withContext(DefaultContextKeys.SERVER_KEY, server).build();
    }

    public static Node make(String node, boolean value, String server, String world) {
        return builder(node).value(value).withContext(DefaultContextKeys.SERVER_KEY, server).withContext(DefaultContextKeys.WORLD_KEY, world).build();
    }

    public static Node make(String node, String server) {
        return builder(node).withContext(DefaultContextKeys.SERVER_KEY, server).build();
    }

    public static Node make(String node, String server, String world) {
        return builder(node).withContext(DefaultContextKeys.SERVER_KEY, server).withContext(DefaultContextKeys.WORLD_KEY, world).build();
    }

    public static Node make(String node, boolean value, boolean temporary) {
        return builder(node).value(value).expiry(temporary ? 10L : 0L).build();
    }

    public static Node make(String node, boolean value, String server, boolean temporary) {
        return builder(node).value(value).withContext(DefaultContextKeys.SERVER_KEY, server).expiry(temporary ? 10L : 0L).build();
    }

    public static Node make(String node, boolean value, String server, String world, boolean temporary) {
        return builder(node).value(value).withContext(DefaultContextKeys.SERVER_KEY, server).withContext(DefaultContextKeys.WORLD_KEY, world).expiry(temporary ? 10L : 0L).build();
    }

    public static Node make(String node, String server, boolean temporary) {
        return builder(node).withContext(DefaultContextKeys.SERVER_KEY, server).expiry(temporary ? 10L : 0L).build();
    }

    public static Node make(String node, String server, String world, boolean temporary) {
        return builder(node).withContext(DefaultContextKeys.SERVER_KEY, server).withContext(DefaultContextKeys.WORLD_KEY, world).expiry(temporary ? 10L : 0L).build();
    }

    public static Node make(String node, boolean value, long expireAt) {
        return builder(node).value(value).expiry(expireAt).build();
    }

    public static Node make(String node, boolean value, String server, long expireAt) {
        return builder(node).value(value).withContext(DefaultContextKeys.SERVER_KEY, server).expiry(expireAt).build();
    }

    public static Node make(String node, boolean value, String server, String world, long expireAt) {
        return builder(node).value(value).withContext(DefaultContextKeys.SERVER_KEY, server).withContext(DefaultContextKeys.WORLD_KEY, world).expiry(expireAt).build();
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

    public static String nodeAsCommand(Node node, String id, HolderType type, boolean set, boolean explicitGlobalContext) {
        StringBuilder sb = new StringBuilder(32);
        sb.append(type.toString()).append(" ").append(id).append(" ");

        if (node instanceof InheritanceNode) {
            sb.append("parent ");

            if (set) {
                sb.append("add");
            } else {
                sb.append("remove");
            }

            if (node.hasExpiry()) {
                sb.append("temp");
            }

            sb.append(" ").append(((InheritanceNode) node).getGroupName());

            if (node.hasExpiry() && set) {
                sb.append(" ").append(node.getExpiry().getEpochSecond());
            }

            return appendContextToCommand(sb, node, explicitGlobalContext).toString();
        }

        if (node.getValue() && (node instanceof ChatMetaNode<?, ?>)) {
            ChatMetaNode<?, ?> cmNode = (ChatMetaNode<?, ?>) node;

            sb.append("meta ");

            if (set) {
                sb.append("add");
            } else {
                sb.append("remove");
            }

            if (node.hasExpiry()) {
                sb.append("temp");
            }

            sb.append(cmNode.getType().toString())
                    .append(" ")
                    .append(cmNode.getPriority()) // weight
                    .append(" ");

            String value = cmNode.getMetaValue();
            if (value.contains(" ")) {
                // wrap value in quotes
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }

            if (set && node.hasExpiry()) {
                sb.append(" ").append(node.getExpiry().getEpochSecond());
            }

            return appendContextToCommand(sb, node, explicitGlobalContext).toString();
        }

        if (node.getValue() && node instanceof MetaNode) {
            sb.append("meta ");

            if (set) {
                sb.append("set");
            } else {
                sb.append("unset");
            }

            if (node.hasExpiry()) {
                sb.append("temp");
            }

            sb.append(" ");


            MetaNode metaNode = (MetaNode) node;
            String key = metaNode.getMetaKey();
            if (key.contains(" ")) {
                sb.append("\"").append(key).append("\"");
            } else {
                sb.append(key);
            }

            if (set) {
                sb.append(" ");

                String value = metaNode.getMetaValue();
                if (value.contains(" ")) {
                    sb.append("\"").append(value).append("\"");
                } else {
                    sb.append(value);
                }

                if (node.hasExpiry()) {
                    sb.append(" ").append(node.getExpiry().getEpochSecond());
                }
            }

            return appendContextToCommand(sb, node, explicitGlobalContext).toString();
        }

        sb.append("permission ");

        if (set) {
            sb.append("set");
        } else {
            sb.append("unset");
        }

        if (node.hasExpiry()) {
            sb.append("temp");
        }

        sb.append(" ");

        String perm = node.getKey();
        if (perm.contains(" ")) {
            sb.append("\"").append(perm).append("\"");
        } else {
            sb.append(perm);
        }
        if (set) {
            sb.append(" ").append(node.getValue());

            if (node.hasExpiry()) {
                sb.append(" ").append(node.getExpiry().getEpochSecond());
            }
        }

        return appendContextToCommand(sb, node, explicitGlobalContext).toString();
    }

    private static StringBuilder appendContextToCommand(StringBuilder sb, Node node, boolean explicitGlobalContext) {
        if (node.getContexts().isEmpty()) {
            if (explicitGlobalContext) {
                sb.append(" global");
            }
            return sb;
        }

        ContextSet contexts = node.getContexts();
        for (Map.Entry<String, String> context : contexts) {
            sb.append(" ").append(context.getKey()).append("=").append(context.getValue());
        }

        return sb;
    }

}
