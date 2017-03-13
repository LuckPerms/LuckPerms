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

package me.lucko.luckperms.common.commands;

import lombok.Getter;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.commands.abstraction.Command;
import me.lucko.luckperms.common.commands.impl.group.CreateGroup;
import me.lucko.luckperms.common.commands.impl.group.DeleteGroup;
import me.lucko.luckperms.common.commands.impl.group.GroupMainCommand;
import me.lucko.luckperms.common.commands.impl.group.ListGroups;
import me.lucko.luckperms.common.commands.impl.log.LogMainCommand;
import me.lucko.luckperms.common.commands.impl.migration.MigrationMainCommand;
import me.lucko.luckperms.common.commands.impl.misc.CheckCommand;
import me.lucko.luckperms.common.commands.impl.misc.ExportCommand;
import me.lucko.luckperms.common.commands.impl.misc.ImportCommand;
import me.lucko.luckperms.common.commands.impl.misc.InfoCommand;
import me.lucko.luckperms.common.commands.impl.misc.NetworkSyncCommand;
import me.lucko.luckperms.common.commands.impl.misc.ReloadConfigCommand;
import me.lucko.luckperms.common.commands.impl.misc.SearchCommand;
import me.lucko.luckperms.common.commands.impl.misc.SyncCommand;
import me.lucko.luckperms.common.commands.impl.misc.TreeCommand;
import me.lucko.luckperms.common.commands.impl.misc.VerboseCommand;
import me.lucko.luckperms.common.commands.impl.track.CreateTrack;
import me.lucko.luckperms.common.commands.impl.track.DeleteTrack;
import me.lucko.luckperms.common.commands.impl.track.ListTracks;
import me.lucko.luckperms.common.commands.impl.track.TrackMainCommand;
import me.lucko.luckperms.common.commands.impl.user.UserMainCommand;
import me.lucko.luckperms.common.commands.impl.usersbulkedit.UsersBulkEditMainCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import io.github.mkremins.fanciful.ChatColor;
import io.github.mkremins.fanciful.FancyMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class CommandManager {

    @Getter
    private final LuckPermsPlugin plugin;

    private final ExecutorService executor;

    @Getter
    private final List<Command> mainCommands;

    public CommandManager(LuckPermsPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor();

        mainCommands = ImmutableList.<Command>builder()
                .add(new UserMainCommand())
                .add(new GroupMainCommand())
                .add(new TrackMainCommand())
                .addAll(plugin.getExtraCommands())
                .add(new LogMainCommand())
                .add(new SyncCommand())
                .add(new InfoCommand())
                .add(new VerboseCommand())
                .add(new TreeCommand())
                .add(new SearchCommand())
                .add(new CheckCommand())
                .add(new NetworkSyncCommand())
                .add(new ImportCommand())
                .add(new ExportCommand())
                .add(new ReloadConfigCommand())
                .add(new MigrationMainCommand())
                .add(new UsersBulkEditMainCommand())
                .add(new CreateGroup())
                .add(new DeleteGroup())
                .add(new ListGroups())
                .add(new CreateTrack())
                .add(new DeleteTrack())
                .add(new ListTracks())
                .build();
    }

    /**
     * Generic on command method to be called from the command executor object of the platform
     * Unlike {@link #execute(Sender, String, List)}, this method is called in a new thread
     * @param sender who sent the command
     * @param label  the command label used
     * @param args   the arguments provided
     */
    public Future<CommandResult> onCommand(Sender sender, String label, List<String> args) {
        return executor.submit(() -> execute(sender, label, args));
    }

    @SuppressWarnings("unchecked")
    private CommandResult execute(Sender sender, String label, List<String> args) {
        // Handle no arguments
        if (args.size() == 0) {
            sendCommandUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        // Look for the main command.
        Optional<Command> o = mainCommands.stream()
                .filter(m -> m.getName().equalsIgnoreCase(args.get(0)))
                .limit(1)
                .findAny();

        // Main command not found
        if (!o.isPresent()) {
            sendCommandUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        // Check the Sender has permission to use the main command.
        final Command main = o.get();
        if (!main.isAuthorized(sender)) {
            sendCommandUsage(sender, label);
            return CommandResult.NO_PERMISSION;
        }

        List<String> arguments = new ArrayList<>(args);
        handleRewrites(arguments);
        arguments.remove(0); // remove the main command arg.

        // Check the correct number of args were given for the main command
        if (main.getArgumentCheck().test(arguments.size())) {
            main.sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        // Try to execute the command.
        CommandResult result;
        try {
            result = main.execute(plugin, sender, null, arguments, label);
        } catch (CommandException e) {
            result = handleException(e, sender, label, main);
        } catch (Exception e) {
            e.printStackTrace();
            result = CommandResult.FAILURE;
        }

        return result;
    }

    /**
     * Generic tab complete method to be called from the command executor object of the platform
     *
     * @param sender who is tab completing
     * @param args   the arguments provided so far
     * @return a list of suggestions
     */
    @SuppressWarnings("unchecked")
    public List<String> onTabComplete(Sender sender, List<String> args) {
        final List<Command> mains = mainCommands.stream()
                .filter(m -> m.isAuthorized(sender))
                .collect(Collectors.toList());

        // Not yet past the point of entering a main command
        if (args.size() <= 1) {

            // Nothing yet entered
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return mains.stream()
                        .map(m -> m.getName().toLowerCase())
                        .collect(Collectors.toList());
            }

            // Started typing a main command
            return mains.stream()
                    .map(m -> m.getName().toLowerCase())
                    .filter(s -> s.startsWith(args.get(0).toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Find a main command matching the first arg
        Optional<Command> o = mains.stream()
                .filter(m -> m.getName().equalsIgnoreCase(args.get(0)))
                .limit(1)
                .findAny();

        if (!o.isPresent()) {
            return Collections.emptyList();
        }

        // Pass the processing onto the main command
        return o.get().tabComplete(plugin, sender, args.subList(1, args.size()));
    }

    private void sendCommandUsage(Sender sender, String label) {
        Util.sendPluginMessage(sender, "&2Running &bLuckPerms v" + plugin.getVersion() + "&2.");
        mainCommands.stream()
                .filter(c -> c.isAuthorized(sender))
                .forEach(c -> {
                    if (!c.shouldDisplay()) {
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    String permission = (String) c.getPermission().map(p -> ((Permission) p).getExample()).orElse("None");
                    FancyMessage msg = new FancyMessage("> ").color(c('3')).then().text(String.format(c.getUsage(), label)).color(c('a'))
                            .formattedTooltip(
                                    new FancyMessage("Command: ").color(c('b')).then().text(c.getName()).color(c('2')),
                                    new FancyMessage("Description: ").color(c('b')).then().text(c.getDescription()).color(c('2')),
                                    new FancyMessage("Usage: ").color(c('b')).then().text(String.format(c.getUsage(), label)).color(c('2')),
                                    new FancyMessage("Permission: ").color(c('b')).then().text(permission).color(c('2')),
                                    new FancyMessage(" "),
                                    new FancyMessage("Click to auto-complete.").color(c('7'))
                            )
                            .suggest(String.format(c.getUsage(), label));
                    sender.sendMessage(msg);
                });
    }
    
    private static ChatColor c(char c) {
        return ChatColor.getByChar(c);
    }

    public static CommandResult handleException(CommandException e, Sender sender, String label, Command command) {
        if (e instanceof ArgumentUtils.ArgumentException) {
            if (e instanceof ArgumentUtils.DetailedUsageException) {
                command.sendDetailedUsage(sender, label);
                return CommandResult.INVALID_ARGS;
            }

            if (e instanceof ArgumentUtils.UseInheritException) {
                Message.USE_INHERIT_COMMAND.send(sender);
                return CommandResult.INVALID_ARGS;
            }

            if (e instanceof ArgumentUtils.InvalidServerException) {
                Message.SERVER_INVALID_ENTRY.send(sender);
                return CommandResult.INVALID_ARGS;
            }

            if (e instanceof ArgumentUtils.PastDateException) {
                Message.PAST_DATE_ERROR.send(sender);
                return CommandResult.INVALID_ARGS;
            }

            if (e instanceof ArgumentUtils.InvalidDateException) {
                Message.ILLEGAL_DATE_ERROR.send(sender, ((ArgumentUtils.InvalidDateException) e).getInvalidDate());
                return CommandResult.INVALID_ARGS;
            }

            if (e instanceof ArgumentUtils.InvalidPriorityException) {
                Message.META_INVALID_PRIORITY.send(sender, ((ArgumentUtils.InvalidPriorityException) e).getInvalidPriority());
                return CommandResult.INVALID_ARGS;
            }
        }

        // Not something we can catch.
        e.printStackTrace();
        return CommandResult.FAILURE;
    }

    private static void handleRewrites(List<String> args) {
        if (args.size() >= 3) {
            if (!args.get(0).equalsIgnoreCase("user") && !args.get(0).equalsIgnoreCase("group")) {
                return;
            }

            String s = args.get(2).toLowerCase();
            switch (s) {
                // Provide aliases
                case "p":
                case "perm":
                case "perms":
                    args.remove(2);
                    args.add(2, "permission");
                    break;
                case "chat":
                case "m":
                    args.remove(2);
                    args.add(2, "meta");
                    break;
                case "i":
                case "inherit":
                case "inheritances":
                case "group":
                case "groups":
                case "rank":
                case "ranks":
                case "parents":
                    args.remove(2);
                    args.add(2, "parent");
                    break;

                // Provide backwards compatibility
                case "setprimarygroup":
                    args.remove(2);
                    args.add(2, "switchprimarygroup");
                    break;
                case "listnodes":
                    args.remove(2);
                    args.add(2, "permission");
                    args.add(3, "info");
                    break;
                case "set":
                case "unset":
                case "settemp":
                case "unsettemp":
                    args.add(2, "permission");
                    break;
                case "haspermission":
                    args.remove(2);
                    args.add(2, "permission");
                    args.add(3, "check");
                    break;
                case "inheritspermission":
                    args.remove(2);
                    args.add(2, "permission");
                    args.add(3, "checkinherits");
                    break;
                case "listgroups":
                    args.remove(2);
                    args.add(2, "parent");
                    args.add(3, "info");
                    break;
                case "addgroup":
                case "setinherit":
                    args.remove(2);
                    args.add(2, "parent");
                    args.add(3, "add");
                    break;
                case "setgroup":
                    args.remove(2);
                    args.add(2, "parent");
                    args.add(3, "set");
                    break;
                case "removegroup":
                case "unsetinherit":
                    args.remove(2);
                    args.add(2, "parent");
                    args.add(3, "remove");
                    break;
                case "addtempgroup":
                case "settempinherit":
                    args.remove(2);
                    args.add(2, "parent");
                    args.add(3, "addtemp");
                    break;
                case "removetempgroup":
                case "unsettempinherit":
                    args.remove(2);
                    args.add(2, "parent");
                    args.add(3, "removetemp");
                    break;
                case "chatmeta":
                    args.remove(2);
                    args.add(2, "meta");
                    args.add(3, "info");
                    break;
                case "addprefix":
                case "addsuffix":
                case "removeprefix":
                case "removesuffix":
                case "addtempprefix":
                case "addtempsuffix":
                case "removetempprefix":
                case "removetempsuffix":
                    args.add(2, "meta");
                    break;
                default:
                    break;
            }

            // Provide lazy set rewrite
            boolean lazySet = (
                    args.size() >= 6 &&
                    args.get(2).equalsIgnoreCase("permission") &&
                    args.get(3).toLowerCase().startsWith("set") &&
                    (args.get(5).equalsIgnoreCase("none") || args.get(5).equalsIgnoreCase("0"))
            );

            if (lazySet) {
                args.remove(5);
                args.remove(3);
                args.add(3, "unset");
            }
        }
    }
}
