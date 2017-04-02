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

package me.lucko.luckperms.common.commands.impl.generic.meta;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.TemporaryModifier;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.stream.Collectors;

public class MetaSetTemp extends SharedSubCommand {
    public MetaSetTemp() {
        super("settemp", "Sets a meta value temporarily", Permission.USER_META_SETTEMP, Permission.GROUP_META_SETTEMP, Predicates.inRange(0, 2),
                Arg.list(
                        Arg.create("key", true, "the key to set"),
                        Arg.create("value", true, "the value to set"),
                        Arg.create("duration", true, "the duration until the meta value expires"),
                        Arg.create("context...", false, "the contexts to add the meta pair in")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException {
        String key = args.get(0);
        String value = args.get(1);
        long duration = ArgumentUtils.handleDuration(2, args);
        MutableContextSet context = ArgumentUtils.handleContext(2, args);
        TemporaryModifier modifier = plugin.getConfiguration().get(ConfigKeys.TEMPORARY_ADD_BEHAVIOUR);

        Node n = NodeFactory.makeMetaNode(key, value).withExtraContext(context).setExpiry(duration).build();

        if (holder.hasPermission(n).asBoolean()) {
            Message.ALREADY_HAS_META.send(sender, holder.getFriendlyName());
            return CommandResult.STATE_ERROR;
        }

        holder.clearMetaKeys(key, context, true);
        duration = holder.setPermission(n, modifier).getValue().getExpiryUnixTime();

        Message.SET_META_TEMP_SUCCESS.send(sender, key, value, holder.getFriendlyName(), DateUtil.formatDateDiff(duration), Util.contextSetToString(context));

        LogEntry.build().actor(sender).acted(holder)
                .action("meta settemp " + args.stream().map(ArgumentUtils.WRAPPER).collect(Collectors.joining(" ")))
                .build().submit(plugin, sender);

        save(holder, sender, plugin);
        return CommandResult.SUCCESS;
    }
}
