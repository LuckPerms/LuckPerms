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

package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.commands.*;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.groups.Group;

import java.util.*;

public class GroupChatMeta extends SubCommand<Group> {
    public GroupChatMeta() {
        super("chatmeta", "Lists the group's chat meta", Permission.GROUP_CHATMETA, Predicate.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        SortedSet<Map.Entry<Integer, String>> prefixes = new TreeSet<>(Util.getMetaComparator().reversed());
        SortedSet<Map.Entry<Integer, String>> suffixes = new TreeSet<>(Util.getMetaComparator().reversed());

        for (Node node : group.getAllNodes(null, Contexts.allowAll())) {
            if (!node.isSuffix() && !node.isPrefix()) {
                continue;
            }

            StringBuilder sb = new StringBuilder();
            if (node.isServerSpecific()) {
                if (node.isWorldSpecific()) {
                    sb.append("&6W=").append(node.getWorld().get()).append(" ");
                }
                sb.append("&6S=").append(node.getServer().get()).append(" ");
            }

            if (node.isPrefix()) {
                sb.append(node.getPrefix().getValue());
                prefixes.add(new AbstractMap.SimpleEntry<>(node.getPrefix().getKey(), sb.toString()));
            }

            if (node.isSuffix()) {
                sb.append(node.getSuffix().getValue());
                suffixes.add(new AbstractMap.SimpleEntry<>(node.getSuffix().getKey(), sb.toString()));
            }
        }

        if (prefixes.isEmpty()) {
            Message.CHAT_META_PREFIX_NONE.send(sender, group.getDisplayName());
        } else {
            Message.CHAT_META_PREFIX_HEADER.send(sender, group.getDisplayName());
            for (Map.Entry<Integer, String> e : prefixes) {
                Message.CHAT_META_ENTRY.send(sender, e.getKey(), e.getValue());
            }
        }

        if (suffixes.isEmpty()) {
            Message.CHAT_META_SUFFIX_NONE.send(sender, group.getDisplayName());
        } else {
            Message.CHAT_META_SUFFIX_HEADER.send(sender, group.getDisplayName());
            for (Map.Entry<Integer, String> e : suffixes) {
                Message.CHAT_META_ENTRY.send(sender, e.getKey(), e.getValue());
            }
        }

        return CommandResult.SUCCESS;
    }
}
