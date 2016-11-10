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
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.generic.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.ContextHelper;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeBuilder;
import me.lucko.luckperms.common.core.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;

import java.util.List;
import java.util.stream.Collectors;

public class MetaSetTemp extends SharedSubCommand {
    public MetaSetTemp() {
        super("settemp", "Sets a meta value temporarily",  Permission.USER_META_SETTEMP, Permission.GROUP_META_SETTEMP, Predicates.notInRange(3, 5),
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
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args) throws CommandException {
        String key = MetaUtils.escapeCharacters(args.get(0));
        String value = MetaUtils.escapeCharacters(args.get(1));
        long duration = ArgumentUtils.handleDuration(2, args);
        String node = "meta." + key + "." + value;
        String server = ArgumentUtils.handleServer(3, args);
        String world = ArgumentUtils.handleWorld(4, args);

        Node n = new NodeBuilder(node).setServer(server).setWorld(world).setExpiry(duration).build();

        if (holder.hasPermission(n).asBoolean()) {
            Message.ALREADY_HAS_META.send(sender, holder.getFriendlyName());
            return CommandResult.STATE_ERROR;
        }

        holder.clearMetaKeys(key, server, world, true);

        try {
            holder.setPermission(n);
        } catch (ObjectAlreadyHasException ignored) {}

        switch (ContextHelper.determine(server, world)) {
            case NONE:
                Message.SET_META_TEMP_SUCCESS.send(sender, key, value, holder.getFriendlyName(), DateUtil.formatDateDiff(duration));
                break;
            case SERVER:
                Message.SET_META_TEMP_SERVER_SUCCESS.send(sender, key, value, holder.getFriendlyName(), server, DateUtil.formatDateDiff(duration));
                break;
            case SERVER_AND_WORLD:
                Message.SET_META_TEMP_SERVER_WORLD_SUCCESS.send(sender, key, value, holder.getFriendlyName(), server, world, DateUtil.formatDateDiff(duration));
                break;
        }

        LogEntry.build().actor(sender).acted(holder)
                .action("meta settemp " + args.stream().map(ArgumentUtils.WRAPPER).collect(Collectors.joining(" ")))
                .build().submit(plugin, sender);

        save(holder, sender, plugin);
        return CommandResult.SUCCESS;
    }
}
