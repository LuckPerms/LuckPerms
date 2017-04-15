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

package me.lucko.luckperms.common.commands.impl.group;

import com.google.common.collect.Maps;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;

import io.github.mkremins.fanciful.ChatColor;
import io.github.mkremins.fanciful.FancyMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GroupListMembers extends SubCommand<Group> {
    public GroupListMembers() {
        super("listmembers", "Show the users/groups who inherit from this group",
                Permission.GROUP_LISTMEMBERS, Predicates.notInRange(0, 1),
                Arg.list(
                        Arg.create("page", false, "the page to view")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) throws CommandException {
        String query = "group." + group.getName();
        int page = ArgumentUtils.handleIntOrElse(0, args, 1);

        Message.SEARCH_SEARCHING_MEMBERS.send(sender, group.getName());

        List<HeldPermission<UUID>> matchedUsers = plugin.getStorage().getUsersWithPermission(query).join();
        List<HeldPermission<String>> matchedGroups = plugin.getStorage().getGroupsWithPermission(query).join();

        int users = matchedUsers.size();
        int groups = matchedGroups.size();

        Message.SEARCH_RESULT.send(sender, users + groups, users, groups);

        Map<UUID, String> uuidLookups = new HashMap<>();
        Function<UUID, String> lookupFunc = uuid -> uuidLookups.computeIfAbsent(uuid, u -> {
            String s = plugin.getStorage().getName(u).join();
            if (s == null || s.isEmpty() || s.equals("null")) {
                s = u.toString();
            }
            return s;
        });

        Map.Entry<FancyMessage, String> msgUsers = searchUserResultToMessage(matchedUsers, lookupFunc, label, page);
        Map.Entry<FancyMessage, String> msgGroups = searchGroupResultToMessage(matchedGroups, label, page);

        if (msgUsers.getValue() != null) {
            Message.SEARCH_SHOWING_USERS_WITH_PAGE.send(sender, msgUsers.getValue());
            sender.sendMessage(msgUsers.getKey());
        } else {
            Message.SEARCH_SHOWING_USERS.send(sender);
            sender.sendMessage(msgUsers.getKey());
        }

        if (msgGroups.getValue() != null) {
            Message.SEARCH_SHOWING_GROUPS_WITH_PAGE.send(sender, msgGroups.getValue());
            sender.sendMessage(msgGroups.getKey());
        } else {
            Message.SEARCH_SHOWING_GROUPS.send(sender);
            sender.sendMessage(msgGroups.getKey());
        }

        return CommandResult.SUCCESS;
    }

    private static Map.Entry<FancyMessage, String> searchUserResultToMessage(List<HeldPermission<UUID>> results, Function<UUID, String> uuidLookup, String label, int pageNumber) {
        if (results.isEmpty()) {
            return Maps.immutableEntry(new FancyMessage("None").color(ChatColor.getByChar('3')), null);
        }

        List<HeldPermission<UUID>> sorted = new ArrayList<>(results);
        sorted.sort(Comparator.comparing(HeldPermission::getHolder));

        int index = pageNumber - 1;
        List<List<HeldPermission<UUID>>> pages = Util.divideList(sorted, 15);

        if (index < 0 || index >= pages.size()) {
            pageNumber = 1;
            index = 0;
        }

        List<HeldPermission<UUID>> page = pages.get(index);
        List<Map.Entry<String, HeldPermission<UUID>>> uuidMappedPage = page.stream()
                .map(hp -> Maps.immutableEntry(uuidLookup.apply(hp.getHolder()), hp))
                .collect(Collectors.toList());

        FancyMessage message = new FancyMessage("");
        String title = "&7(page &f" + pageNumber + "&7 of &f" + pages.size() + "&7 - &f" + sorted.size() + "&7 entries)";

        for (Map.Entry<String, HeldPermission<UUID>> ent : uuidMappedPage) {
            message.then("> ").color(ChatColor.getByChar('3')).apply(m -> makeFancy(m, ent.getKey(), false, label, ent.getValue()))
                    .then(ent.getKey() + " ").color(ChatColor.getByChar('b')).apply(m -> makeFancy(m, ent.getKey(), false, label, ent.getValue()))
                    .apply(ent.getValue().asNode(), GroupListMembers::appendNodeExpiry)
                    .apply(ent.getValue().asNode(), Util::appendNodeContextDescription)
                    .then("\n");
        }

        return Maps.immutableEntry(message, title);
    }

    private static Map.Entry<FancyMessage, String> searchGroupResultToMessage(List<HeldPermission<String>> results, String label, int pageNumber) {
        if (results.isEmpty()) {
            return Maps.immutableEntry(new FancyMessage("None").color(ChatColor.getByChar('3')), null);
        }

        List<HeldPermission<String>> sorted = new ArrayList<>(results);
        sorted.sort(Comparator.comparing(HeldPermission::getHolder));

        int index = pageNumber - 1;
        List<List<HeldPermission<String>>> pages = Util.divideList(sorted, 15);

        if (index < 0 || index >= pages.size()) {
            pageNumber = 1;
            index = 0;
        }

        List<HeldPermission<String>> page = pages.get(index);

        FancyMessage message = new FancyMessage("");
        String title = "&7(page &f" + pageNumber + "&7 of &f" + pages.size() + "&7 - &f" + sorted.size() + "&7 entries)";

        for (HeldPermission<String> ent : page) {
            message.then("> ").color(ChatColor.getByChar('3')).apply(m -> makeFancy(m, ent.getHolder(), true, label, ent))
                    .then(ent.getHolder() + " ").color(ChatColor.getByChar('b')).apply(m -> makeFancy(m, ent.getHolder(), true, label, ent))
                    .apply(ent.asNode(), GroupListMembers::appendNodeExpiry)
                    .apply(ent.asNode(), Util::appendNodeContextDescription)
                    .then("\n");
        }

        return Maps.immutableEntry(message, title);
    }

    private static void appendNodeExpiry(FancyMessage message, Node node) {
        if (!node.isTemporary()) {
            return;
        }

        message.then(" (").color(ChatColor.getByChar('8'))
                .then("expires in " + DateUtil.formatDateDiff(node.getExpiryUnixTime())).color(ChatColor.getByChar('7'))
                .then(")").color(ChatColor.getByChar('8'));
    }

    private static void makeFancy(FancyMessage message, String holderName, boolean group, String label, HeldPermission<?> perm) {
        Node node = perm.asNode();

        message = message.formattedTooltip(
                new FancyMessage("> ")
                        .color(ChatColor.getByChar('3'))
                        .then(node.getPermission())
                        .color(node.getValue() ? ChatColor.getByChar('a') : ChatColor.getByChar('c')),
                new FancyMessage(" "),
                new FancyMessage("Click to remove this parent from " + holderName).color(ChatColor.getByChar('7'))
        );

        String command = NodeFactory.nodeAsCommand(node, holderName, group)
                .replace("/luckperms", "/" + label)
                .replace("permission set", "permission unset")
                .replace("parent add", "parent remove")
                .replace(" true", "")
                .replace(" false", "");

        message.suggest(command);
    }
}
