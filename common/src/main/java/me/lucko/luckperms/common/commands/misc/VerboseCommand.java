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

package me.lucko.luckperms.common.commands.misc;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.verbose.InvalidFilterException;
import me.lucko.luckperms.common.verbose.VerboseFilter;
import me.lucko.luckperms.common.verbose.VerboseListener;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VerboseCommand extends SingleCommand {
    public VerboseCommand(LocaleManager locale) {
        super(CommandSpec.VERBOSE.localize(locale), "Verbose", CommandPermission.VERBOSE, Predicates.is(0));
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

            String filter = filters.isEmpty() ? "" : String.join(" ", filters);

            VerboseFilter compiledFilter;
            try {
                compiledFilter = new VerboseFilter(filter);
            } catch (InvalidFilterException e) {
                Message.VERBOSE_INVALID_FILTER.send(sender, filter, e.getCause().getMessage());
                return CommandResult.FAILURE;
            }

            boolean notify = !mode.equals("record");

            plugin.getVerboseHandler().registerListener(sender, compiledFilter, notify);

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

        if (mode.equals("off") || mode.equals("false") || mode.equals("paste") || mode.equals("upload")) {
            VerboseListener listener = plugin.getVerboseHandler().unregisterListener(sender.getUniqueId());

            if (mode.equals("paste") || mode.equals("upload")) {
                if (listener == null) {
                    Message.VERBOSE_OFF.send(sender);
                } else {
                    Message.VERBOSE_UPLOAD_START.send(sender);
                    String id = listener.uploadPasteData(plugin.getBytebin());
                    String url = plugin.getConfiguration().get(ConfigKeys.VERBOSE_VIEWER_URL_PATTERN) + id;

                    Message.VERBOSE_RESULTS_URL.send(sender);

                    Component message = TextComponent.builder(url).color(TextColor.AQUA)
                            .clickEvent(ClickEvent.openUrl(url))
                            .hoverEvent(HoverEvent.showText(TextComponent.of("Click to open the results page.").color(TextColor.GRAY)))
                            .build();

                    sender.sendMessage(message);
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
            return ImmutableList.of("on", "record", "off", "upload");
        }

        if (args.size() == 1) {
            return Stream.of("on", "record", "off", "upload").filter(s -> s.toLowerCase().startsWith(args.get(0).toLowerCase())).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
