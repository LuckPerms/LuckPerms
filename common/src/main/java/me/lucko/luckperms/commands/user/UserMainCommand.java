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

package me.lucko.luckperms.commands.user;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;
import java.util.UUID;

public class UserMainCommand extends MainCommand<User> {
    public UserMainCommand() {
        super("User", "/%s user <user>", 2);
    }

    @Override
    protected void getTarget(String target, LuckPermsPlugin plugin, Sender sender, Callback<User> onSuccess) {
        UUID u = Util.parseUuid(target);
        if (u != null) {
            runSub(plugin, sender, u, onSuccess);
            return;
        }

        if (target.length() <= 16) {
            if (Patterns.NON_USERNAME.matcher(target).find()) {
                Message.USER_INVALID_ENTRY.send(sender, target);
                return;
            }

            Message.USER_ATTEMPTING_LOOKUP.send(sender);

            plugin.getDatastore().getUUID(target, uuid -> {
                if (uuid == null) {
                    Message.USER_NOT_FOUND.send(sender);
                    return;
                }

                runSub(plugin, sender, uuid, onSuccess);
            });
            return;
        }

        Message.USER_INVALID_ENTRY.send(sender, target);
    }

    private void runSub(LuckPermsPlugin plugin, Sender sender, UUID uuid, Callback<User> onSuccess) {
        plugin.getDatastore().loadUser(uuid, success -> {
            if (!success) {
                Message.USER_NOT_FOUND.send(sender);
                return;
            }

            User user = plugin.getUserManager().getUser(uuid);
            if (user == null) {
                Message.USER_NOT_FOUND.send(sender);
                return;
            }

            onSuccess.onComplete(user);
            plugin.getUserManager().cleanupUser(user);
        });
    }

    @Override
    protected List<String> getObjects(LuckPermsPlugin plugin) {
        return plugin.getPlayerList();
    }
}
