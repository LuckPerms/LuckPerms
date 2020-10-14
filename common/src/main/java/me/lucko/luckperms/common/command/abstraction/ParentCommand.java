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
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.tabcomplete.CompletionSupplier;
import me.lucko.luckperms.common.command.tabcomplete.TabCompleter;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public abstract class ParentCommand<T, I> extends Command<Void> {

    /** Child sub commands */
    private final List<Command<T>> children;
    /** The type of parent command */
    private final Type type;

    public ParentCommand(CommandSpec spec, String name, Type type, List<Command<T>> children) {
        super(spec, name, null, Predicates.alwaysFalse());
        this.children = children;
        this.type = type;
    }

    public @NonNull List<Command<T>> getChildren() {
        return this.children;
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Void ignored, ArgumentList args, String label) {
        // check if required argument and/or subcommand is missing
        if (args.size() < this.type.minArgs) {
            sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        Command<T> sub = getChildren().stream()
                .filter(s -> s.getName().equalsIgnoreCase(args.get(this.type.cmdIndex)))
                .findFirst()
                .orElse(null);

        if (sub == null) {
            Message.COMMAND_NOT_RECOGNISED.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (!sub.isAuthorized(sender)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        if (sub.getArgumentCheck().test(args.size() - this.type.minArgs)) {
            sub.sendDetailedUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        final String targetArgument = args.get(0);
        I targetId = null;
        if (this.type == Type.TAKES_ARGUMENT_FOR_TARGET) {
            targetId = parseTarget(targetArgument, plugin, sender);
            if (targetId == null) {
                return CommandResult.LOADING_ERROR;
            }
        }

        ReentrantLock lock = getLockForTarget(targetId);
        lock.lock();
        try {
            T target = getTarget(targetId, plugin, sender);
            if (target == null) {
                return CommandResult.LOADING_ERROR;
            }

            CommandResult result;
            try {
                result = sub.execute(plugin, sender, target, args.subList(this.type.minArgs, args.size()), label);
            } catch (CommandException e) {
                result = e.handle(sender, label, sub);
            }

            cleanup(target, plugin);
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, ArgumentList args) {
        switch (this.type) {
            case TAKES_ARGUMENT_FOR_TARGET:
                return TabCompleter.create()
                        .at(0, CompletionSupplier.startsWith(() -> getTargets(plugin).stream()))
                        .at(1, CompletionSupplier.startsWith(() -> getChildren().stream()
                                .filter(s -> s.isAuthorized(sender))
                                .map(s -> s.getName().toLowerCase())
                        ))
                        .from(2, partial -> getChildren().stream()
                                .filter(s -> s.isAuthorized(sender))
                                .filter(s -> s.getName().equalsIgnoreCase(args.get(1)))
                                .findFirst()
                                .map(cmd -> cmd.tabComplete(plugin, sender, args.subList(2, args.size())))
                                .orElse(Collections.emptyList())
                        )
                        .complete(args);
            case NO_TARGET_ARGUMENT:
                return TabCompleter.create()
                        .at(0, CompletionSupplier.startsWith(() -> getChildren().stream()
                                .filter(s -> s.isAuthorized(sender))
                                .map(s -> s.getName().toLowerCase())
                        ))
                        .from(1, partial -> getChildren().stream()
                                .filter(s -> s.isAuthorized(sender))
                                .filter(s -> s.getName().equalsIgnoreCase(args.get(0)))
                                .findFirst()
                                .map(cmd -> cmd.tabComplete(plugin, sender, args.subList(1, args.size())))
                                .orElse(Collections.emptyList())
                        )
                        .complete(args);
            default:
                throw new AssertionError(this.type);
        }
    }

    @Override
    public void sendUsage(Sender sender, String label) {
        List<Command<?>> subs = getChildren().stream()
                .filter(s -> s.isAuthorized(sender))
                .collect(Collectors.toList());

        if (!subs.isEmpty()) {
            Message.MAIN_COMMAND_USAGE_HEADER.send(sender, getName(), String.format(getUsage(), label));
            for (Command<?> s : subs) {
                s.sendUsage(sender, label);
            }
        } else {
            Message.COMMAND_NO_PERMISSION.send(sender);
        }
    }

    @Override
    public void sendDetailedUsage(Sender sender, String label) {
        sendUsage(sender, label);
    }

    @Override
    public boolean isAuthorized(Sender sender) {
        return getChildren().stream().anyMatch(sc -> sc.isAuthorized(sender));
    }

    protected abstract List<String> getTargets(LuckPermsPlugin plugin);

    protected abstract I parseTarget(String target, LuckPermsPlugin plugin, Sender sender);

    protected abstract ReentrantLock getLockForTarget(I target);

    protected abstract T getTarget(I target, LuckPermsPlugin plugin, Sender sender);

    protected abstract void cleanup(T t, LuckPermsPlugin plugin);

    public enum Type {
        // e.g. /lp log sub-command....
        NO_TARGET_ARGUMENT(0),
        // e.g. /lp user <USER> sub-command....
        TAKES_ARGUMENT_FOR_TARGET(1);

        private final int cmdIndex;
        private final int minArgs;

        Type(int cmdIndex) {
            this.cmdIndex = cmdIndex;
            this.minArgs = cmdIndex + 1;
        }
    }

}
