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
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MetaRemoveSuffix extends SharedSubCommand {
    public MetaRemoveSuffix() {
        super("removesuffix", "Removes a suffix", Permission.USER_META_REMOVESUFFIX, Permission.GROUP_META_REMOVESUFFIX,
                Predicates.notInRange(2, 4),
                Arg.list(
                        Arg.create("priority", true, "the priority to add the suffix at"),
                        Arg.create("suffix", true, "the suffix string"),
                        Arg.create("server", false, "the server to add the suffix on"),
                        Arg.create("world", false, "the world to add the suffix on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException {
        int priority = ArgumentUtils.handlePriority(0, args);
        String suffix = ArgumentUtils.handleNodeWithoutCheck(1, args);
        String server = ArgumentUtils.handleServer(2, args);
        String world = ArgumentUtils.handleWorld(3, args);

        // Handle bulk removal
        if (suffix.equalsIgnoreCase("null")) {
            List<Node> toRemove = new ArrayList<>();
            for (Node node : holder.getNodes()) {
                if (!node.isSuffix()) continue;
                if (node.getSuffix().getKey() != priority) continue;
                if (node.isTemporary()) continue;

                if (node.getServer().isPresent()) {
                    if (server == null) continue;
                    if (!node.getServer().get().equalsIgnoreCase(server)) continue;
                } else {
                    if (server != null) continue;
                }

                if (node.getWorld().isPresent()) {
                    if (world == null) continue;
                    if (!node.getWorld().get().equalsIgnoreCase(world)) continue;
                } else {
                    if (world != null) continue;
                }

                toRemove.add(node);
            }

            toRemove.forEach(n -> {
                try {
                    holder.unsetPermission(n);
                } catch (ObjectLacksException ignored) {
                }
            });

            Message.BULK_CHANGE_SUCCESS.send(sender, toRemove.size());
            save(holder, sender, plugin);
            return CommandResult.SUCCESS;
        }

        final String node = "suffix." + priority + "." + MetaUtils.escapeCharacters(suffix);

        try {
            switch (ContextHelper.determine(server, world)) {
                case NONE:
                    holder.unsetPermission(node);
                    Message.REMOVESUFFIX_SUCCESS.send(sender, holder.getFriendlyName(), suffix, priority);
                    break;
                case SERVER:
                    holder.unsetPermission(node, server);
                    Message.REMOVESUFFIX_SERVER_SUCCESS.send(sender, holder.getFriendlyName(), suffix, priority, server);
                    break;
                case SERVER_AND_WORLD:
                    holder.unsetPermission(node, server, world);
                    Message.REMOVESUFFIX_SERVER_WORLD_SUCCESS.send(sender, holder.getFriendlyName(), suffix, priority, server, world);
                    break;
            }

            LogEntry.build().actor(sender).acted(holder)
                    .action("meta removesuffix " + args.stream().map(ArgumentUtils.WRAPPER).collect(Collectors.joining(" ")))
                    .build().submit(plugin, sender);

            save(holder, sender, plugin);
            return CommandResult.SUCCESS;

        } catch (ObjectLacksException e) {
            Message.DOES_NOT_HAVE_SUFFIX.send(sender, holder.getFriendlyName());
            return CommandResult.STATE_ERROR;
        }
    }
}
