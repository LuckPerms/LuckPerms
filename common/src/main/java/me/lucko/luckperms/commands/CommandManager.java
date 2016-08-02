package me.lucko.luckperms.commands;

import lombok.Getter;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.group.CreateGroup;
import me.lucko.luckperms.commands.group.DeleteGroup;
import me.lucko.luckperms.commands.group.GroupMainCommand;
import me.lucko.luckperms.commands.group.ListGroups;
import me.lucko.luckperms.commands.group.subcommands.*;
import me.lucko.luckperms.commands.misc.DebugCommand;
import me.lucko.luckperms.commands.misc.InfoCommand;
import me.lucko.luckperms.commands.misc.SyncCommand;
import me.lucko.luckperms.commands.track.CreateTrack;
import me.lucko.luckperms.commands.track.DeleteTrack;
import me.lucko.luckperms.commands.track.ListTracks;
import me.lucko.luckperms.commands.track.TrackMainCommand;
import me.lucko.luckperms.commands.track.subcommands.*;
import me.lucko.luckperms.commands.user.UserMainCommand;
import me.lucko.luckperms.commands.user.subcommands.*;
import me.lucko.luckperms.constants.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CommandManager {
    private final LuckPermsPlugin plugin;

    @Getter
    private final List<MainCommand> mainCommands = new ArrayList<>();

    public CommandManager(LuckPermsPlugin plugin) {
        this.plugin = plugin;

        UserMainCommand userCommand = new UserMainCommand();
        this.registerMainCommand(userCommand);
        userCommand.registerSubCommand(new UserInfo());
        userCommand.registerSubCommand(new UserGetUUID());
        userCommand.registerSubCommand(new UserListNodes());
        userCommand.registerSubCommand(new UserHasPerm());
        userCommand.registerSubCommand(new UserInheritsPerm());
        userCommand.registerSubCommand(new UserSetPermission());
        userCommand.registerSubCommand(new UserUnSetPermission());
        userCommand.registerSubCommand(new UserAddGroup());
        userCommand.registerSubCommand(new UserRemoveGroup());
        userCommand.registerSubCommand(new UserSetTempPermission());
        userCommand.registerSubCommand(new UserUnsetTempPermission());
        userCommand.registerSubCommand(new UserAddTempGroup());
        userCommand.registerSubCommand(new UserRemoveTempGroup());
        userCommand.registerSubCommand(new UserSetPrimaryGroup());
        userCommand.registerSubCommand(new UserShowTracks());
        userCommand.registerSubCommand(new UserPromote());
        userCommand.registerSubCommand(new UserDemote());
        userCommand.registerSubCommand(new UserShowPos());
        userCommand.registerSubCommand(new UserClear());

        GroupMainCommand groupCommand = new GroupMainCommand();
        this.registerMainCommand(groupCommand);
        groupCommand.registerSubCommand(new GroupInfo());
        groupCommand.registerSubCommand(new GroupListNodes());
        groupCommand.registerSubCommand(new GroupHasPerm());
        groupCommand.registerSubCommand(new GroupInheritsPerm());
        groupCommand.registerSubCommand(new GroupSetPermission());
        groupCommand.registerSubCommand(new GroupUnSetPermission());
        groupCommand.registerSubCommand(new GroupSetInherit());
        groupCommand.registerSubCommand(new GroupUnsetInherit());
        groupCommand.registerSubCommand(new GroupSetTempPermission());
        groupCommand.registerSubCommand(new GroupUnsetTempPermission());
        groupCommand.registerSubCommand(new GroupSetTempInherit());
        groupCommand.registerSubCommand(new GroupUnsetTempInherit());
        groupCommand.registerSubCommand(new GroupShowTracks());
        groupCommand.registerSubCommand(new GroupClear());

        TrackMainCommand trackCommand = new TrackMainCommand();
        this.registerMainCommand(trackCommand);
        trackCommand.registerSubCommand(new TrackInfo());
        trackCommand.registerSubCommand(new TrackAppend());
        trackCommand.registerSubCommand(new TrackInsert());
        trackCommand.registerSubCommand(new TrackRemove());
        trackCommand.registerSubCommand(new TrackClear());

        this.registerMainCommand(new SyncCommand());
        this.registerMainCommand(new InfoCommand());
        this.registerMainCommand(new DebugCommand());
        this.registerMainCommand(new CreateGroup());
        this.registerMainCommand(new DeleteGroup());
        this.registerMainCommand(new ListGroups());
        this.registerMainCommand(new CreateTrack());
        this.registerMainCommand(new DeleteTrack());
        this.registerMainCommand(new ListTracks());
    }

    /**
     * Generic on command method to be called from the command executor object of the platform
     * @param sender who sent the command
     * @param label the command label used
     * @param args the arguments provided
     * @return if the command was successful
     */
    public boolean onCommand(Sender sender, String label, List<String> args) {
        if (args.size() == 0) {
            sendCommandUsage(sender, label);
            return true;
        }

        Optional<MainCommand> o = mainCommands.stream().filter(m -> m.getName().equalsIgnoreCase(args.get(0))).limit(1).findAny();

        if (!o.isPresent()) {
            sendCommandUsage(sender, label);
            return true;
        }

        final MainCommand main = o.get();
        if (!main.isAuthorized(sender)) {
            sendCommandUsage(sender, label);
            return true;
        }

        if (main.getRequiredArgsLength() == 0) {
            main.execute(plugin, sender, null, label);
            return true;
        }

        if (args.size() == 1) {
            main.sendUsage(sender, label);
            return true;
        }

        main.execute(plugin, sender, new ArrayList<>(args.subList(1, args.size())), label);
        return true;

    }

    /**
     * Generic tab complete method to be called from the command executor object of the platform
     * @param sender who is tab completing
     * @param args the arguments provided so far
     * @return a list of suggestions
     */
    @SuppressWarnings("unchecked")
    public List<String> onTabComplete(Sender sender, List<String> args) {
        final List<MainCommand> mains = mainCommands.stream().filter(m -> m.isAuthorized(sender)).collect(Collectors.toList());

        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return mains.stream().map(MainCommand::getName).map(String::toLowerCase).collect(Collectors.toList());
            }

            return mains.stream().map(MainCommand::getName).map(String::toLowerCase)
                    .filter(s -> s.startsWith(args.get(0).toLowerCase())).collect(Collectors.toList());
        }

        Optional<MainCommand> o = mains.stream().filter(m -> m.getName().equalsIgnoreCase(args.get(0))).limit(1).findAny();
        if (!o.isPresent()) {
            return Collections.emptyList();
        }

        return o.get().onTabComplete(sender, args.subList(1, args.size()), plugin);
    }

    private void registerMainCommand(MainCommand command) {
        plugin.getLogger().log(Level.INFO, "[CommandManager] Registered main command '" + command.getName() + "'");
        mainCommands.add(command);
    }

    private void sendCommandUsage(Sender sender, String label) {
        Message.INFO_BRIEF.send(sender, plugin.getVersion());

        mainCommands.stream()
                .filter(c -> c.isAuthorized(sender))
                .forEach(c -> Util.sendPluginMessage(sender, "&e-> &d" + String.format(c.getUsage(), label)));
    }
}
