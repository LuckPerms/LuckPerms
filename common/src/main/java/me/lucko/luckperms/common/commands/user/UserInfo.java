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

import com.google.common.collect.Maps;

import me.lucko.luckperms.common.cacheddata.type.MetaCache;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SubCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.utils.MessageUtils;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.DurationFormatter;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.verbose.event.MetaCheckEvent;

import net.luckperms.api.context.ContextSet;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserInfo extends SubCommand<User> {
    public UserInfo(LocaleManager locale) {
        super(CommandSpec.USER_INFO.localize(locale), "info", CommandPermission.USER_INFO, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), user)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        Message status = plugin.getBootstrap().isPlayerOnline(user.getUniqueId()) ? Message.PLAYER_ONLINE : Message.PLAYER_OFFLINE;

        Message.USER_INFO_GENERAL.send(sender,
                user.getUsername().orElse("Unknown"),
                user.getUniqueId(),
                user.getUniqueId().version() == 4 ? "&2mojang" : "&8offline",
                status.asString(plugin.getLocaleManager()),
                user.getPrimaryGroup().getValue()
        );

        List<InheritanceNode> parents = user.normalData().inheritanceAsSortedSet().stream()
                .filter(Node::getValue)
                .filter(n -> !n.hasExpiry())
                .collect(Collectors.toList());

        List<InheritanceNode> tempParents = user.normalData().inheritanceAsSortedSet().stream()
                .filter(Node::getValue)
                .filter(Node::hasExpiry)
                .collect(Collectors.toList());

        if (!parents.isEmpty()) {
            Message.INFO_PARENT_HEADER.send(sender);
            for (InheritanceNode node : parents) {
                Message.INFO_PARENT_ENTRY.send(sender, node.getGroupName(), MessageUtils.getAppendableNodeContextString(plugin.getLocaleManager(), node));
            }
        }

        if (!tempParents.isEmpty()) {
            Message.INFO_TEMP_PARENT_HEADER.send(sender);
            for (InheritanceNode node : tempParents) {
                Message.INFO_PARENT_ENTRY.send(sender, node.getGroupName(), MessageUtils.getAppendableNodeContextString(plugin.getLocaleManager(), node));
                Message.INFO_PARENT_ENTRY_EXPIRY.send(sender, DurationFormatter.LONG.format(node.getExpiryDuration()));
            }
        }

        String context = "&bNone";
        String prefix = "&bNone";
        String suffix = "&bNone";
        String meta = "&bNone";
        QueryOptions queryOptions = plugin.getQueryOptionsForUser(user).orElse(null);
        if (queryOptions != null) {
            ContextSet contextSet = queryOptions.context();
            if (contextSet != null && !contextSet.isEmpty()) {
                context = contextSet.toSet().stream()
                        .map(e -> MessageUtils.contextToString(plugin.getLocaleManager(), e.getKey(), e.getValue()))
                        .collect(Collectors.joining(" "));
            }

            MetaCache data = user.getCachedData().getMetaData(queryOptions);
            String prefixValue = data.getPrefix(MetaCheckEvent.Origin.INTERNAL);
            if (prefixValue != null) {
                prefix = "&f\"" + prefixValue + "&f\"";
            }
            String sussexValue = data.getSuffix(MetaCheckEvent.Origin.INTERNAL);
            if (sussexValue != null) {
                suffix = "&f\"" + sussexValue + "&f\"";
            }

            Map<String, List<String>> metaMap = data.getMeta(MetaCheckEvent.Origin.INTERNAL);
            if (!metaMap.isEmpty()) {
                meta = metaMap.entrySet().stream()
                        .flatMap(entry -> entry.getValue().stream().map(value -> Maps.immutableEntry(entry.getKey(), value)))
                        .map(e -> MessageUtils.contextToString(plugin.getLocaleManager(), e.getKey(), e.getValue()))
                        .collect(Collectors.joining(" "));
            }
        }

        Message.USER_INFO_DATA.send(sender, MessageUtils.formatBoolean(queryOptions != null), context, prefix, suffix, meta);
        return CommandResult.SUCCESS;
    }
}
