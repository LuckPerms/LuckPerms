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
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SingleCommand;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.verbose.CheckOrigin;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CheckCommand extends SingleCommand {
    public CheckCommand(LocaleManager locale) {
        super(CommandSpec.CHECK.spec(locale), "Check", CommandPermission.CHECK, Predicates.not(2));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) throws CommandException {
        String target = args.get(0);
        String permission = args.get(1);

        User user;
        UUID u = CommandUtils.parseUuid(target);
        if (u != null) {
            user = plugin.getUserManager().getIfLoaded(u);
        } else {
            user = plugin.getUserManager().getByUsername(target);
        }

        if (user == null) {
            Message.USER_NOT_ONLINE.send(sender, target);
            return CommandResult.STATE_ERROR;
        }

        Tristate tristate = user.getCachedData().getPermissionData(plugin.getContextForUser(user)).getPermissionValue(permission, CheckOrigin.INTERNAL);
        Message.CHECK_RESULT.send(sender, user.getFriendlyName(), permission, CommandUtils.formatTristate(tristate));
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        if (args.isEmpty()) {
            return plugin.getPlayerList().collect(Collectors.toList());
        }

        if (args.size() == 1) {
            return plugin.getPlayerList().filter(s -> s.toLowerCase().startsWith(args.get(0).toLowerCase())).collect(Collectors.toList());
        }

        args.remove(0);
        return SubCommand.getPermissionTabComplete(args, plugin.getPermissionVault());
    }
}
