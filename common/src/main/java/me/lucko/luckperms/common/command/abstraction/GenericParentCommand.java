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

package me.lucko.luckperms.common.command.abstraction;

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.tabcomplete.CompletionSupplier;
import me.lucko.luckperms.common.command.tabcomplete.TabCompleter;
import me.lucko.luckperms.common.locale.command.LocalizedCommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A shared main command. Shared meaning it can apply to both users and groups.
 * This extends sub command as they're actually sub commands of the main user/group commands.
 * @param <T>
 */
public class GenericParentCommand<T extends PermissionHolder> extends ChildCommand<T> {

    private final List<GenericChildCommand> children;

    private final HolderType type;

    public GenericParentCommand(LocalizedCommandSpec spec, String name, HolderType type, List<GenericChildCommand> children) {
        super(spec, name, null, Predicates.alwaysFalse());
        this.children = children;
        this.type = type;
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, T holder, List<String> args, String label) {
        if (args.isEmpty()) {
            sendUsageDetailed(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        GenericChildCommand sub = this.children.stream()
                .filter(s -> s.getName().equalsIgnoreCase(args.get(0)))
                .findFirst()
                .orElse(null);

        if (sub == null) {
            Message.COMMAND_NOT_RECOGNISED.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (!sub.isAuthorized(sender, this.type)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        if (sub.getArgumentCheck().test(args.size() - 1)) {
            sub.sendDetailedUsage(sender);
            return CommandResult.INVALID_ARGS;
        }

        CommandResult result;
        try {
            result = sub.execute(plugin, sender, holder, args.subList(1, args.size()), label, this.type == HolderType.USER ? sub.getUserPermission() : sub.getGroupPermission());
        } catch (CommandException e) {
            result = e.handle(sender, sub);
        }
        return result;
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return TabCompleter.create()
                .at(0, CompletionSupplier.startsWith(() -> this.children.stream()
                        .filter(s -> s.isAuthorized(sender, this.type))
                        .map(s -> s.getName().toLowerCase())
                ))
                .from(1, partial -> this.children.stream()
                        .filter(s -> s.isAuthorized(sender, this.type))
                        .filter(s -> s.getName().equalsIgnoreCase(args.get(0)))
                        .findFirst()
                        .map(cmd -> cmd.tabComplete(plugin, sender, args.subList(1, args.size())))
                        .orElse(Collections.emptyList())
                )
                .complete(args);
    }

    @Override
    public boolean isAuthorized(Sender sender) {
        return this.children.stream().anyMatch(sc -> sc.isAuthorized(sender, this.type));
    }

    private void sendUsageDetailed(Sender sender, String label) {
        List<GenericChildCommand> subs = this.children.stream()
                .filter(s -> s.isAuthorized(sender, this.type))
                .collect(Collectors.toList());

        if (!subs.isEmpty()) {
            switch (this.type) {
                case USER:
                    Message.MAIN_COMMAND_USAGE_HEADER.send(sender, getName(), String.format("/%s user <user> " + getName().toLowerCase(), label));
                    break;
                case GROUP:
                    Message.MAIN_COMMAND_USAGE_HEADER.send(sender, getName(), String.format("/%s group <group> " + getName().toLowerCase(), label));
                    break;
                default:
                    throw new AssertionError(this.type);
            }

            for (GenericChildCommand s : subs) {
                s.sendUsage(sender);
            }

        } else {
            Message.COMMAND_NO_PERMISSION.send(sender);
        }
    }

}
