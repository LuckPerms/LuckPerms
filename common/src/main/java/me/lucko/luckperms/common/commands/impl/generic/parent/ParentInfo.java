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

package me.lucko.luckperms.common.commands.impl.generic.parent;

import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.SortedSet;

public class ParentInfo extends SharedSubCommand {
    public ParentInfo() {
        super("info", "Lists the groups that this object inherits from",
                Permission.USER_PARENT_INFO, Permission.GROUP_PARENT_INFO, Predicates.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException {
        Message.LISTPARENTS.send(sender, holder.getFriendlyName(), permGroupsToString(holder.mergePermissionsToSortedSet()));
        Message.LISTPARENTS_TEMP.send(sender, holder.getFriendlyName(), tempGroupsToString(holder.mergePermissionsToSortedSet()));
        return CommandResult.SUCCESS;
    }

    private static String permGroupsToString(SortedSet<LocalizedNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (Node node : nodes) {
            if (!node.isGroupNode()) continue;
            if (node.isTemporary()) continue;

            sb.append("&3> &f")
                    .append(node.getGroupName())
                    .append(Util.getAppendableNodeContextString(node))
                    .append("\n");
        }
        return sb.length() == 0 ? "&3None" : sb.toString();
    }

    private static String tempGroupsToString(SortedSet<LocalizedNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (Node node : nodes) {
            if (!node.isGroupNode()) continue;
            if (!node.isTemporary()) continue;

            sb.append("&3> &f")
                    .append(node.getGroupName())
                    .append(Util.getAppendableNodeContextString(node))
                    .append("\n&2-    expires in ")
                    .append(DateUtil.formatDateDiff(node.getExpiryUnixTime()))
                    .append("\n");
        }
        return sb.length() == 0 ? "&3None" : sb.toString();
    }
}
