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

package me.lucko.luckperms.common.commands.impl.user;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.commands.abstraction.Command;
import me.lucko.luckperms.common.commands.abstraction.MainCommand;
import me.lucko.luckperms.common.commands.impl.generic.meta.CommandMeta;
import me.lucko.luckperms.common.commands.impl.generic.other.HolderClear;
import me.lucko.luckperms.common.commands.impl.generic.other.HolderEditor;
import me.lucko.luckperms.common.commands.impl.generic.other.HolderShowTracks;
import me.lucko.luckperms.common.commands.impl.generic.parent.CommandParent;
import me.lucko.luckperms.common.commands.impl.generic.permission.CommandPermission;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.constants.DataConstraints;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.List;
import java.util.UUID;

public class UserMainCommand extends MainCommand<User> {
    public UserMainCommand(LocaleManager locale) {
        super(CommandSpec.USER.spec(locale), "User", 2, ImmutableList.<Command<User, ?>>builder()
                .add(new UserInfo(locale))
                .add(new CommandPermission<>(locale, true))
                .add(new CommandParent<>(locale, true))
                .add(new CommandMeta<>(locale, true))
                .add(new HolderEditor<>(locale, true))
                .add(new UserSwitchPrimaryGroup(locale))
                .add(new UserPromote(locale))
                .add(new UserDemote(locale))
                .add(new HolderShowTracks<>(locale, true))
                .add(new HolderClear<>(locale, true))
                .build()
        );
    }

    @Override
    protected User getTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        UUID u = Util.parseUuid(target.toLowerCase());
        if (u == null) {
            if (!DataConstraints.PLAYER_USERNAME_TEST.test(target)) {
                Message.USER_INVALID_ENTRY.send(sender, target);
                return null;
            }

            u = plugin.getStorage().getUUID(target.toLowerCase()).join();
            if (u == null) {
                if (!plugin.getConfiguration().get(ConfigKeys.USE_SERVER_UUID_CACHE)) {
                    Message.USER_NOT_FOUND.send(sender);
                    return null;
                }

                u = plugin.lookupUuid(target).orElse(null);
                if (u == null) {
                    Message.USER_NOT_FOUND.send(sender);
                    return null;
                }
            }
        }

        String name = plugin.getStorage().getName(u).join();
        if (name == null) {
            name = "null";
        }

        if (!plugin.getStorage().loadUser(u, name).join()) {
            Message.LOADING_ERROR.send(sender);
        }

        User user = plugin.getUserManager().getIfLoaded(u);
        if (user == null) {
            Message.LOADING_ERROR.send(sender);
            return null;
        }

        user.auditTemporaryPermissions();
        return user;
    }

    @Override
    protected void cleanup(User user, LuckPermsPlugin plugin) {
        plugin.getUserManager().cleanup(user);
    }

    @Override
    protected List<String> getTargets(LuckPermsPlugin plugin) {
        return plugin.getPlayerList();
    }
}