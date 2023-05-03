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

    public static String undoCommand(Node node, String holder, HolderType holderType, boolean explicitGlobalContext) {
        StringBuilder sb = new StringBuilder(32);

        sb.append(holderType.toString()).append(' ').append(holder).append(' ');

        if (node instanceof InheritanceNode) {
            // command
            sb.append("parent remove");
            if (node.hasExpiry()) {
                sb.append("temp");
            }
            sb.append(' ');
            
            // value
            sb.append(((InheritanceNode) node).getGroupName());

        } else if (node.getValue() && node instanceof ChatMetaNode<?, ?>) {
            ChatMetaNode<?, ?> chatNode = (ChatMetaNode<?, ?>) node;

            // command
            sb.append("meta remove");
            if (node.hasExpiry()) {
                sb.append("temp");
            }
            sb.append(chatNode.getMetaType().toString());
            
            // values
            sb.append(' ').append(chatNode.getPriority()).append(' ');
            appendEscaped(sb, chatNode.getMetaValue());

        } else if (node.getValue() && node instanceof MetaNode) {
            MetaNode metaNode = (MetaNode) node;
            
            // command
            sb.append("meta unset");
            if (node.hasExpiry()) {
                sb.append("temp");
            }
            sb.append(' ');

            // value
            appendEscaped(sb, metaNode.getMetaKey());

        } else {
            // command
            sb.append("permission unset");
            if (node.hasExpiry()) {
                sb.append("temp");
            }
            sb.append(' ');

            // value
            appendEscaped(sb, node.getKey());
        }

        if (!node.getContexts().isEmpty()) {
            for (Context context : node.getContexts()) {
                sb.append(' ').append(context.getKey()).append("=").append(context.getValue());
            }
        } else if (explicitGlobalContext) {
            sb.append(" global");
        }

        return sb.toString();
    }
    
    private static void appendEscaped(StringBuilder sb, String value) {
        if (value.indexOf(' ') != -1 || value.isEmpty()) {
            sb.append("\"").append(value).append("\"");
        } else {
            sb.append(value);
        }
    }
}
