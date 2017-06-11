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
import me.lucko.luckperms.common.commands.impl.misc.ApplyEditsCommand;
import me.lucko.luckperms.common.commands.impl.misc.BulkUpdateCommand;
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
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.TextUtils;

import net.kyori.text.Component;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.serializer.ComponentSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandManager {
    public static final Pattern COMMAND_SEPARATOR_PATTERN = Pattern.compile(" (?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)");

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
                .add(new BulkUpdateCommand())
                .add(new MigrationMainCommand())
                .add(new ApplyEditsCommand())
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
        return executor.submit(() -> {
            try {
                return execute(sender, label, args);
            } catch (Throwable e) {
                plugin.getLog().severe("Exception whilst executing command: " + args.toString());
                e.printStackTrace();
                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private CommandResult execute(Sender sender, String label, List<String> args) {
        List<String> arguments = new ArrayList<>(args);
        handleRewrites(arguments);

        // Handle no arguments
        if (arguments.size() == 0) {
            sendCommandUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        // Look for the main command.
        Optional<Command> o = mainCommands.stream()
                .filter(m -> m.getName().equalsIgnoreCase(arguments.get(0)))
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

        arguments.remove(0); // remove the main command arg.

        // Check the correct number of args were given for the main command
        if (main.getArgumentCheck().test(arguments.size())) {
            main.sendDetailedUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        // Try to execute the command.
        CommandResult result;
        try {
            result = main.execute(plugin, sender, null, arguments, label);
        } catch (CommandException e) {
            result = handleException(e, sender, label, main);
        } catch (Throwable e) {
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
        List<String> arguments = new ArrayList<>(args);

        // we rewrite tab completions too!
        handleRewrites(arguments);

        final List<Command> mains = mainCommands.stream()
                .filter(Command::shouldDisplay)
                .filter(m -> m.isAuthorized(sender))
                .collect(Collectors.toList());

        // Not yet past the point of entering a main command
        if (arguments.size() <= 1) {

            // Nothing yet entered
            if (arguments.isEmpty() || arguments.get(0).equalsIgnoreCase("")) {
                return mains.stream()
                        .map(m -> m.getName().toLowerCase())
                        .collect(Collectors.toList());
            }

            // Started typing a main command
            return mains.stream()
                    .map(m -> m.getName().toLowerCase())
                    .filter(s -> s.startsWith(arguments.get(0).toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Find a main command matching the first arg
        Optional<Command> o = mains.stream()
                .filter(m -> m.getName().equalsIgnoreCase(arguments.get(0)))
                .limit(1)
                .findAny();

        arguments.remove(0); // remove the main command arg.

        if (!o.isPresent()) {
            return Collections.emptyList();
        }

        // Pass the processing onto the main command
        return o.get().tabComplete(plugin, sender, arguments);
    }

    private void sendCommandUsage(Sender sender, String label) {
        Util.sendPluginMessage(sender, "&2Running &bLuckPerms v" + plugin.getVersion() + "&2.");
        mainCommands.stream()
                .filter(Command::shouldDisplay)
                .filter(c -> c.isAuthorized(sender))
                .forEach(c -> {
                    @SuppressWarnings("unchecked")
                    String permission = (String) c.getPermission().map(p -> ((Permission) p).getExample()).orElse("None");

                    Component component = ComponentSerializer.parseFromLegacy("&3> &a" + String.format(c.getUsage(), label), Constants.FORMAT_CHAR)
                            .applyRecursively(comp -> {
                                comp.hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentSerializer.parseFromLegacy(TextUtils.joinNewline(
                                        "&bCommand: &2" + c.getName(),
                                        "&bDescription: &2" + c.getDescription(),
                                        "&bUsage: &2" + String.format(c.getUsage(), label),
                                        "&bPermission: &2" + permission,
                                        " ",
                                        "&7Click to auto-complete."
                                        ), Constants.FORMAT_CHAR)));
                                comp.clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.format(c.getUsage(), label)));
                            });
                    sender.sendMessage(component);
                });
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

            if (e instanceof ArgumentUtils.InvalidServerWorldException) {
                Message.SERVER_WORLD_INVALID_ENTRY.send(sender);
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
        if (args.size() >= 1) {
            if (args.get(0).equalsIgnoreCase("u")) {
                args.remove(0);
                args.add(0, "user");
            }
            if (args.get(0).equalsIgnoreCase("g")) {
                args.remove(0);
                args.add(0, "group");
            }
        }

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
                case "about":
                case "list":
                    args.remove(2);
                    args.add(2, "info");
                    break;
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

            // provide lazy info
            boolean lazyInfo = (
                    args.size() >= 4 &&
                    (args.get(2).equalsIgnoreCase("permission") || args.get(2).equalsIgnoreCase("parent") || args.get(2).equalsIgnoreCase("meta")) &&
                    (args.get(3).equalsIgnoreCase("i") || args.get(3).equalsIgnoreCase("about") || args.get(3).equalsIgnoreCase("list"))
            );

            if (lazyInfo) {
                args.remove(3);
                args.add(3, "info");
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
