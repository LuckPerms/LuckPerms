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

package me.lucko.luckperms.common.commands.group.subcommands;

import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.*;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.utils.ArgumentChecker;

import java.util.List;
import java.util.stream.Collectors;

public class GroupClear extends SubCommand<Group> {
    public GroupClear() {
        super("clear", "Clears the group's permissions and parent groups", Permission.GROUP_CLEAR, Predicate.notInRange(0, 2),
                Arg.list(
                        Arg.create("server", false, "the server name to filter by"),
                        Arg.create("world", false, "the world name to filter by")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        int before = group.getNodes().size();

        if (args.size() == 0) {
            group.clearNodes();
        } else {
            final String server = args.get(0);
            if (ArgumentChecker.checkServer(server)) {
                Message.SERVER_INVALID_ENTRY.send(sender);
                return CommandResult.INVALID_ARGS;
            }

            if (args.size() == 2) {
                final String world = args.get(1);
                group.clearNodes(server, world);

            } else {
                group.clearNodes(server);
            }
        }

        int changed = before - group.getNodes().size();
        if (changed == 1) {
            Message.CLEAR_SUCCESS_SINGULAR.send(sender, group.getName(), changed);
        } else {
            Message.CLEAR_SUCCESS.send(sender, group.getName(), changed);
        }

        LogEntry.build().actor(sender).acted(group).action("clear " + args.stream().collect(Collectors.joining(" "))).build().submit(plugin, sender);
        save(group, sender, plugin);
        return CommandResult.SUCCESS;
    }
}
