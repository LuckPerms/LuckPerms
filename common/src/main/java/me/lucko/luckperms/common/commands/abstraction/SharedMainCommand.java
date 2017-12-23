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

package me.lucko.luckperms.common.commands.abstraction;

import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.locale.LocalizedSpec;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A shared main command. Shared meaning it can apply to both users and groups.
 * This extends sub command as they're actually sub commands of the main user/group commands.
 * @param <T>
 */
public class SharedMainCommand<T extends PermissionHolder> extends SubCommand<T> {

    private final List<SharedSubCommand> secondaryCommands;

    /**
     * If this instance of the shared command is targeting a user. Otherwise, it targets a group.
     */
    private final boolean user;

    public SharedMainCommand(LocalizedSpec spec, String name, boolean user, List<SharedSubCommand> secondaryCommands) {
        super(spec, name, null, Predicates.alwaysFalse());
        this.secondaryCommands = secondaryCommands;
        this.user = user;
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, T t, List<String> args, String label) throws CommandException {
        if (args.size() == 0) {
            sendUsageDetailed(sender, user, label);
            return CommandResult.INVALID_ARGS;
        }

        Optional<SharedSubCommand> o = secondaryCommands.stream()
                .filter(s -> s.getName().equalsIgnoreCase(args.get(0)))
                .limit(1)
                .findAny();

        if (!o.isPresent()) {
            Message.COMMAND_NOT_RECOGNISED.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        final SharedSubCommand sub = o.get();
        if (!sub.isAuthorized(sender, user)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        List<String> strippedArgs = new ArrayList<>();
        if (args.size() > 1) {
            strippedArgs.addAll(args.subList(1, args.size()));
        }

        if (sub.getArgumentCheck().test(strippedArgs.size())) {
            sub.sendDetailedUsage(sender);
            return CommandResult.INVALID_ARGS;
        }

        CommandResult result;
        try {
            result = sub.execute(plugin, sender, t, strippedArgs, label, user ? sub.getUserPermission() : sub.getGroupPermission());
        } catch (CommandException e) {
            result = handleException(e, sender, sub);
        }
        return result;
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        final List<SharedSubCommand> subs = secondaryCommands.stream()
                .filter(s -> s.isAuthorized(sender, user))
                .collect(Collectors.toList());

        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return subs.stream()
                        .map(m -> m.getName().toLowerCase())
                        .collect(Collectors.toList());
            }

            return subs.stream()
                    .map(m -> m.getName().toLowerCase())
                    .filter(s -> s.toLowerCase().startsWith(args.get(0).toLowerCase()))
                    .collect(Collectors.toList());
        }

        Optional<SharedSubCommand> o = subs.stream()
                .filter(s -> s.getName().equalsIgnoreCase(args.get(0)))
                .limit(1)
                .findAny();

        return o.map(cmd -> cmd.onTabComplete(plugin, sender, args.subList(1, args.size()))).orElseGet(Collections::emptyList);

    }

    @Override
    public boolean isAuthorized(Sender sender) {
        return secondaryCommands.stream().anyMatch(sc -> sc.isAuthorized(sender, user));
    }

    private void sendUsageDetailed(Sender sender, boolean user, String label) {
        List<SharedSubCommand> subs = secondaryCommands.stream()
                .filter(s -> s.isAuthorized(sender, user))
                .collect(Collectors.toList());

        if (subs.size() > 0) {
            if (user) {
                CommandUtils.sendPluginMessage(sender, "&b" + getName() + " Sub Commands: &7(" + String.format("/%s user <user> " + getName().toLowerCase() + " ...)", label));
            } else {
                CommandUtils.sendPluginMessage(sender, "&b" + getName() + " Sub Commands: &7(" + String.format("/%s group <group> " + getName().toLowerCase() + " ...)", label));
            }

            for (SharedSubCommand s : subs) {
                s.sendUsage(sender);
            }

        } else {
            Message.COMMAND_NO_PERMISSION.send(sender);
        }
    }

    private static CommandResult handleException(CommandException e, Sender sender, SharedSubCommand command) {
        if (e instanceof ArgumentUtils.ArgumentException) {
            if (e instanceof ArgumentUtils.DetailedUsageException) {
                command.sendDetailedUsage(sender);
                return CommandResult.INVALID_ARGS;
            }

            if (e instanceof ArgumentUtils.InvalidServerWorldException) {
                Message.SERVER_WORLD_INVALID_ENTRY.send(sender);
                return CommandResult.INVALID_ARGS;
            }

            if (e instanceof ArgumentUtils.PastDateException) {
                Message.PAST_DATE_ERROR.send(sender);
                return CommandResult.INVALID_ARGS;
            }

            if (e instanceof ArgumentUtils.InvalidDateException) {
                Message.ILLEGAL_DATE_ERROR.send(sender, ((ArgumentUtils.InvalidDateException) e).getInvalidDate());
                return CommandResult.INVALID_ARGS;
            }

            if (e instanceof ArgumentUtils.InvalidPriorityException) {
                Message.META_INVALID_PRIORITY.send(sender, ((ArgumentUtils.InvalidPriorityException) e).getInvalidPriority());
                return CommandResult.INVALID_ARGS;
            }
        }

        // Not something we can catch.
        e.printStackTrace();
        return CommandResult.FAILURE;
    }
}
