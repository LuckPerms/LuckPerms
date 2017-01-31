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

package me.lucko.luckperms.common.commands.group;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupInfo extends SubCommand<Group> {
    public GroupInfo() {
        super("info", "Gives info about the group", Permission.GROUP_INFO, Predicates.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) throws CommandException {
        Message.GROUP_INFO_GENERAL.send(sender,
                group.getId(),
                group.getDisplayName(),
                group.getWeight().isPresent() ? group.getWeight().getAsInt() : "None",
                group.getPermanentNodes().size(),
                group.getTemporaryNodes().size(),
                group.getPrefixNodes().size(),
                group.getSuffixNodes().size(),
                group.getMetaNodes().size()
        );

        Set<Node> parents = group.getPermissions(false).stream()
                .filter(Node::isGroupNode)
                .filter(Node::isPermanent)
                .collect(Collectors.toSet());

        Set<Node> tempParents = group.getPermissions(false).stream()
                .filter(Node::isGroupNode)
                .filter(Node::isTemporary)
                .collect(Collectors.toSet());

        if (!parents.isEmpty()) {
            Message.INFO_PARENT_HEADER.send(sender);
            for (Node node : parents) {
                Message.EMPTY.send(sender, "&f-    &3> &f" + node.getGroupName() + Util.getNodeContextDescription(node));
            }
        }

        if (!tempParents.isEmpty()) {
            Message.INFO_TEMP_PARENT_HEADER.send(sender);
            for (Node node : tempParents) {
                Message.EMPTY.send(sender, "&f-    &3> &f" + node.getGroupName() + Util.getNodeContextDescription(node));
                Message.EMPTY.send(sender, "&f-    &2-    expires in " + DateUtil.formatDateDiff(node.getExpiryUnixTime()));
            }
        }
        return CommandResult.SUCCESS;
    }
}
