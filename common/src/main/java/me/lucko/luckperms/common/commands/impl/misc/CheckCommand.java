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

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.caching.UserCache;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SingleCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.UUID;

public class CheckCommand extends SingleCommand {
    public CheckCommand() {
        super("Check", "Perform a standard permission check on an online player",
                "/%s check <user> <permission>", Permission.CHECK, Predicates.not(2),
                Arg.list(
                        Arg.create("user", true, "the user to check"),
                        Arg.create("permission", true, "the permission to check for")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) throws CommandException {
        String target = args.get(0);
        String permission = args.get(1);

        User user;
        UUID u = Util.parseUuid(target);
        if (u != null) {
            user = plugin.getUserManager().get(u);
        } else {
            user = plugin.getUserManager().getByUsername(target);
        }

        if (user == null) {
            Message.USER_NOT_ONLINE.send(sender, target);
            return CommandResult.STATE_ERROR;
        }

        UserCache data = user.getUserData();
        if (data == null) {
            Message.USER_NO_DATA.send(sender, user.getName());
            return CommandResult.STATE_ERROR;
        }

        Tristate tristate = data.getPermissionData(plugin.getContextForUser(user)).getPermissionValue(permission);
        Message.CHECK_RESULT.send(sender, user.getName(), permission, Util.formatTristate(tristate));
        return CommandResult.SUCCESS;
    }
}
