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

package me.lucko.luckperms.common.commands.impl.user;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.caching.MetaData;
import me.lucko.luckperms.api.caching.UserData;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UserInfo extends SubCommand<User> {
    public UserInfo() {
        super("info", "Shows info about the user", Permission.USER_INFO, Predicates.alwaysFalse(), null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) throws CommandException {
        Message.USER_INFO_GENERAL.send(sender,
                user.getName(),
                user.getUuid(),
                plugin.getPlayerStatus(user.getUuid()),
                user.getPrimaryGroup().getValue(),
                user.getPermanentNodes().size(),
                user.getTemporaryNodes().size(),
                user.getPrefixNodes().size(),
                user.getSuffixNodes().size(),
                user.getMetaNodes().size()
        );

        Set<Node> parents = user.getPermissions(false).stream()
                .filter(Node::isGroupNode)
                .filter(Node::isPermanent)
                .collect(Collectors.toSet());

        Set<Node> tempParents = user.getPermissions(false).stream()
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

        UserData data = user.getUserData();
        String context = "&bNone";
        String prefix = "&bNone";
        String suffix = "&bNone";
        if (data != null) {
            Contexts contexts = plugin.getContextForUser(user);
            if (contexts != null) {
                context = contexts.getContexts().toSet().stream()
                        .map(e -> Util.contextToString(e.getKey(), e.getValue()))
                        .collect(Collectors.joining(" "));

                MetaData meta = data.getMetaData(contexts);
                if (meta.getPrefix() != null) {
                    prefix = "&f\"" + meta.getPrefix() + "&f\"";
                }
                if (meta.getSuffix() != null) {
                    suffix = "&f\"" + meta.getSuffix() + "&f\"";
                }
            }
        }

        Message.USER_INFO_DATA.send(sender, Util.formatBoolean(data != null), context, prefix, suffix);
        return CommandResult.SUCCESS;
    }
}
