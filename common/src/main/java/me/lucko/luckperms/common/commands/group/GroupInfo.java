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

package me.lucko.luckperms.common.commands.group;

import me.lucko.luckperms.common.cacheddata.type.MonitoredMetaCache;
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;

import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GroupInfo extends ChildCommand<Group> {
    public GroupInfo() {
        super(CommandSpec.GROUP_INFO, "info", CommandPermission.GROUP_INFO, Predicates.alwaysFalse());
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, Group target, ArgumentList args, String label) {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        Message.GROUP_INFO_GENERAL.send(sender, target.getName(), target.getPlainDisplayName(), target.getWeight());

        Map<Boolean, List<InheritanceNode>> parents = target.normalData().inheritanceAsSortedSet().stream()
                .filter(Node::getValue)
                .collect(Collectors.groupingBy(Node::hasExpiry, Collectors.toList()));

        List<InheritanceNode> temporaryParents = parents.getOrDefault(true, Collections.emptyList());
        List<InheritanceNode> permanentParents = parents.getOrDefault(false, Collections.emptyList());

        if (!permanentParents.isEmpty()) {
            Message.INFO_PARENT_HEADER.send(sender);
            for (InheritanceNode node : permanentParents) {
                Message.INFO_PARENT_NODE_ENTRY.send(sender, node);
            }
        }

        if (!temporaryParents.isEmpty()) {
            Message.INFO_TEMP_PARENT_HEADER.send(sender);
            for (InheritanceNode node : temporaryParents) {
                Message.INFO_PARENT_TEMPORARY_NODE_ENTRY.send(sender, node);
            }
        }

        QueryOptions queryOptions = plugin.getContextManager().getStaticQueryOptions();
        MonitoredMetaCache data = target.getCachedData().getMetaData(queryOptions);
        String prefix = data.getPrefix(CheckOrigin.INTERNAL).result();
        String suffix = data.getSuffix(CheckOrigin.INTERNAL).result();
        Map<String, List<String>> meta = data.getMeta(CheckOrigin.INTERNAL);

        Message.GROUP_INFO_CONTEXTUAL_DATA.send(sender, prefix, suffix, meta);
    }
}
