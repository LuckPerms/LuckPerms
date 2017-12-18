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

package me.lucko.luckperms.common.commands.impl.misc;

import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SingleCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.messaging.ExtendedMessagingService;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.Optional;

public class NetworkSyncCommand extends SingleCommand {
    public NetworkSyncCommand(LocaleManager locale) {
        super(CommandSpec.NETWORK_SYNC.spec(locale), "NetworkSync", CommandPermission.SYNC, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        Message.UPDATE_TASK_REQUEST.send(sender);
        plugin.getUpdateTaskBuffer().request().join();
        Message.UPDATE_TASK_COMPLETE_NETWORK.send(sender);

        Optional<ExtendedMessagingService> messagingService = plugin.getMessagingService();
        if (!messagingService.isPresent()) {
            Message.UPDATE_TASK_PUSH_FAILURE_NOT_SETUP.send(sender);
            return CommandResult.FAILURE;
        }

        try {
            messagingService.get().pushUpdate();
            Message.UPDATE_TASK_PUSH_SUCCESS.send(sender, messagingService.get().getName());
            return CommandResult.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            Message.UPDATE_TASK_PUSH_FAILURE.send(sender);
            return CommandResult.FAILURE;
        }
    }
}
