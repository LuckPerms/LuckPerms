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

package me.lucko.luckperms.common.commands.generic.meta;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.Predicate;
import me.lucko.luckperms.common.commands.Sender;
import me.lucko.luckperms.common.commands.Util;
import me.lucko.luckperms.common.commands.generic.SecondarySubCommand;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.PermissionHolder;

import java.util.*;

public class MetaInfo extends SecondarySubCommand {
    public MetaInfo() {
        super("info", "Shows all chat meta",  Permission.USER_META_INFO, Permission.GROUP_META_INFO, Predicate.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args) {
        SortedSet<Map.Entry<Integer, Node>> prefixes = new TreeSet<>(Util.getMetaComparator().reversed());
        SortedSet<Map.Entry<Integer, Node>> suffixes = new TreeSet<>(Util.getMetaComparator().reversed());
        Set<Node> meta = new HashSet<>();

        // Collect data
        for (Node node : holder.getAllNodes(null, Contexts.allowAll())) {
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
            for (Map.Entry<Integer, Node> e : prefixes) {
                if (e.getValue().isServerSpecific() || e.getValue().isWorldSpecific()) {
                    StringBuilder sb = new StringBuilder();
                    if (e.getValue().isServerSpecific()) {
                        sb.append(" &8(&7server=&f").append(e.getValue().getServer().get()).append("&8)");
                    }
                    if (e.getValue().isWorldSpecific()) {
                        sb.append(" &8(&7world=&f").append(e.getValue().getWorld().get()).append("&8)");
                    }
                    
                    Message.CHAT_META_ENTRY_WITH_CONTEXT.send(sender, e.getKey(), e.getValue().getPrefix().getValue(), sb.toString());
                } else {
                    Message.CHAT_META_ENTRY.send(sender, e.getKey(), e.getValue().getPrefix().getValue());
                }
            }
        }

        if (suffixes.isEmpty()) {
            Message.CHAT_META_SUFFIX_NONE.send(sender, holder.getFriendlyName());
        } else {
            Message.CHAT_META_SUFFIX_HEADER.send(sender, holder.getFriendlyName());
            for (Map.Entry<Integer, Node> e : suffixes) {
                if (e.getValue().isServerSpecific() || e.getValue().isWorldSpecific()) {
                    StringBuilder sb = new StringBuilder();
                    if (e.getValue().isServerSpecific()) {
                        sb.append(" &8(&7server=&f").append(e.getValue().getServer().get()).append("&8)");
                    }
                    if (e.getValue().isWorldSpecific()) {
                        sb.append(" &8(&7world=&f").append(e.getValue().getWorld().get()).append("&8)");
                    }

                    Message.CHAT_META_ENTRY_WITH_CONTEXT.send(sender, e.getKey(), e.getValue().getSuffix().getValue(), sb.toString());
                } else {
                    Message.CHAT_META_ENTRY.send(sender, e.getKey(), e.getValue().getSuffix().getValue());
                }
            }
        }

        if (meta.isEmpty()) {
            Message.META_NONE.send(sender, holder.getFriendlyName());
        } else {
            Message.META_HEADER.send(sender, holder.getFriendlyName());
            for (Node m : meta) {
                if (m.isServerSpecific() || m.isWorldSpecific()) {
                    StringBuilder sb = new StringBuilder();
                    if (m.isServerSpecific()) {
                        sb.append(" &8(&7server=&f").append(m.getServer().get()).append("&8)");
                    }
                    if (m.isWorldSpecific()) {
                        sb.append(" &8(&7world=&f").append(m.getWorld().get()).append("&8)");
                    }

                    Message.META_ENTRY_WITH_CONTEXT.send(sender, m.getMeta().getKey(), m.getMeta().getValue(), sb.toString());
                } else {
                    Message.META_ENTRY.send(sender, m.getMeta().getKey(), m.getMeta().getValue());
                }
            }
        }

        return CommandResult.SUCCESS;
    }
}
