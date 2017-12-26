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

package me.lucko.luckperms.common.commands.impl.user;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.caching.MetaData;
import me.lucko.luckperms.common.commands.ArgumentPermissions;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UserInfo extends SubCommand<User> {
    public UserInfo(LocaleManager locale) {
        super(CommandSpec.USER_INFO.spec(locale), "info", CommandPermission.USER_INFO, Predicates.alwaysFalse());
    }

    @SuppressWarnings("unchecked")
    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) throws CommandException {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), user)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        Message.USER_INFO_GENERAL.send(sender,
                user.getName().orElse("Unknown"),
                user.getUuid(),
                plugin.getPlayerStatus(user.getUuid()).asString(plugin.getLocaleManager()),
                user.getPrimaryGroup().getValue(),
                user.getOwnNodes().size(),
                user.getOwnNodes().stream().filter(n -> !(n.isGroupNode() || n.isPrefix() || n.isSuffix() || n.isMeta())).mapToInt(n -> 1).sum(),
                user.getOwnNodes().stream().filter(Node::isPrefix).mapToInt(n -> 1).sum(),
                user.getOwnNodes().stream().filter(Node::isSuffix).mapToInt(n -> 1).sum(),
                user.getOwnNodes().stream().filter(Node::isMeta).mapToInt(n -> 1).sum()
        );

        Set<Node> parents = user.getOwnNodesSet().stream()
                .filter(Node::isGroupNode)
                .filter(Node::isPermanent)
                .collect(Collectors.toSet());

        Set<Node> tempParents = user.getOwnNodesSet().stream()
                .filter(Node::isGroupNode)
                .filter(Node::isTemporary)
                .collect(Collectors.toSet());

        if (!parents.isEmpty()) {
            Message.INFO_PARENT_HEADER.send(sender);
            for (Node node : parents) {
                Message.EMPTY.send(sender, "&f-    &3> &f" + node.getGroupName() + CommandUtils.getAppendableNodeContextString(node));
            }
        }

        if (!tempParents.isEmpty()) {
            Message.INFO_TEMP_PARENT_HEADER.send(sender);
            for (Node node : tempParents) {
                Message.EMPTY.send(sender, "&f-    &3> &f" + node.getGroupName() + CommandUtils.getAppendableNodeContextString(node));
                Message.EMPTY.send(sender, "&f-    &2-    expires in " + DateUtil.formatDateDiff(node.getExpiryUnixTime()));
            }
        }

        String context = "&bNone";
        String prefix = "&bNone";
        String suffix = "&bNone";
        Contexts contexts = plugin.getContextForUser(user);
        if (contexts != null) {
            context = contexts.getContexts().toSet().stream()
                    .map(e -> CommandUtils.contextToString(e.getKey(), e.getValue()))
                    .collect(Collectors.joining(" "));

            MetaData meta = user.getCachedData().getMetaData(contexts);
            if (meta.getPrefix() != null) {
                prefix = "&f\"" + meta.getPrefix() + "&f\"";
            }
            if (meta.getSuffix() != null) {
                suffix = "&f\"" + meta.getSuffix() + "&f\"";
            }
        }

        Message.USER_INFO_DATA.send(sender, CommandUtils.formatBoolean(contexts != null), context, prefix, suffix);
        return CommandResult.SUCCESS;
    }
}
