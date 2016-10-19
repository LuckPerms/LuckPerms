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

package me.lucko.luckperms.common.commands.generic.meta;

import me.lucko.luckperms.api.MetaUtils;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.Predicate;
import me.lucko.luckperms.common.commands.Sender;
import me.lucko.luckperms.common.commands.generic.SecondarySubCommand;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.utils.ArgumentChecker;

import java.util.List;
import java.util.stream.Collectors;

public class MetaUnset extends SecondarySubCommand {
    public MetaUnset() {
        super("unset", "Unsets a meta value",  Permission.USER_META_UNSET, Permission.GROUP_META_UNSET, Predicate.notInRange(1, 3),
                Arg.list(
                        Arg.create("key", true, "the key to unset"),
                        Arg.create("server", false, "the server to remove the meta pair on"),
                        Arg.create("world", false, "the world to remove the meta pair on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args) {
        String key = MetaUtils.escapeCharacters(args.get(0));

        String server = null;
        String world = null;

        if (args.size() >= 2) {
            server = args.get(1).toLowerCase();
            if (ArgumentChecker.checkServer(server)) {
                Message.SERVER_INVALID_ENTRY.send(sender);
                return CommandResult.INVALID_ARGS;
            }

            if (args.size() != 2) {
                world = args.get(2).toLowerCase();
            }
        }

        holder.clearMetaKeys(key, server, world, false);

        if (server == null) {
            Message.UNSET_META_SUCCESS.send(sender, key, holder.getFriendlyName());
        } else {
            if (world == null) {
                Message.UNSET_META_SERVER_SUCCESS.send(sender, key, holder.getFriendlyName(), server);
            } else {
                Message.UNSET_META_SERVER_WORLD_SUCCESS.send(sender, key, holder.getFriendlyName(), server, world);
            }
        }

        LogEntry.build().actor(sender).acted(holder)
                .action("meta unset " + args.stream().collect(Collectors.joining(" ")))
                .build().submit(plugin, sender);

        save(holder, sender, plugin);
        return CommandResult.SUCCESS;
    }
}
