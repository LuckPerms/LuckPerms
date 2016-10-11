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

package me.lucko.luckperms.commands.meta.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Arg;
import me.lucko.luckperms.commands.CommandResult;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.meta.MetaSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.core.PermissionHolder;
import me.lucko.luckperms.data.LogEntry;
import me.lucko.luckperms.utils.ArgumentChecker;

import java.util.List;
import java.util.stream.Collectors;

public class MetaClear extends MetaSubCommand {
    public MetaClear() {
        super("clear", "Clears all chat meta",  Permission.USER_CLEARMETA, Permission.GROUP_CLEARMETA, Predicate.notInRange(0, 2),
                Arg.list(
                        Arg.create("server", false, "the server name to filter by"),
                        Arg.create("world", false, "the world name to filter by")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args) {
        int before = holder.getNodes().size();

        if (args.size() == 0) {
            holder.clearMeta();
        } else {
            final String server = args.get(0);
            if (ArgumentChecker.checkServer(server)) {
                Message.SERVER_INVALID_ENTRY.send(sender);
                return CommandResult.INVALID_ARGS;
            }

            if (args.size() == 2) {
                final String world = args.get(1);
                holder.clearMeta(server, world);

            } else {
                holder.clearMeta(server);
            }
        }

        int changed = before - holder.getNodes().size();
        if (changed == 1) {
            Message.META_CLEAR_SUCCESS_SINGULAR.send(sender, holder.getFriendlyName(), changed);
        } else {
            Message.META_CLEAR_SUCCESS.send(sender, holder.getFriendlyName(), changed);
        }

        LogEntry.build().actor(sender).acted(holder).action("meta clear " + args.stream().collect(Collectors.joining(" "))).build().submit(plugin, sender);
        save(holder, sender, plugin);
        return CommandResult.SUCCESS;
    }
}
