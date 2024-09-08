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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.lucko.luckperms.common.command.abstraction.Command;
import me.lucko.luckperms.common.command.abstraction.CommandException;
import me.lucko.luckperms.common.command.tabcomplete.CompletionSupplier;
import me.lucko.luckperms.common.command.tabcomplete.TabCompleter;
import me.lucko.luckperms.common.command.tabcomplete.TabCompletions;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.commands.group.CreateGroup;
import me.lucko.luckperms.common.commands.group.DeleteGroup;
import me.lucko.luckperms.common.commands.group.GroupParentCommand;
import me.lucko.luckperms.common.commands.group.ListGroups;
import me.lucko.luckperms.common.commands.log.LogParentCommand;
import me.lucko.luckperms.common.commands.misc.ApplyEditsCommand;
import me.lucko.luckperms.common.commands.misc.BulkUpdateCommand;
import me.lucko.luckperms.common.commands.misc.EditorCommand;
import me.lucko.luckperms.common.commands.misc.ExportCommand;
import me.lucko.luckperms.common.commands.misc.ImportCommand;
import me.lucko.luckperms.common.commands.misc.InfoCommand;
import me.lucko.luckperms.common.commands.misc.NetworkSyncCommand;
import me.lucko.luckperms.common.commands.misc.ReloadConfigCommand;
import me.lucko.luckperms.common.commands.misc.SearchCommand;
import me.lucko.luckperms.common.commands.misc.SyncCommand;
import me.lucko.luckperms.common.commands.misc.TranslationsCommand;
import me.lucko.luckperms.common.commands.misc.TreeCommand;
import me.lucko.luckperms.common.commands.misc.TrustEditorCommand;
import me.lucko.luckperms.common.commands.misc.VerboseCommand;
import me.lucko.luckperms.common.commands.track.CreateTrack;
import me.lucko.luckperms.common.commands.track.DeleteTrack;
import me.lucko.luckperms.common.commands.track.ListTracks;
import me.lucko.luckperms.common.commands.track.TrackParentCommand;
import me.lucko.luckperms.common.commands.user.UserParentCommand;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.ExpiringSet;
import me.lucko.luckperms.common.util.ImmutableCollectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Root command manager for the '/luckperms' command.
 */
public class CommandManager {

    private final LuckPermsPlugin plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("luckperms-command-executor")
            .build()
    );
    private final AtomicBoolean executingCommand = new AtomicBoolean(false);
    private final Set<UUID> playerRateLimit = ExpiringSet.newExpiringSet(500, TimeUnit.MILLISECONDS);
    private final TabCompletions tabCompletions;
    private final Map<String, Command<?>> mainCommands;

    public CommandManager(LuckPermsPlugin plugin) {
        this.plugin = plugin;
        this.tabCompletions = new TabCompletions(plugin);
        this.mainCommands = ImmutableList.<Command<?>>builder()
                .add(new UserParentCommand())
                .add(new GroupParentCommand())
                .add(new TrackParentCommand())
                .addAll(plugin.getExtraCommands())
                .add(new LogParentCommand())
                .add(new SyncCommand())
                .add(new InfoCommand())
                .add(new EditorCommand())
                .add(new VerboseCommand())
                .add(new TreeCommand())
                .add(new SearchCommand())
                .add(new NetworkSyncCommand())
                .add(new ImportCommand())
                .add(new ExportCommand())
                .add(new ReloadConfigCommand())
                .add(new BulkUpdateCommand())
                .add(new TranslationsCommand())
                .add(new ApplyEditsCommand())
                .add(new TrustEditorCommand())
                .add(new CreateGroup())
                .add(new DeleteGroup())
                .add(new ListGroups())
                .add(new CreateTrack())
                .add(new DeleteTrack())
                .add(new ListTracks())
                .build()
                .stream()
                .collect(ImmutableCollectors.toMap(c -> c.getName().toLowerCase(Locale.ROOT), Function.identity()));
    }

    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    public TabCompletions getTabCompletions() {
        return this.tabCompletions;
    }

    @VisibleForTesting
    public Map<String, Command<?>> getMainCommands() {
        return this.mainCommands;
    }

    public CompletableFuture<Void> executeCommand(Sender sender, String label, List<String> args) {
        UUID uniqueId = sender.getUniqueId();
        if (this.plugin.getConfiguration().get(ConfigKeys.COMMANDS_RATE_LIMIT) && !sender.isConsole() && !this.playerRateLimit.add(uniqueId)) {
            this.plugin.getLogger().warn("Player '" + uniqueId + "' is spamming LuckPerms commands. Ignoring further inputs.");
            return CompletableFuture.completedFuture(null);
        }

        SchedulerAdapter scheduler = this.plugin.getBootstrap().getScheduler();
        List<String> argsCopy = new ArrayList<>(args);

        // if the executingCommand flag is set, there is another command executing at the moment
        if (this.executingCommand.get()) {
            Message.ALREADY_EXECUTING_COMMAND.send(sender);
        }

        // a reference to the thread being used to execute the command
        AtomicReference<Thread> executorThread = new AtomicReference<>();
        // a reference to the timeout task scheduled to catch if this command takes too long to execute
        AtomicReference<SchedulerTask> timeoutTask = new AtomicReference<>();

        // schedule the actual execution of the command using the command executor service
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            // set flags
            executorThread.set(Thread.currentThread());
            this.executingCommand.set(true);

            // actually try to execute the command
            try {
                execute(sender, label, args);
            } catch (Throwable e) {
                // catch any exception
                this.plugin.getLogger().severe("Exception whilst executing command: " + args, e);
            } finally {
                // unset flags
                this.executingCommand.set(false);
                executorThread.set(null);

                // cancel the timeout task
                SchedulerTask timeout;
                if ((timeout = timeoutTask.get()) != null) {
                    timeout.cancel();
                }
            }
        }, this.executor);

        // schedule another task to catch if the command doesn't complete after 10 seconds
        timeoutTask.set(scheduler.asyncLater(() -> {
            if (!future.isDone()) {
                handleCommandTimeout(executorThread, argsCopy);
            }
        }, 10, TimeUnit.SECONDS));

        return future;
    }

    private void handleCommandTimeout(AtomicReference<Thread> thread, List<String> args) {
        Thread executorThread = thread.get();
        if (executorThread == null) {
            this.plugin.getLogger().warn("Command execution " + args + " has not completed - is another command execution blocking it?");
        } else {
            String stackTrace = Arrays.stream(executorThread.getStackTrace())
                    .map(el -> "  " + el.toString())
                    .collect(Collectors.joining("\n"));
            this.plugin.getLogger().warn("Command execution " + args + " has not completed. Trace: \n" + stackTrace);
        }
    }

    public boolean hasPermissionForAny(Sender sender) {
        return this.mainCommands.values().stream().anyMatch(c -> c.shouldDisplay() && c.isAuthorized(sender));
    }

    private void execute(Sender sender, String label, List<String> arguments) {
        applyConvenienceAliases(arguments, true);

        // Handle no arguments
        if (arguments.isEmpty() || arguments.size() == 1 && arguments.get(0).trim().isEmpty()) {
            sender.sendMessage(Message.prefixed(Component.text()
                    .color(NamedTextColor.DARK_GREEN)
                    .append(Component.text("Running "))
                    .append(Component.text(AbstractLuckPermsPlugin.getPluginName(), NamedTextColor.AQUA))
                    .append(Component.space())
                    .append(Component.text("v" + this.plugin.getBootstrap().getVersion(), NamedTextColor.AQUA))
                    .append(Message.FULL_STOP)
            ));

            if (hasPermissionForAny(sender)) {
                Message.VIEW_AVAILABLE_COMMANDS_PROMPT.send(sender, label);
                return;
            }

            Collection<? extends Group> groups = this.plugin.getGroupManager().getAll().values();
            if (groups.size() <= 1 && groups.stream().allMatch(g -> g.normalData().isEmpty())) {
                Message.FIRST_TIME_SETUP.send(sender, label, sender.getName());
            } else {
                Message.NO_PERMISSION_FOR_SUBCOMMANDS.send(sender);
            }
            return;
        }

        // Look for the main command.
        Command<?> main = this.mainCommands.get(arguments.get(0).toLowerCase(Locale.ROOT));

        // Main command not found
        if (main == null) {
            sendCommandUsage(sender, label);
            return;
        }

        // Check the Sender has permission to use the main command.
        if (!main.isAuthorized(sender)) {
            sendCommandUsage(sender, label);
            return;
        }

        arguments.remove(0); // remove the main command arg.

        // Check the correct number of args were given for the main command
        if (main.getArgumentCheck().test(arguments.size())) {
            main.sendDetailedUsage(sender, label);
            return;
        }

        // Try to execute the command.
        try {
            main.execute(this.plugin, sender, null, new ArgumentList(arguments), label);
        } catch (CommandException e) {
            e.handle(sender, label, main);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public List<String> tabCompleteCommand(Sender sender, List<String> arguments) {
        applyConvenienceAliases(arguments, false);

        final List<Command<?>> mains = this.mainCommands.values().stream()
                .filter(Command::shouldDisplay)
                .filter(m -> m.isAuthorized(sender))
                .collect(Collectors.toList());

        return TabCompleter.create()
                .at(0, CompletionSupplier.startsWith(() -> mains.stream().map(c -> c.getName().toLowerCase(Locale.ROOT))))
                .from(1, partial -> mains.stream()
                        .filter(m -> m.getName().equalsIgnoreCase(arguments.get(0)))
                        .findFirst()
                        .map(cmd -> cmd.tabComplete(this.plugin, sender, new ArgumentList(arguments.subList(1, arguments.size()))))
                        .orElse(Collections.emptyList())
                )
                .complete(arguments);
    }

    private void sendCommandUsage(Sender sender, String label) {
        sender.sendMessage(Message.prefixed(Component.text()
                .color(NamedTextColor.DARK_GREEN)
                .append(Component.text("Running "))
                .append(Component.text(AbstractLuckPermsPlugin.getPluginName(), NamedTextColor.AQUA))
                .append(Component.space())
                .append(Component.text("v" + this.plugin.getBootstrap().getVersion(), NamedTextColor.AQUA))
                .append(Message.FULL_STOP)
        ));

        this.mainCommands.values().stream()
                .filter(Command::shouldDisplay)
                .filter(c -> c.isAuthorized(sender))
                .forEach(c -> sender.sendMessage(Component.text()
                        .append(Component.text('>', NamedTextColor.DARK_AQUA))
                        .append(Component.space())
                        .append(Component.text(String.format(c.getUsage(), label), NamedTextColor.GREEN))
                        .clickEvent(ClickEvent.suggestCommand(String.format(c.getUsage(), label)))
                        .build()
                ));
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
            String arg0 = args.get(0).toLowerCase(Locale.ROOT);
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
                    String arg2 = args.get(2).toLowerCase(Locale.ROOT);
                    if (arg2.equals("permission") || arg2.equals("parent") || arg2.equals("meta")) {
                        replaceArgs(args, 3, arg -> arg.equals("i") ? "info" : null);
                    }
                }
            }
        }
    }

    private static void replaceArgs(List<String> args, int i, Function<String, String> rewrites) {
        String arg = args.get(i).toLowerCase(Locale.ROOT);
        String rewrite = rewrites.apply(arg);
        if (rewrite != null) {
            args.remove(i);
            args.add(i, rewrite);
        }
    }

}
