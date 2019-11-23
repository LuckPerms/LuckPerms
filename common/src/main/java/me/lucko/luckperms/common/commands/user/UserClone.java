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

package me.lucko.luckperms.common.commands.user;

import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SubCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.utils.ArgumentParser;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;

import net.luckperms.api.model.data.DataType;

import java.util.List;
import java.util.UUID;

public class UserClone extends SubCommand<User> {
    public UserClone(LocaleManager locale) {
        super(CommandSpec.USER_CLONE.localize(locale), "clone", CommandPermission.USER_CLONE, Predicates.not(1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), user)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        UUID uuid = ArgumentParser.parseUserTarget(0, args, plugin, sender);
        if (uuid == null) {
            return CommandResult.INVALID_ARGS;
        }

        User otherUser = plugin.getStorage().loadUser(uuid, null).join();
        if (otherUser == null) {
            Message.USER_LOAD_ERROR.send(sender);
            return CommandResult.LOADING_ERROR;
        }

        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), otherUser)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        otherUser.replaceNodes(DataType.NORMAL, user.normalData().immutable());

        Message.CLONE_SUCCESS.send(sender, user.getFormattedDisplayName(), otherUser.getFormattedDisplayName());

        LoggedAction.build().source(sender).target(otherUser)
                .description("clone", user.getUsername())
                .build().submit(plugin, sender);

        StorageAssistant.save(otherUser, sender, plugin);
        plugin.getUserManager().getHouseKeeper().cleanup(otherUser.getUniqueId());
        return CommandResult.SUCCESS;
    }
}
