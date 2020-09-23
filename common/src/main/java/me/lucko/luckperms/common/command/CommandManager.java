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

package me.lucko.luckperms.common.command;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.command.abstraction.Command;
import me.lucko.luckperms.common.command.abstraction.CommandException;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.tabcomplete.CompletionSupplier;
import me.lucko.luckperms.common.command.tabcomplete.TabCompleter;
import me.lucko.luckperms.common.command.tabcomplete.TabCompletions;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.commands.group.CreateGroup;
import me.lucko.luckperms.common.commands.group.DeleteGroup;
import me.lucko.luckperms.common.commands.group.GroupParentCommand;
import me.lucko.luckperms.common.commands.group.ListGroups;
import me.lucko.luckperms.common.commands.log.LogParentCommand;
import me.lucko.luckperms.common.commands.migration.MigrationParentCommand;
import me.lucko.luckperms.common.commands.misc.ApplyEditsCommand;
import me.lucko.luckperms.common.commands.misc.BulkUpdateCommand;
import me.lucko.luckperms.common.commands.misc.CheckCommand;
import me.lucko.luckperms.common.commands.misc.EditorCommand;
import me.lucko.luckperms.common.commands.misc.ExportCommand;
import me.lucko.luckperms.common.commands.misc.ImportCommand;
import me.lucko.luckperms.common.commands.misc.InfoCommand;
import me.lucko.luckperms.common.commands.misc.NetworkSyncCommand;
import me.lucko.luckperms.common.commands.misc.ReloadConfigCommand;
import me.lucko.luckperms.common.commands.misc.SearchCommand;
import me.lucko.luckperms.common.commands.misc.SyncCommand;
import me.lucko.luckperms.common.commands.misc.TreeCommand;
import me.lucko.luckperms.common.commands.misc.VerboseCommand;
import me.lucko.luckperms.common.commands.track.CreateTrack;
import me.lucko.luckperms.common.commands.track.DeleteTrack;
import me.lucko.luckperms.common.commands.track.ListTracks;
import me.lucko.luckperms.common.commands.track.TrackParentCommand;
import me.lucko.luckperms.common.commands.user.UserParentCommand;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.ImmutableCollectors;
import me.lucko.luckperms.common.util.TextUtils;

import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Root command manager for the '/luckperms' command.
 */
public class CommandManager {

    private final LuckPermsPlugin plugin;

    // the default executor to run commands on
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final TabCompletions tabCompletions;

    private final Map<String, Command<?>> mainCommands;

    public CommandManager(LuckPermsPlugin plugin) {
        this.plugin = plugin;
        LocaleManager locale = plugin.getLocaleManager();

        this.tabCompletions = new TabCompletions(plugin);
        this.mainCommands = ImmutableList.<Command<?>>builder()
                .add(new UserParentCommand(locale))
                .add(new GroupParentCommand(locale))
                .add(new TrackParentCommand(locale))
                .addAll(plugin.getExtraCommands())
                .add(new LogParentCommand(locale))
                .add(new SyncCommand(locale))
                .add(new InfoCommand(locale))
                .add(new EditorCommand(locale))
                .add(new VerboseCommand(locale))
                .add(new TreeCommand(locale))
                .add(new SearchCommand(locale))
                .add(new CheckCommand(locale))
                .add(new NetworkSyncCommand(locale))
                .add(new ImportCommand(locale))
                .add(new ExportCommand(locale))
                .add(new ReloadConfigCommand(locale))
                .add(new BulkUpdateCommand(locale))
                .add(new MigrationParentCommand(locale))
                .add(new ApplyEditsCommand(locale))
                .add(new CreateGroup(locale))
                .add(new DeleteGroup(locale))
                .add(new ListGroups(locale))
                .add(new CreateTrack(locale))
                .add(new DeleteTrack(locale))
                .add(new ListTracks(locale))
                .build()
                .stream()
                .collect(ImmutableCollectors.toMap(c -> c.getName().toLowerCase(), Function.identity()));
    }

    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    public TabCompletions getTabCompletions() {
        return this.tabCompletions;
    }

    public CompletableFuture<CommandResult> executeCommand(Sender sender, String label, List<String> args) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(sender, label, args);
            } catch (Throwable e) {
                this.plugin.getLogger().severe("Exception whilst executing command: " + args.toString());
                e.printStackTrace();
                return null;
            }
        }, this.executor);
    }

    public boolean hasPermissionForAny(Sender sender) {
        return this.mainCommands.values().stream().anyMatch(c -> c.shouldDisplay() && c.isAuthorized(sender));
    }

    private CommandResult execute(Sender sender, String label, List<String> arguments) {
        applyConvenienceAliases(arguments, true);
        if(this.plugin.getConfiguration().get(ConfigKeys.BLOCK_NON_PLAYER_SOURCES)) {
            if(sender.isConsole() || sender.isValid()) {
                Message.NON_PLAYER_COMMAND_ERROR.send(sender);
                return CommandResult.FAILURE;
            }
        }
        // Handle no arguments
        if (arguments.isEmpty() || (arguments.size() == 1 && arguments.get(0).trim().isEmpty())) {
            Message.BLANK.send(sender, "&2Running &b" + AbstractLuckPermsPlugin.getPluginName() + " v" + this.plugin.getBootstrap().getVersion() + "&2.");
            if (hasPermissionForAny(sender)) {
                Message.VIEW_AVAILABLE_COMMANDS_PROMPT.send(sender, label);
                return CommandResult.SUCCESS;
            }

            Collection<? extends Group> groups = this.plugin.getGroupManager().getAll().values();
            if (groups.size() <= 1 && groups.stream().allMatch(g -> g.normalData().isEmpty())) {
                Message.FIRST_TIME_SETUP.send(sender, label, sender.getName());
            } else {
                Message.NO_PERMISSION_FOR_SUBCOMMANDS.send(sender);
            }
            return CommandResult.NO_PERMISSION;
        }

        // Look for the main command.
        Command<?> main = this.mainCommands.get(arguments.get(0).toLowerCase());

        // Main command not found
        if (main == null) {
            sendCommandUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        // Check the Sender has permission to use the main command.
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
            result = main.execute(this.plugin, sender, null, new ArgumentList(arguments), label);
        } catch (CommandException e) {
            result = e.handle(sender, label, main);
        } catch (Throwable e) {
            e.printStackTrace();
            result = CommandResult.FAILURE;
        }

        return result;
    }

    public List<String> tabCompleteCommand(Sender sender, List<String> arguments) {
        applyConvenienceAliases(arguments, false);

        final List<Command<?>> mains = this.mainCommands.values().stream()
                .filter(Command::shouldDisplay)
                .filter(m -> m.isAuthorized(sender))
                .collect(Collectors.toList());

        return TabCompleter.create()
                .at(0, CompletionSupplier.startsWith(() -> mains.stream().map(c -> c.getName().toLowerCase())))
                .from(1, partial -> mains.stream()
                        .filter(m -> m.getName().equalsIgnoreCase(arguments.get(0)))
                        .findFirst()
                        .map(cmd -> cmd.tabComplete(this.plugin, sender, new ArgumentList(arguments.subList(1, arguments.size()))))
                        .orElse(Collections.emptyList())
                )
                .complete(arguments);
    }

    private void sendCommandUsage(Sender sender, String label) {
        Message.BLANK.send(sender, "&2Running &b" + AbstractLuckPermsPlugin.getPluginName() + " v" + this.plugin.getBootstrap().getVersion() + "&2.");
        this.mainCommands.values().stream()
                .filter(Command::shouldDisplay)
                .filter(c -> c.isAuthorized(sender))
                .forEach(c -> {
                    String permission = c.getPermission().map(CommandPermission::getPermission).orElse("None");

                    TextComponent component = TextUtils.fromLegacy("&3> &a" + String.format(c.getUsage(), label), TextUtils.AMPERSAND_CHAR)
                            .toBuilder().applyDeep(comp -> {
                                comp.hoverEvent(HoverEvent.showText(TextUtils.fromLegacy(TextUtils.joinNewline(
                                        "&bCommand: &2" + c.getName(),
                                        "&bDescription: &2" + c.getDescription(),
                                        "&bUsage: &2" + String.format(c.getUsage(), label),
                                        "&bPermission: &2" + permission,
                                        " ",
                                        "&7Click to auto-complete."
                                ), TextUtils.AMPERSAND_CHAR)));
                                comp.clickEvent(ClickEvent.suggestCommand(String.format(c.getUsage(), label)));
                            }).build();
                    sender.sendMessage(component);
                });
    }

    /**
     * Applies "convenience" aliases to the given cmd line arguments.
     *
     * @param args the current args list
     * @param rewriteLastArgument if the last argument should be rewritten - 
     *                            this is false when the method is called on tab completions
     */
    private static void applyConvenienceAliases(List<String> args, boolean rewriteLastArgument) {
        // '/lp u' --> '/lp user' etc
        //      ^           ^^^^
        if (args.size() >= 1 && (rewriteLastArgument || args.size() >= 2)) {
            replaceArgs(args, 0, arg -> {
                switch (arg) {
                    case "u": return "user";
                    case "g": return "group";
                    case "t": return "track";
                    case "i": return "info";
                    default: return null;
                }
            });
        }

        // '/lp user Luck p set --> /lp user Luck permission set' etc
        //                ^                       ^^^^^^^^^^
        if (args.size() >= 3 && (rewriteLastArgument || args.size() >= 4)) {
            String arg0 = args.get(0).toLowerCase();
            if (arg0.equals("user") || arg0.equals("group")) {
                replaceArgs(args, 2, arg -> {
                    switch (arg) {
                        case "p":
                        case "perm":
                            return "permission";
                        case "g":
                        case "group":
                            return "parent";
                        case "m": return "meta";
                        case "i": return "info";
                        case "e": return "editor";
                        default: return null;
                    }
                });

                // '/lp user Luck permission i' --> '/lp user Luck permission info' etc
                //                           ^                                ^^^^
                if (args.size() >= 4 && (rewriteLastArgument || args.size() >= 5)) {
                    String arg2 = args.get(2).toLowerCase();
                    if (arg2.equals("permission") || arg2.equals("parent") || arg2.equals("meta")) {
                        replaceArgs(args, 3, arg -> arg.equals("i") ? "info" : null);
                    }
                }
            }
        }
    }

    private static void replaceArgs(List<String> args, int i, Function<String, String> rewrites) {
        String arg = args.get(i).toLowerCase();
        String rewrite = rewrites.apply(arg);
        if (rewrite != null) {
            args.remove(i);
            args.add(i, rewrite);
        }
    }

}
