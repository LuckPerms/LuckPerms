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

package me.lucko.luckperms.common.commands.log;

import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;

import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;

import java.util.Optional;
import java.util.UUID;

public class LogNotify extends ChildCommand<Log> {
    private static final String IGNORE_NODE = "luckperms.log.notify.ignoring";

    public LogNotify() {
        super(CommandSpec.LOG_NOTIFY, "notify", CommandPermission.LOG_NOTIFY, Predicates.notInRange(0, 1));
    }

    public static boolean isIgnoring(LuckPermsPlugin plugin, UUID uuid) {
        User user = plugin.getUserManager().getIfLoaded(uuid);
        if (user == null) {
            return false;
        }

        Optional<? extends Node> node = user.normalData().nodesInContext(ImmutableContextSetImpl.EMPTY).stream()
                .filter(n -> n.getKey().equalsIgnoreCase(IGNORE_NODE))
                .findFirst();

        // if they don't have the perm, they're not ignoring
        // if set to false, ignore it, return false
        return node.map(Node::getValue).orElse(false);
    }

    private static void setIgnoring(LuckPermsPlugin plugin, UUID uuid, boolean state) {
        User user = plugin.getUserManager().getIfLoaded(uuid);
        if (user == null) {
            return;
        }

        if (state) {
            // add the perm
            user.setNode(DataType.NORMAL, NodeBuilders.determineMostApplicable(IGNORE_NODE).build(), true);
        } else {
            // remove the perm
            user.removeIf(DataType.NORMAL, ImmutableContextSetImpl.EMPTY, n -> n.getKey().equalsIgnoreCase(IGNORE_NODE), false);
        }

        plugin.getStorage().saveUser(user).join();
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Log log, ArgumentList args, String label) {
        if (sender.isConsole()) {
            Message.LOG_NOTIFY_CONSOLE.send(sender);
            return CommandResult.SUCCESS;
        }

        final UUID uuid = sender.getUniqueId();
        if (args.isEmpty()) {
            if (isIgnoring(plugin, uuid)) {
                // toggle on
                setIgnoring(plugin, uuid, false);
                Message.LOG_NOTIFY_TOGGLE_ON.send(sender);
                return CommandResult.SUCCESS;
            }
            // toggle off
            setIgnoring(plugin, uuid, true);
            Message.LOG_NOTIFY_TOGGLE_OFF.send(sender);
            return CommandResult.SUCCESS;
        }

        if (args.get(0).equalsIgnoreCase("on")) {
            if (!isIgnoring(plugin, uuid)) {
                // already on
                Message.LOG_NOTIFY_ALREADY_ON.send(sender);
                return CommandResult.STATE_ERROR;
            }

            // toggle on
            setIgnoring(plugin, uuid, false);
            Message.LOG_NOTIFY_TOGGLE_ON.send(sender);
            return CommandResult.SUCCESS;
        }

        if (args.get(0).equalsIgnoreCase("off")) {
            if (isIgnoring(plugin, uuid)) {
                // already off
                Message.LOG_NOTIFY_ALREADY_OFF.send(sender);
                return CommandResult.STATE_ERROR;
            }

            // toggle off
            setIgnoring(plugin, uuid, true);
            Message.LOG_NOTIFY_TOGGLE_OFF.send(sender);
            return CommandResult.SUCCESS;
        }

        // not recognised
        Message.LOG_NOTIFY_UNKNOWN.send(sender);
        return CommandResult.INVALID_ARGS;
    }
}
