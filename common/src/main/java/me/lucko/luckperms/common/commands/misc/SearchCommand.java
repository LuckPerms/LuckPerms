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

package me.lucko.luckperms.common.commands.misc;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SingleCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.utils.Predicates;

import io.github.mkremins.fanciful.FancyMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class SearchCommand extends SingleCommand {
    public SearchCommand() {
        super("Search", "Search for users/groups with a specific permission",
                "/%s search <permission>", Permission.SEARCH, Predicates.notInRange(1, 2),
                Arg.list(
                        Arg.create("permission", true, "the permission to search for"),
                        Arg.create("page", false, "the page to view")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) throws CommandException {
        String query = args.get(0);

        int page = 1;
        if (args.size() > 0) {
            try {
                page = Integer.parseInt(args.get(0));
            } catch (NumberFormatException e) {
                // ignored
            }
        }

        Message.SEARCH_SEARCHING.send(sender, query);

        List<HeldPermission<UUID>> matchedUsers = plugin.getStorage().getUsersWithPermission(query).join();
        List<HeldPermission<String>> matchedGroups = plugin.getStorage().getGroupsWithPermission(query).join();

        int users = matchedUsers.size();
        int groups = matchedGroups.size();

        Message.SEARCH_RESULT.send(sender, users + groups, users, groups);

        Map<UUID, String> uuidLookups = new HashMap<>();
        Function<UUID, String> lookupFunc = uuid -> uuidLookups.computeIfAbsent(uuid, u -> {
            String s = plugin.getStorage().getName(u).join();
            if (s == null) {
                s = "null";
            }
            return s;
        });

        Map.Entry<FancyMessage, String> msgUsers = Util.searchUserResultToMessage(matchedUsers, lookupFunc, label, page);
        Map.Entry<FancyMessage, String> msgGroups = Util.searchGroupResultToMessage(matchedGroups, label, page);

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
}
