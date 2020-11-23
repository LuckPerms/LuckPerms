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

package me.lucko.luckperms.common.commands.user;

import me.lucko.luckperms.common.cacheddata.type.MetaCache;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.util.UniqueIdType;
import me.lucko.luckperms.common.verbose.event.MetaCheckEvent;

import net.luckperms.api.context.ContextSet;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserInfo extends ChildCommand<User> {
    public UserInfo() {
        super(CommandSpec.USER_INFO, "info", CommandPermission.USER_INFO, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User target, ArgumentList args, String label) {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        Message.USER_INFO_GENERAL.send(sender,
                target.getUsername().orElse("Unknown"),
                target.getUniqueId().toString(),
                UniqueIdType.determineType(target.getUniqueId(), plugin).describe(),
                plugin.getBootstrap().isPlayerOnline(target.getUniqueId())
        );

        List<InheritanceNode> parents = target.normalData().inheritanceAsSortedSet().stream()
                .filter(Node::getValue)
                .filter(n -> !n.hasExpiry())
                .collect(Collectors.toList());

        List<InheritanceNode> tempParents = target.normalData().inheritanceAsSortedSet().stream()
                .filter(Node::getValue)
                .filter(Node::hasExpiry)
                .collect(Collectors.toList());

        if (!parents.isEmpty()) {
            Message.INFO_PARENT_HEADER.send(sender);
            for (InheritanceNode node : parents) {
                Message.INFO_PARENT_NODE_ENTRY.send(sender, node);
            }
        }

        if (!tempParents.isEmpty()) {
            Message.INFO_TEMP_PARENT_HEADER.send(sender);
            for (InheritanceNode node : tempParents) {
                Message.INFO_PARENT_TEMPORARY_NODE_ENTRY.send(sender, node);
            }
        }

        QueryOptions queryOptions = plugin.getQueryOptionsForUser(target).orElse(null);
        boolean active = true;

        if (queryOptions == null) {
            active = false;
            queryOptions = plugin.getContextManager().getStaticQueryOptions();
        }

        ContextSet contextSet = queryOptions.context();
        MetaCache data = target.getCachedData().getMetaData(queryOptions);
        String prefix = data.getPrefix(MetaCheckEvent.Origin.INTERNAL);
        String suffix = data.getSuffix(MetaCheckEvent.Origin.INTERNAL);
        String primaryGroup = data.getPrimaryGroup(MetaCheckEvent.Origin.INTERNAL);
        Map<String, List<String>> meta = data.getMeta(MetaCheckEvent.Origin.INTERNAL);

        Message.USER_INFO_CONTEXTUAL_DATA.send(sender, active, contextSet, prefix, suffix, primaryGroup, meta);
        return CommandResult.SUCCESS;
    }
}
