/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
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

package me.lucko.luckperms.common.commands.impl.misc;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SingleCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.verbose.VerboseListener;

import io.github.mkremins.fanciful.ChatColor;
import io.github.mkremins.fanciful.FancyMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VerboseCommand extends SingleCommand {
    public VerboseCommand() {
        super("Verbose", "Manage verbose permission checking", "/%s verbose <true|false> [filter]", Permission.VERBOSE, Predicates.is(0),
                Arg.list(
                        Arg.create("on|record|off|paste", true, "whether to enable/disable logging, or to paste the logged output"),
                        Arg.create("filter", false, "the filter to match entries against")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        if (args.isEmpty()) {
            sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        String mode = args.get(0).toLowerCase();

        if (mode.equals("on") || mode.equals("true") || mode.equals("record")) {
            List<String> filters = new ArrayList<>();
            if (args.size() != 1) {
                filters.addAll(args.subList(1, args.size()));
            }

            String filter = filters.isEmpty() ? "" : filters.stream().collect(Collectors.joining(" "));

            if (!VerboseListener.isValidFilter(filter)) {
                Message.VERBOSE_INVALID_FILTER.send(sender, filter);
                return CommandResult.FAILURE;
            }

            boolean notify = !mode.equals("record");

            plugin.getVerboseHandler().register(sender, filter, notify);

            if (notify) {
                if (!filter.equals("")) {
                    Message.VERBOSE_ON_QUERY.send(sender, filter);
                } else {
                    Message.VERBOSE_ON.send(sender);
                }
            } else {
                if (!filter.equals("")) {
                    Message.VERBOSE_RECORDING_ON_QUERY.send(sender, filter);
                } else {
                    Message.VERBOSE_RECORDING_ON.send(sender);
                }
            }

            return CommandResult.SUCCESS;
        }

        if (mode.equals("off") || mode.equals("false") || mode.equals("paste")) {
            VerboseListener listener = plugin.getVerboseHandler().unregister(sender.getUuid());

            if (mode.equals("paste")) {
                if (listener == null) {
                    Message.VERBOSE_OFF.send(sender);
                } else {
                    Message.VERBOSE_RECORDING_UPLOAD_START.send(sender);

                    String url = listener.uploadPasteData();
                    if (url == null) {
                        url = "null";
                    }

                    Message.VERBOSE_RECORDING_URL.send(sender);
                    sender.sendMessage(new FancyMessage(url).color(ChatColor.getByChar('b')).link(url));
                    return CommandResult.SUCCESS;
                }
            } else {
                Message.VERBOSE_OFF.send(sender);
            }

            return CommandResult.SUCCESS;
        }

        sendUsage(sender, label);
        return CommandResult.INVALID_ARGS;
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        if (args.isEmpty()) {
            return ImmutableList.of("on", "record", "off", "paste");
        }

        if (args.size() == 1) {
            return Stream.of("on", "record", "off", "paste").filter(s -> s.toLowerCase().startsWith(args.get(0).toLowerCase())).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
