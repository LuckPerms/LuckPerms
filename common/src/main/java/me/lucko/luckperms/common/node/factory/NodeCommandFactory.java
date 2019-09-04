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

import me.lucko.luckperms.common.model.HolderType;

import net.luckperms.api.context.Context;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.ChatMetaNode;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;

public final class NodeCommandFactory {
    private NodeCommandFactory() {}

    public static String generateCommand(Node node, String id, HolderType type, boolean set, boolean explicitGlobalContext) {
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

            sb.append(cmNode.getMetaType().toString())
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

        for (Context context : node.getContexts()) {
            sb.append(" ").append(context.getKey()).append("=").append(context.getValue());
        }

        return sb;
    }
}
