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

import com.google.common.collect.Maps;

import me.lucko.luckperms.common.cache.LoadingMap;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.node.comparator.NodeEntryComparator;
import me.lucko.luckperms.common.node.matcher.ConstraintNodeMatcher;
import me.lucko.luckperms.common.node.matcher.StandardNodeMatchers;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.storage.misc.NodeEntry;
import me.lucko.luckperms.common.util.Iterators;
import me.lucko.luckperms.common.util.Predicates;

import net.luckperms.api.node.types.InheritanceNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GroupListMembers extends ChildCommand<Group> {
    public GroupListMembers() {
        super(CommandSpec.GROUP_LISTMEMBERS, "listmembers", CommandPermission.GROUP_LIST_MEMBERS, Predicates.notInRange(0, 1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Group target, ArgumentList args, String label) {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        InheritanceNode node = Inheritance.builder(target.getName()).build();
        ConstraintNodeMatcher<InheritanceNode> matcher = StandardNodeMatchers.key(node);
        int page = args.getIntOrDefault(0, 1);

        Message.SEARCH_SEARCHING_MEMBERS.send(sender, target.getName());

        List<NodeEntry<UUID, InheritanceNode>> matchedUsers = plugin.getStorage().searchUserNodes(matcher).join().stream()
                .filter(n -> n.getNode().getValue())
                .collect(Collectors.toList());

        // special handling for default group
        if (target.getName().equals(GroupManager.DEFAULT_GROUP_NAME)) {
            // include all non-saved online players in the results
            for (User user : plugin.getUserManager().getAll().values()) {
                if (!plugin.getUserManager().shouldSave(user)) {
                    matchedUsers.add(NodeEntry.of(user.getUniqueId(), node));
                }
            }

            // send a warning message about this behaviour
            Message.SEARCH_RESULT_GROUP_DEFAULT.send(sender);
        }

        List<NodeEntry<String, InheritanceNode>> matchedGroups = plugin.getStorage().searchGroupNodes(matcher).join().stream()
                .filter(n -> n.getNode().getValue())
                .collect(Collectors.toList());

        int users = matchedUsers.size();
        int groups = matchedGroups.size();

        Message.SEARCH_RESULT.send(sender, users + groups, users, groups);

        if (!matchedUsers.isEmpty()) {
            Map<UUID, String> uuidLookups = LoadingMap.of(u -> plugin.lookupUsername(u).orElseGet(u::toString));
            sendResult(sender, matchedUsers, uuidLookups::get, Message.SEARCH_SHOWING_USERS, HolderType.USER, label, page);
        }

        if (!matchedGroups.isEmpty()) {
            sendResult(sender, matchedGroups, Function.identity(), Message.SEARCH_SHOWING_GROUPS, HolderType.GROUP, label, page);
        }

        return CommandResult.SUCCESS;
    }

    private static <T extends Comparable<T>> void sendResult(Sender sender, List<NodeEntry<T, InheritanceNode>> results, Function<T, String> lookupFunction, Message.Args3<Integer, Integer, Integer> headerMessage, HolderType holderType, String label, int page) {
        results = new ArrayList<>(results);
        results.sort(NodeEntryComparator.normal());

        int pageIndex = page - 1;
        List<List<NodeEntry<T, InheritanceNode>>> pages = Iterators.divideIterable(results, 15);

        if (pageIndex < 0 || pageIndex >= pages.size()) {
            page = 1;
            pageIndex = 0;
        }

        List<NodeEntry<T, InheritanceNode>> content = pages.get(pageIndex);

        List<Map.Entry<String, NodeEntry<T, InheritanceNode>>> mappedContent = content.stream()
                .map(hp -> Maps.immutableEntry(lookupFunction.apply(hp.getHolder()), hp))
                .collect(Collectors.toList());

        // send header
        headerMessage.send(sender, page, pages.size(), results.size());

        for (Map.Entry<String, NodeEntry<T, InheritanceNode>> ent : mappedContent) {
            Message.SEARCH_INHERITS_NODE_ENTRY.send(sender, ent.getValue().getNode(), ent.getKey(), holderType, label, sender.getPlugin());
        }
    }
}
