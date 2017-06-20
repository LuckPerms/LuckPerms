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

package me.lucko.luckperms.common.commands.impl.log;

import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.data.Log;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LogNotify extends SubCommand<Log> {
    public LogNotify(LocaleManager locale) {
        super(CommandSpec.LOG_NOTIFY.spec(locale), "notify", Permission.LOG_NOTIFY, Predicates.notInRange(0, 1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Log log, List<String> args, String label) throws CommandException {
        final Set<UUID> ignoring = plugin.getIgnoringLogs();
        final UUID uuid = sender.getUuid();
        if (args.size() == 0) {
            if (ignoring.contains(uuid)) {
                // toggle on
                ignoring.remove(uuid);
                Message.LOG_NOTIFY_TOGGLE_ON.send(sender);
                return CommandResult.SUCCESS;
            }
            // toggle off
            ignoring.add(uuid);
            Message.LOG_NOTIFY_TOGGLE_OFF.send(sender);
            return CommandResult.SUCCESS;
        }

        if (args.get(0).equalsIgnoreCase("on")) {
            if (!ignoring.contains(uuid)) {
                // already on
                Message.LOG_NOTIFY_ALREADY_ON.send(sender);
                return CommandResult.STATE_ERROR;
            }

            // toggle on
            ignoring.remove(uuid);
            Message.LOG_NOTIFY_TOGGLE_ON.send(sender);
            return CommandResult.SUCCESS;
        }

        if (args.get(0).equalsIgnoreCase("off")) {
            if (ignoring.contains(uuid)) {
                // already off
                Message.LOG_NOTIFY_ALREADY_OFF.send(sender);
                return CommandResult.STATE_ERROR;
            }

            // toggle off
            ignoring.add(uuid);
            Message.LOG_NOTIFY_TOGGLE_OFF.send(sender);
            return CommandResult.SUCCESS;
        }

        // not recognised
        Message.LOG_NOTIFY_UNKNOWN.send(sender);
        return CommandResult.INVALID_ARGS;
    }
}
