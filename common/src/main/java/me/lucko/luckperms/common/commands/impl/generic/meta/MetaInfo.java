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

package me.lucko.luckperms.common.commands.impl.generic.meta;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.ExtractedContexts;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class MetaInfo extends SharedSubCommand {
    private static String processLocation(LocalizedNode node, PermissionHolder holder) {
        return node.getLocation().equalsIgnoreCase(holder.getObjectName()) ? "self" : node.getLocation();
    }

    public MetaInfo() {
        super("info", "Shows all chat meta", Permission.USER_META_INFO, Permission.GROUP_META_INFO, Predicates.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException {
        SortedSet<Map.Entry<Integer, LocalizedNode>> prefixes = new TreeSet<>(Util.META_COMPARATOR.reversed());
        SortedSet<Map.Entry<Integer, LocalizedNode>> suffixes = new TreeSet<>(Util.META_COMPARATOR.reversed());
        Set<LocalizedNode> meta = new HashSet<>();

        // Collect data
        for (LocalizedNode node : holder.resolveInheritancesAlmostEqual(ExtractedContexts.generate(Contexts.allowAll()))) {
            if (!node.isSuffix() && !node.isPrefix() && !node.isMeta()) {
                continue;
            }

            if (node.isPrefix()) {
                prefixes.add(new AbstractMap.SimpleEntry<>(node.getPrefix().getKey(), node));
            } else if (node.isSuffix()) {
                suffixes.add(new AbstractMap.SimpleEntry<>(node.getSuffix().getKey(), node));
            } else if (node.isMeta()) {
                meta.add(node);
            }
        }

        if (prefixes.isEmpty()) {
            Message.CHAT_META_PREFIX_NONE.send(sender, holder.getFriendlyName());
        } else {
            Message.CHAT_META_PREFIX_HEADER.send(sender, holder.getFriendlyName());
            for (Map.Entry<Integer, LocalizedNode> e : prefixes) {
                String location = processLocation(e.getValue(), holder);
                if (e.getValue().isServerSpecific() || e.getValue().isWorldSpecific() || !e.getValue().getContexts().isEmpty()) {
                    String context = Util.getNodeContextDescription(e.getValue());
                    Message.CHAT_META_ENTRY_WITH_CONTEXT.send(sender, e.getKey(), e.getValue().getPrefix().getValue(), location, context);
                } else {
                    Message.CHAT_META_ENTRY.send(sender, e.getKey(), e.getValue().getPrefix().getValue(), location);
                }
            }
        }

        if (suffixes.isEmpty()) {
            Message.CHAT_META_SUFFIX_NONE.send(sender, holder.getFriendlyName());
        } else {
            Message.CHAT_META_SUFFIX_HEADER.send(sender, holder.getFriendlyName());
            for (Map.Entry<Integer, LocalizedNode> e : suffixes) {
                String location = processLocation(e.getValue(), holder);
                if (e.getValue().isServerSpecific() || e.getValue().isWorldSpecific() || !e.getValue().getContexts().isEmpty()) {
                    String context = Util.getNodeContextDescription(e.getValue());
                    Message.CHAT_META_ENTRY_WITH_CONTEXT.send(sender, e.getKey(), e.getValue().getSuffix().getValue(), location, context);
                } else {
                    Message.CHAT_META_ENTRY.send(sender, e.getKey(), e.getValue().getSuffix().getValue(), location);
                }
            }
        }

        if (meta.isEmpty()) {
            Message.META_NONE.send(sender, holder.getFriendlyName());
        } else {
            Message.META_HEADER.send(sender, holder.getFriendlyName());
            for (LocalizedNode m : meta) {
                String location = processLocation(m, holder);
                if (m.isServerSpecific() || m.isWorldSpecific() || !m.getContexts().isEmpty()) {
                    String context = Util.getNodeContextDescription(m);
                    Message.META_ENTRY_WITH_CONTEXT.send(sender, m.getMeta().getKey(), m.getMeta().getValue(), location, context);
                } else {
                    Message.META_ENTRY.send(sender, m.getMeta().getKey(), m.getMeta().getValue(), location);
                }
            }
        }

        return CommandResult.SUCCESS;
    }
}
