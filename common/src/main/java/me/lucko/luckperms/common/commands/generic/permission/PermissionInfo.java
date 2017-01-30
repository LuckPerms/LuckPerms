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

package me.lucko.luckperms.common.commands.generic.permission;

import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.generic.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.utils.Predicates;

import io.github.mkremins.fanciful.FancyMessage;

import java.util.List;
import java.util.Map;

public class PermissionInfo extends SharedSubCommand {
    public PermissionInfo() {
        super("info", "Lists the permission nodes the object has", Permission.USER_PERM_INFO,
                Permission.GROUP_PERM_INFO, Predicates.alwaysFalse(),
                Arg.list(
                        Arg.create("page", false, "the page to view")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException {
        if (sender.getUuid().equals(Constants.CONSOLE_UUID)) {
            Message.LISTNODES.send(sender, holder.getFriendlyName());
            sender.sendMessage(Util.color(Util.permNodesToStringConsole(holder.getPermissions(false))));
        } else {
            int page = ArgumentUtils.handleIntOrElse(0, args, 1);

            Map.Entry<FancyMessage, String> ent = Util.permNodesToMessage(holder.getPermissions(false), holder, label, page);
            if (ent.getValue() != null) {
                Message.LISTNODES_WITH_PAGE.send(sender, holder.getFriendlyName(), ent.getValue());
                sender.sendMessage(ent.getKey());
            } else {
                Message.LISTNODES.send(sender, holder.getFriendlyName());
                sender.sendMessage(ent.getKey());
            }
        }

        Message.LISTNODES_TEMP.send(sender, holder.getFriendlyName(), Util.tempNodesToString(holder.getPermissions(false)));
        return CommandResult.SUCCESS;
    }
}
