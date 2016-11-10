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

import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.ContextHelper;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.stream.Collectors;

public class UserClear extends SubCommand<User> {
    public UserClear() {
        super("clear", "Clears the user's permissions and groups", Permission.USER_CLEAR, Predicates.notInRange(0, 2),
                Arg.list(
                        Arg.create("server", false, "the server name to filter by"),
                        Arg.create("world", false, "the world name to filter by")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) throws CommandException {
        int before = user.getNodes().size();

        String server = ArgumentUtils.handleServer(0, args);
        String world = ArgumentUtils.handleWorld(1, args);

        switch (ContextHelper.determine(server, world)) {
            case NONE:
                user.clearNodes();
                break;
            case SERVER:
                user.clearNodes(server);
                break;
            case SERVER_AND_WORLD:
                user.clearNodes(server, world);
                break;
        }

        int changed = before - user.getNodes().size();
        if (changed == 1) {
            Message.CLEAR_SUCCESS_SINGULAR.send(sender, user.getName(), changed);
        } else {
            Message.CLEAR_SUCCESS.send(sender, user.getName(), changed);
        }

        LogEntry.build().actor(sender).acted(user)
                .action("clear " + args.stream().map(ArgumentUtils.WRAPPER).collect(Collectors.joining(" ")))
                .build().submit(plugin, sender);

        save(user, sender, plugin);
        return CommandResult.SUCCESS;
    }
}
