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

package me.lucko.luckperms.common.commands.generic.meta;

import com.google.common.collect.Maps;

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.GenericChildCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.comparator.NodeWithContextComparator;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.query.QueryOptionsImpl;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;

import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.ChatMetaNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class MetaInfo extends GenericChildCommand {
    public MetaInfo() {
        super(CommandSpec.META_INFO, "info", CommandPermission.USER_META_INFO, CommandPermission.GROUP_META_INFO, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        SortedSet<Map.Entry<Integer, PrefixNode>> prefixes = new TreeSet<>(MetaComparator.INSTANCE.reversed());
        SortedSet<Map.Entry<Integer, SuffixNode>> suffixes = new TreeSet<>(MetaComparator.INSTANCE.reversed());
        Set<MetaNode> meta = new LinkedHashSet<>();

        // Collect data
        for (Node node : target.resolveInheritedNodes(NodeType.META_OR_CHAT_META, QueryOptionsImpl.DEFAULT_NON_CONTEXTUAL)) {
            if (node instanceof PrefixNode) {
                PrefixNode pn = (PrefixNode) node;
                prefixes.add(Maps.immutableEntry(pn.getPriority(), pn));
            } else if (node instanceof SuffixNode) {
                SuffixNode sn = (SuffixNode) node;
                suffixes.add(Maps.immutableEntry(sn.getPriority(), sn));
            } else if (node instanceof MetaNode) {
                meta.add(((MetaNode) node));
            }
        }

        if (prefixes.isEmpty()) {
            Message.CHAT_META_PREFIX_NONE.send(sender, target);
        } else {
            Message.CHAT_META_PREFIX_HEADER.send(sender, target);
            for (Map.Entry<Integer, PrefixNode> e : prefixes) {
                Message.CHAT_META_ENTRY.send(sender, e.getValue(), target, label);
            }
        }

        if (suffixes.isEmpty()) {
            Message.CHAT_META_SUFFIX_NONE.send(sender, target);
        } else {
            Message.CHAT_META_SUFFIX_HEADER.send(sender, target);
            for (Map.Entry<Integer, SuffixNode> e : suffixes) {
                Message.CHAT_META_ENTRY.send(sender, e.getValue(), target, label);
            }
        }

        if (meta.isEmpty()) {
            Message.META_NONE.send(sender, target);
        } else {
            Message.META_HEADER.send(sender, target);
            for (MetaNode node : meta) {
                Message.META_ENTRY.send(sender, node, target, label);
            }
        }

        return CommandResult.SUCCESS;
    }

    private static final class MetaComparator implements Comparator<Map.Entry<Integer, ? extends ChatMetaNode<?, ?>>> {
        public static final MetaComparator INSTANCE = new MetaComparator();

        @Override
        public int compare(Map.Entry<Integer, ? extends ChatMetaNode<?, ?>> o1, Map.Entry<Integer, ? extends ChatMetaNode<?, ?>> o2) {
            int result = Integer.compare(o1.getKey(), o2.getKey());
            if (result != 0) {
                return result;
            }
            return NodeWithContextComparator.normal().compare(o1.getValue(), o2.getValue());
        }
    }
}
