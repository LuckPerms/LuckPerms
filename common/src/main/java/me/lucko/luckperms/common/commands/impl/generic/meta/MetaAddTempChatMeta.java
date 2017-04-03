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
import me.lucko.luckperms.common.core.DataMutateResult;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.TemporaryModifier;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MetaAddTempChatMeta extends SharedSubCommand {
    private static final Function<Boolean, String> DESCRIPTOR = b -> b ? "prefix" : "suffix";
    private final boolean isPrefix;

    public MetaAddTempChatMeta(boolean isPrefix) {
        super("addtemp" + DESCRIPTOR.apply(isPrefix),
                "Adds a " + DESCRIPTOR.apply(isPrefix) + " temporarily",
                isPrefix ? Permission.USER_META_ADDTEMP_PREFIX : Permission.USER_META_ADDTEMP_SUFFIX,
                isPrefix ? Permission.GROUP_META_ADDTEMP_PREFIX : Permission.GROUP_META_ADDTEMP_SUFFIX,
                Predicates.inRange(0, 2),
                Arg.list(
                        Arg.create("priority", true, "the priority to add the " + DESCRIPTOR.apply(isPrefix) + " at"),
                        Arg.create(DESCRIPTOR.apply(isPrefix), true, "the " + DESCRIPTOR.apply(isPrefix) + " string"),
                        Arg.create("duration", true, "the duration until the " + DESCRIPTOR.apply(isPrefix) + " expires"),
                        Arg.create("context...", false, "the contexts to add the " + DESCRIPTOR.apply(isPrefix) + " in")
                )
        );
        this.isPrefix = isPrefix;
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException {
        int priority = ArgumentUtils.handlePriority(0, args);
        String meta = ArgumentUtils.handleString(1, args);
        long duration = ArgumentUtils.handleDuration(2, args);
        MutableContextSet context = ArgumentUtils.handleContext(3, args);
        TemporaryModifier modifier = plugin.getConfiguration().get(ConfigKeys.TEMPORARY_ADD_BEHAVIOUR);

        Map.Entry<DataMutateResult, Node> ret = holder.setPermission(NodeFactory.makeChatMetaNode(isPrefix, priority, meta).setExpiry(duration).withExtraContext(context).build(), modifier);

        if (ret.getKey().asBoolean()) {
            duration = ret.getValue().getExpiryUnixTime();

            Message.ADD_TEMP_CHATMETA_SUCCESS.send(sender, holder.getFriendlyName(), DESCRIPTOR.apply(isPrefix), meta, priority, DateUtil.formatDateDiff(duration), Util.contextSetToString(context));

            LogEntry.build().actor(sender).acted(holder)
                    .action("meta addtemp" + DESCRIPTOR.apply(isPrefix) + " " + args.stream().map(ArgumentUtils.WRAPPER).collect(Collectors.joining(" ")))
                    .build().submit(plugin, sender);

            save(holder, sender, plugin);
            return CommandResult.SUCCESS;
        } else {
            Message.ALREADY_HAS_CHAT_META.send(sender, holder.getFriendlyName(), DESCRIPTOR.apply(isPrefix));
            return CommandResult.STATE_ERROR;
        }
    }
}
