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

package me.lucko.luckperms.common.commands.user;

import com.google.common.collect.ImmutableList;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.MainCommand;
import me.lucko.luckperms.common.commands.Sender;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.Util;
import me.lucko.luckperms.common.commands.generic.meta.CommandMeta;
import me.lucko.luckperms.common.commands.generic.parent.CommandParent;
import me.lucko.luckperms.common.commands.user.subcommands.*;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Patterns;
import me.lucko.luckperms.common.users.User;

import java.util.List;
import java.util.UUID;

public class UserMainCommand extends MainCommand<User> {
    public UserMainCommand() {
        super("User", "/%s user <user>", 2, ImmutableList.<SubCommand<User>>builder()
            .add(new UserInfo())
            .add(new CommandParent<>(true))
            .add(new CommandMeta<>(true))
            .add(new UserGetUUID())
            .add(new UserListNodes())
            .add(new UserHasPerm())
            .add(new UserInheritsPerm())
            .add(new UserSetPermission())
            .add(new UserUnSetPermission())
            .add(new UserSetTempPermission())
            .add(new UserUnsetTempPermission())
            .add(new UserSetPrimaryGroup())
            .add(new UserShowTracks())
            .add(new UserPromote())
            .add(new UserDemote())
            .add(new UserShowPos())
            .add(new UserBulkChange())
            .add(new UserClear())
            .build()
        );
    }

    @Override
    protected User getTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        UUID u = Util.parseUuid(target);
        if (u == null) {
            if (target.length() <= 16) {
                if (Patterns.NON_USERNAME.matcher(target).find()) {
                    Message.USER_INVALID_ENTRY.send(sender, target);
                    return null;
                }

                u = plugin.getDatastore().getUUID(target);
                if (u == null) {
                    Message.USER_NOT_FOUND.send(sender);
                    return null;
                }
            } else {
                Message.USER_INVALID_ENTRY.send(sender, target);
            }
        }

        String name = plugin.getDatastore().getName(u);
        if (name == null) name = "null";

        if (!plugin.getDatastore().loadUser(u, name)) {
            Message.LOADING_ERROR.send(sender);
        }

        User user = plugin.getUserManager().get(u);
        if (user == null) {
            Message.LOADING_ERROR.send(sender);
            return null;
        }

        return user;
    }

    @Override
    protected void cleanup(User user, LuckPermsPlugin plugin) {
        plugin.getUserManager().cleanup(user);
    }

    @Override
    protected List<String> getObjects(LuckPermsPlugin plugin) {
        return plugin.getPlayerList();
    }
}
