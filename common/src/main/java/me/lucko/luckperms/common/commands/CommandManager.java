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

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.group.CreateGroup;
import me.lucko.luckperms.common.commands.group.DeleteGroup;
import me.lucko.luckperms.common.commands.group.GroupMainCommand;
import me.lucko.luckperms.common.commands.group.ListGroups;
import me.lucko.luckperms.common.commands.log.LogMainCommand;
import me.lucko.luckperms.common.commands.migration.MigrationMainCommand;
import me.lucko.luckperms.common.commands.misc.*;
import me.lucko.luckperms.common.commands.track.CreateTrack;
import me.lucko.luckperms.common.commands.track.DeleteTrack;
import me.lucko.luckperms.common.commands.track.ListTracks;
import me.lucko.luckperms.common.commands.track.TrackMainCommand;
import me.lucko.luckperms.common.commands.user.UserMainCommand;
import me.lucko.luckperms.common.commands.usersbulkedit.UsersBulkEditMainCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
public class CommandManager {
    @Getter
    private final LuckPermsPlugin plugin;

    @Getter
    private final List<MainCommand> mainCommands = ImmutableList.<MainCommand>builder()
            .add(new UserMainCommand())
            .add(new GroupMainCommand())
            .add(new TrackMainCommand())
            .add(new LogMainCommand())
            .add(new SyncCommand())
            .add(new NetworkSyncCommand())
            .add(new InfoCommand())
            .add(new DebugCommand())
            .add(new VerboseCommand())
            .add(new ImportCommand())
            .add(new ExportCommand())
            .add(new QueueCommand())
            .add(new MigrationMainCommand())
            .add(new UsersBulkEditMainCommand())
            .add(new CreateGroup())
            .add(new DeleteGroup())
            .add(new ListGroups())
            .add(new CreateTrack())
            .add(new DeleteTrack())
            .add(new ListTracks())
            .build();


    /**
     * Generic on command method to be called from the command executor object of the platform
     * Unlike {@link #onCommand(Sender, String, List)}, this method is called in a new thread
     * @param sender who sent the command
     * @param label the command label used
     * @param args the arguments provided
     * @param result the callback to be called when the command has fully executed
     */
    public void onCommand(Sender sender, String label, List<String> args, Callback<CommandResult> result) {
        plugin.doAsync(() -> {
            CommandResult r = onCommand(sender, label, args);
            plugin.doSync(() -> result.onComplete(r));
        });
    }

    /**
     * Generic on command method to be called from the command executor object of the platform
     * @param sender who sent the command
     * @param label the command label used
     * @param args the arguments provided
     * @return if the command was successful
     */
    public CommandResult onCommand(Sender sender, String label, List<String> args) {
        if (args.size() == 0) {
            sendCommandUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        Optional<MainCommand> o = mainCommands.stream()
                .filter(m -> m.getName().equalsIgnoreCase(args.get(0)))
                .limit(1)
                .findAny();

        if (!o.isPresent()) {
            sendCommandUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        final MainCommand main = o.get();
        if (!main.isAuthorized(sender)) {
            sendCommandUsage(sender, label);
            return CommandResult.NO_PERMISSION;
        }

        if (main.getRequiredArgsLength() == 0) {
            try {
                return main.execute(plugin, sender, null, label);
            } catch (Exception e) {
                e.printStackTrace();
                return CommandResult.FAILURE;
            }
        }

        if (args.size() == 1) {
            main.sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        List<String> arguments = new ArrayList<>(args);
        handleRewrites(arguments);

        try {
            return main.execute(plugin, sender, arguments.subList(1, arguments.size()), label);
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.FAILURE;
        }
    }

    /**
     * Generic tab complete method to be called from the command executor object of the platform
     * @param sender who is tab completing
     * @param args the arguments provided so far
     * @return a list of suggestions
     */
    @SuppressWarnings("unchecked")
    public List<String> onTabComplete(Sender sender, List<String> args) {
        final List<MainCommand> mains = mainCommands.stream()
                .filter(m -> m.isAuthorized(sender))
                .collect(Collectors.toList());

        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return mains.stream()
                        .map(m -> m.getName().toLowerCase())
                        .collect(Collectors.toList());
            }

            return mains.stream()
                    .map(m -> m.getName().toLowerCase())
                    .filter(s -> s.startsWith(args.get(0).toLowerCase()))
                    .collect(Collectors.toList());
        }

        Optional<MainCommand> o = mains.stream()
                .filter(m -> m.getName().equalsIgnoreCase(args.get(0)))
                .limit(1)
                .findAny();

        if (!o.isPresent()) {
            return Collections.emptyList();
        }

        return o.get().onTabComplete(sender, args.subList(1, args.size()), plugin);
    }

    private void sendCommandUsage(Sender sender, String label) {
        Util.sendPluginMessage(sender, "&2Running &bLuckPerms v" + plugin.getVersion() + "&2.");
        mainCommands.stream()
                .filter(c -> c.isAuthorized(sender))
                .forEach(c -> Util.sendPluginMessage(sender, "&3> &a" + String.format(c.getUsage(), label)));
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
                case "m":
                    args.remove(2);
                    args.add(2, "meta");
                    break;
                case "i":
                case "inherit":
                case "inheritances":
                case "group":
                case "rank":
                    args.remove(2);
                    args.add(2, "parent");
                    break;

                // Provide backwards compatibility
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
            }
        }
    }
}
