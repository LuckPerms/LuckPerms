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
import me.lucko.luckperms.common.core.Node;
import me.lucko.luckperms.common.core.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.utils.ArgumentChecker;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;

import java.util.List;

public class MetaSet extends SecondarySubCommand {
    public MetaSet() {
        super("set", "Sets a meta value",  Permission.USER_SET_META, Permission.GROUP_SET_META, Predicate.notInRange(2, 4),
                Arg.list(
                        Arg.create("key", true, "the key to set"),
                        Arg.create("value", true, "the value to set"),
                        Arg.create("server", false, "the server to add the meta pair on"),
                        Arg.create("world", false, "the world to add the meta pair on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args) {
        String key = MetaUtils.escapeCharacters(args.get(0));
        String value = MetaUtils.escapeCharacters(args.get(1));

        String node = "meta." + key + "." + value;
        String server = null;
        String world = null;

        if (args.size() >= 3) {
            server = args.get(2).toLowerCase();
            if (ArgumentChecker.checkServer(server)) {
                Message.SERVER_INVALID_ENTRY.send(sender);
                return CommandResult.INVALID_ARGS;
            }

            if (args.size() != 3) {
                world = args.get(3).toLowerCase();
            }
        }

        me.lucko.luckperms.api.Node n = new Node.Builder(node).setServer(server).setWorld(world).build();

        if (holder.hasPermission(n).asBoolean()) {
            Message.ALREADY_HAS_META.send(sender, holder.getFriendlyName());
            return CommandResult.STATE_ERROR;
        }

        holder.clearMetaKeys(key, server, world, false);

        try {
            holder.setPermission(n);
        } catch (ObjectAlreadyHasException ignored) {}

        if (server == null) {
            Message.SET_META_SUCCESS.send(sender, key, value, holder.getFriendlyName());
            LogEntry.build().actor(sender).acted(holder)
                    .action("meta set " + key + " " + value)
                    .build().submit(plugin, sender);
        } else {
            if (world == null) {
                Message.SET_META_SERVER_SUCCESS.send(sender, key, value, holder.getFriendlyName(), server);
                LogEntry.build().actor(sender).acted(holder)
                        .action("meta set " + key + " " + value + " " + server)
                        .build().submit(plugin, sender);
            } else {
                Message.SET_META_SERVER_WORLD_SUCCESS.send(sender, key, value, holder.getFriendlyName(), server, world);
                LogEntry.build().actor(sender).acted(holder)
                        .action("meta set " + key + " " + value + " " + server + " " + world)
                        .build().submit(plugin, sender);
            }
        }

        save(holder, sender, plugin);
        return CommandResult.SUCCESS;
    }
}
