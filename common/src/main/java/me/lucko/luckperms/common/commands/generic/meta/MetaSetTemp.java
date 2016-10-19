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
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;

import java.util.List;

public class MetaSetTemp extends SecondarySubCommand {
    public MetaSetTemp() {
        super("settemp", "Sets a meta value temporarily",  Permission.USER_META_SETTEMP, Permission.GROUP_META_SETTEMP, Predicate.notInRange(3, 5),
                Arg.list(
                        Arg.create("key", true, "the key to set"),
                        Arg.create("value", true, "the value to set"),
                        Arg.create("duration", true, "the duration until the meta value expires"),
                        Arg.create("server", false, "the server to add the meta pair on"),
                        Arg.create("world", false, "the world to add the meta pair on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args) {
        long duration;
        try {
            duration = Long.parseLong(args.get(2));
        } catch (NumberFormatException e) {
            try {
                duration = DateUtil.parseDateDiff(args.get(2), true);
            } catch (DateUtil.IllegalDateException e1) {
                Message.ILLEGAL_DATE_ERROR.send(sender, args.get(2));
                return CommandResult.INVALID_ARGS;
            }
        }

        if (DateUtil.shouldExpire(duration)) {
            Message.PAST_DATE_ERROR.send(sender);
            return CommandResult.INVALID_ARGS;
        }

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

        me.lucko.luckperms.api.Node n = new Node.Builder(node).setServer(server).setWorld(world).setExpiry(duration).build();

        if (holder.hasPermission(n).asBoolean()) {
            Message.ALREADY_HAS_META.send(sender, holder.getFriendlyName());
            return CommandResult.STATE_ERROR;
        }

        holder.clearMetaKeys(key, server, world, true);

        try {
            holder.setPermission(n);
        } catch (ObjectAlreadyHasException ignored) {}

        if (server == null) {
            Message.SET_META_TEMP_SUCCESS.send(sender, key, value, holder.getFriendlyName(), DateUtil.formatDateDiff(duration));
            LogEntry.build().actor(sender).acted(holder)
                    .action("meta settemp " + key + " " + value + " " + duration)
                    .build().submit(plugin, sender);
        } else {
            if (world == null) {
                Message.SET_META_TEMP_SERVER_SUCCESS.send(sender, key, value, holder.getFriendlyName(), server, DateUtil.formatDateDiff(duration));
                LogEntry.build().actor(sender).acted(holder)
                        .action("meta settemp " + key + " " + value + " " + duration + " " + server)
                        .build().submit(plugin, sender);
            } else {
                Message.SET_META_TEMP_SERVER_WORLD_SUCCESS.send(sender, key, value, holder.getFriendlyName(), server, world, DateUtil.formatDateDiff(duration));
                LogEntry.build().actor(sender).acted(holder)
                        .action("meta settemp " + key + " " + value + " " + duration + " " + server + " " + world)
                        .build().submit(plugin, sender);
            }
        }

        save(holder, sender, plugin);
        return CommandResult.SUCCESS;
    }
}
