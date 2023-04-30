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

package me.lucko.luckperms.common.commands.generic.parent;

import me.lucko.luckperms.common.command.abstraction.GenericChildCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.command.utils.SortMode;
import me.lucko.luckperms.common.command.utils.SortType;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.comparator.NodeWithContextComparator;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.query.QueryOptionsImpl;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Iterators;
import me.lucko.luckperms.common.util.Predicates;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class ParentInfo extends GenericChildCommand {
    public ParentInfo() {
        super(CommandSpec.PARENT_INFO, "info", CommandPermission.USER_PARENT_INFO, CommandPermission.GROUP_PARENT_INFO, Predicates.notInRange(0, 2));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        int page = args.getIntOrDefault(0, 1);
        SortMode sortMode = SortMode.determine(args);

        // get the holders nodes
        List<InheritanceNode> nodes = new LinkedList<>();
        target.normalData().copyInheritanceNodesTo(nodes, QueryOptionsImpl.DEFAULT_NON_CONTEXTUAL);

        // remove irrelevant types (these are displayed in the other info commands)
        nodes.removeIf(node -> !node.getValue());

        // handle empty
        if (nodes.isEmpty()) {
            Message.PARENT_INFO_NO_DATA.send(sender, target);
            return;
        }

        // sort the list alphabetically instead
        if (sortMode.getType() == SortType.ALPHABETICALLY) {
            nodes.sort(ALPHABETICAL_NODE_COMPARATOR);
        }

        // reverse the order if necessary
        if (!sortMode.isAscending()) {
            Collections.reverse(nodes);
        }

        int pageIndex = page - 1;
        List<List<InheritanceNode>> pages = Iterators.divideIterable(nodes, 19);

        if (pageIndex < 0 || pageIndex >= pages.size()) {
            page = 1;
            pageIndex = 0;
        }

        List<InheritanceNode> content = pages.get(pageIndex);

        // send header
        Message.PARENT_INFO.send(sender, target, page, pages.size(), nodes.size());

        // send content
        for (InheritanceNode node : content) {
            if (node.hasExpiry()) {
                Message.PARENT_INFO_TEMPORARY_NODE_ENTRY.send(sender, node, target, label);
            } else {
                Message.PARENT_INFO_NODE_ENTRY.send(sender, node, target, label);
            }
        }
    }

    private static final Comparator<InheritanceNode> ALPHABETICAL_NODE_COMPARATOR = (o1, o2) -> {
        int i = o1.getGroupName().compareTo(o2.getGroupName());
        if (i != 0) {
            return i;
        }

        // fallback to priority
        return NodeWithContextComparator.reverse().compare(o1, o2);
    };
}
