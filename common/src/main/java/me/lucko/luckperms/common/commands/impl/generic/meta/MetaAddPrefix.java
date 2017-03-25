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

import me.lucko.luckperms.api.MetaUtils;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.ContextHelper;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;

import java.util.List;
import java.util.stream.Collectors;

public class MetaAddPrefix extends SharedSubCommand {
    public MetaAddPrefix() {
        super("addprefix", "Adds a prefix", Permission.USER_META_ADDPREFIX, Permission.GROUP_META_ADDPREFIX,
                Predicates.notInRange(2, 4),
                Arg.list(
                        Arg.create("priority", true, "the priority to add the prefix at"),
                        Arg.create("prefix", true, "the prefix string"),
                        Arg.create("server", false, "the server to add the prefix on"),
                        Arg.create("world", false, "the world to add the prefix on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException {
        int priority = ArgumentUtils.handlePriority(0, args);
        String prefix = ArgumentUtils.handleString(1, args);
        String server = ArgumentUtils.handleServer(2, args);
        String world = ArgumentUtils.handleWorld(3, args);

        final String node = "prefix." + priority + "." + MetaUtils.escapeCharacters(prefix);

        try {
            switch (ContextHelper.determine(server, world)) {
                case NONE:
                    holder.setPermission(NodeFactory.make(node, true));
                    Message.ADDPREFIX_SUCCESS.send(sender, holder.getFriendlyName(), prefix, priority);
                    break;
                case SERVER:
                    holder.setPermission(NodeFactory.make(node, true, server));
                    Message.ADDPREFIX_SERVER_SUCCESS.send(sender, holder.getFriendlyName(), prefix, priority, server);
                    break;
                case SERVER_AND_WORLD:
                    holder.setPermission(NodeFactory.make(node, true, server, world));
                    Message.ADDPREFIX_SERVER_WORLD_SUCCESS.send(sender, holder.getFriendlyName(), prefix, priority, server, world);
                    break;
            }

            LogEntry.build().actor(sender).acted(holder)
                    .action("meta addprefix " + args.stream().map(ArgumentUtils.WRAPPER).collect(Collectors.joining(" ")))
                    .build().submit(plugin, sender);

            save(holder, sender, plugin);
            return CommandResult.SUCCESS;

        } catch (ObjectAlreadyHasException e) {
            Message.ALREADY_HAS_PREFIX.send(sender, holder.getFriendlyName());
            return CommandResult.STATE_ERROR;
        }
    }
}
