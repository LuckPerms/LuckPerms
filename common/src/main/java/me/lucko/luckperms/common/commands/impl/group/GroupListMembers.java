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
import me.lucko.luckperms.common.commands.ArgumentPermissions;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.CommandPermission;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.TextUtils;

import net.kyori.text.BuildableComponent;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GroupListMembers extends SubCommand<Group> {
    public GroupListMembers(LocaleManager locale) {
        super(CommandSpec.GROUP_LISTMEMBERS.spec(locale), "listmembers", CommandPermission.GROUP_LIST_MEMBERS, Predicates.notInRange(0, 1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) throws CommandException {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), group)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

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

        Map.Entry<Component, String> msgUsers = searchUserResultToMessage(matchedUsers, lookupFunc, label, page);
        Map.Entry<Component, String> msgGroups = searchGroupResultToMessage(matchedGroups, label, page);

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

    private static Map.Entry<Component, String> searchUserResultToMessage(List<HeldPermission<UUID>> results, Function<UUID, String> uuidLookup, String label, int pageNumber) {
        if (results.isEmpty()) {
            return Maps.immutableEntry(TextComponent.builder("None").color(TextColor.DARK_AQUA).build(), null);
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

        TextComponent.Builder message = TextComponent.builder("");
        String title = "&7(page &f" + pageNumber + "&7 of &f" + pages.size() + "&7 - &f" + sorted.size() + "&7 entries)";

        for (Map.Entry<String, HeldPermission<UUID>> ent : uuidMappedPage) {
            String s = "&3> &b" + ent.getKey() + " " + getNodeExpiryString(ent.getValue().asNode()) + Util.getAppendableNodeContextString(ent.getValue().asNode()) + "\n";
            message.append(TextUtils.fromLegacy(s, Constants.FORMAT_CHAR).toBuilder().applyDeep(makeFancy(ent.getKey(), false, label, ent.getValue())).build());
        }

        return Maps.immutableEntry(message.build(), title);
    }

    private static Map.Entry<Component, String> searchGroupResultToMessage(List<HeldPermission<String>> results, String label, int pageNumber) {
        if (results.isEmpty()) {
            return Maps.immutableEntry(TextComponent.builder("None").color(TextColor.DARK_AQUA).build(), null);
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

        TextComponent.Builder message = TextComponent.builder("");
        String title = "&7(page &f" + pageNumber + "&7 of &f" + pages.size() + "&7 - &f" + sorted.size() + "&7 entries)";

        for (HeldPermission<String> ent : page) {
            String s = "&3> &b" + ent.getHolder() + " " + getNodeExpiryString(ent.asNode()) + Util.getAppendableNodeContextString(ent.asNode()) + "\n";
            message.append(TextUtils.fromLegacy(s, Constants.FORMAT_CHAR).toBuilder().applyDeep(makeFancy(ent.getHolder(), true, label, ent)).build());
        }

        return Maps.immutableEntry(message.build(), title);
    }

    private static String getNodeExpiryString(Node node) {
        if (!node.isTemporary()) {
            return "";
        }

        return " &8(&7expires in " + DateUtil.formatDateDiff(node.getExpiryUnixTime()) + "&8)";
    }

    private static Consumer<BuildableComponent.Builder<? ,?>> makeFancy(String holderName, boolean group, String label, HeldPermission<?> perm) {
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextUtils.fromLegacy(TextUtils.joinNewline(
                "&3> " + (perm.asNode().getValuePrimitive() ? "&a" : "&c") + perm.asNode().getGroupName(),
                " ",
                "&7Click to remove this parent from " + holderName
        ), Constants.FORMAT_CHAR));

        String command = "/" + label + " " + NodeFactory.nodeAsCommand(perm.asNode(), holderName, group, false);

        return component -> {
            component.hoverEvent(hoverEvent);
            component.clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        };
    }
}
