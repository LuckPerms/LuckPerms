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

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.tabcomplete.CompletionSupplier;
import me.lucko.luckperms.common.command.tabcomplete.TabCompleter;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.verbose.InvalidFilterException;
import me.lucko.luckperms.common.verbose.VerboseFilter;
import me.lucko.luckperms.common.verbose.VerboseHandler;
import me.lucko.luckperms.common.verbose.VerboseListener;
import me.lucko.luckperms.common.web.UnsuccessfulRequestException;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VerboseCommand extends SingleCommand {
    public VerboseCommand(LocaleManager locale) {
        super(CommandSpec.VERBOSE.localize(locale), "Verbose", CommandPermission.VERBOSE, Predicates.is(0));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        if (args.isEmpty()) {
            sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        VerboseHandler verboseHandler = plugin.getVerboseHandler();
        String mode = args.get(0).toLowerCase();

        if (mode.equals("command") || mode.equals("cmd")) {
            if (args.size() < 3) {
                sendDetailedUsage(sender, label);
                return CommandResult.INVALID_ARGS;
            }

            String name = args.get(1);
            Sender executor;

            if (name.equals("me") || name.equals("self") || name.equalsIgnoreCase(sender.getName())) {
                executor = sender;
            } else {
                if (!CommandPermission.VERBOSE_COMMAND_OTHERS.isAuthorized(sender)) {
                    Message.COMMAND_NO_PERMISSION.send(sender);
                    return CommandResult.NO_PERMISSION;
                }

                executor = plugin.getOnlineSenders()
                        .filter(s -> !s.isConsole())
                        .filter(s -> s.getName().equalsIgnoreCase(name))
                        .findAny()
                        .orElse(null);

                if (executor == null) {
                    Message.USER_NOT_ONLINE.send(sender, name);
                    return CommandResult.STATE_ERROR;
                }
            }

            String commandWithSlash = String.join(" ", args.subList(2, args.size()));
            String command = commandWithSlash.charAt(0) == '/' ? commandWithSlash.substring(1) : commandWithSlash;

            plugin.getBootstrap().getScheduler().sync().execute(() -> {
                Message.VERBOSE_ON_COMMAND.send(sender, executor.getName(), command);

                verboseHandler.registerListener(sender, VerboseFilter.acceptAll(), true);
                executor.performCommand(command);
                verboseHandler.unregisterListener(sender);

                Message.VERBOSE_OFF_COMMAND.send(sender);
            });

            return CommandResult.SUCCESS;
        }

        if (mode.equals("on") || mode.equals("true") || mode.equals("record")) {
            List<String> filters = new ArrayList<>();
            if (args.size() != 1) {
                filters.addAll(args.subList(1, args.size()));
            }

            String filter = filters.isEmpty() ? "" : String.join(" ", filters);

            VerboseFilter compiledFilter;
            try {
                compiledFilter = VerboseFilter.compile(filter);
            } catch (InvalidFilterException e) {
                Message.VERBOSE_INVALID_FILTER.send(sender, filter, e.getCause().getMessage());
                return CommandResult.FAILURE;
            }

            boolean notify = !mode.equals("record");

            verboseHandler.registerListener(sender, compiledFilter, notify);

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
            VerboseListener listener = verboseHandler.unregisterListener(sender);

            if (mode.equals("paste") || mode.equals("upload")) {
                if (listener == null) {
                    Message.VERBOSE_OFF.send(sender);
                } else {
                    Message.VERBOSE_UPLOAD_START.send(sender);

                    String id;
                    try {
                        id = listener.uploadPasteData(plugin.getBytebin());
                    } catch (UnsuccessfulRequestException e) {
                        Message.GENERIC_HTTP_REQUEST_FAILURE.send(sender, e.getResponse().code(), e.getResponse().message());
                        return CommandResult.STATE_ERROR;
                    } catch (IOException e) {
                        new RuntimeException("Error uploading data to bytebin", e).printStackTrace();
                        Message.GENERIC_HTTP_UNKNOWN_FAILURE.send(sender);
                        return CommandResult.STATE_ERROR;
                    }

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
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, CompletionSupplier.startsWith("on", "record", "off", "upload", "command"))
                .complete(args);
    }
}
