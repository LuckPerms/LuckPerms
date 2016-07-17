package me.lucko.luckperms.commands;

import lombok.Getter;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.group.CreateGroupCommand;
import me.lucko.luckperms.commands.group.DeleteGroupCommand;
import me.lucko.luckperms.commands.group.GroupMainCommand;
import me.lucko.luckperms.commands.group.ListGroupsCommand;
import me.lucko.luckperms.commands.group.subcommands.*;
import me.lucko.luckperms.commands.misc.DebugCommand;
import me.lucko.luckperms.commands.misc.InfoCommand;
import me.lucko.luckperms.commands.misc.SyncCommand;
import me.lucko.luckperms.commands.track.CreateTrackCommand;
import me.lucko.luckperms.commands.track.DeleteTrackCommand;
import me.lucko.luckperms.commands.track.ListTracksCommand;
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
        userCommand.registerSubCommand(new UserAddGroupCommand());
        userCommand.registerSubCommand(new UserClearCommand());
        userCommand.registerSubCommand(new UserDemoteCommand());
        userCommand.registerSubCommand(new UserGetUUIDCommand());
        userCommand.registerSubCommand(new UserHasPermCommand());
        userCommand.registerSubCommand(new UserInfoCommand());
        userCommand.registerSubCommand(new UserInheritsPermCommand());
        userCommand.registerSubCommand(new UserListNodesCommand());
        userCommand.registerSubCommand(new UserPromoteCommand());
        userCommand.registerSubCommand(new UserRemoveGroupCommand());
        userCommand.registerSubCommand(new UserSetPermissionCommand());
        userCommand.registerSubCommand(new UserSetPrimaryGroupCommand());
        userCommand.registerSubCommand(new UserShowPosCommand());
        userCommand.registerSubCommand(new UserShowTracksCommand());
        userCommand.registerSubCommand(new UserUnSetPermissionCommand());

        GroupMainCommand groupCommand = new GroupMainCommand();
        this.registerMainCommand(groupCommand);
        groupCommand.registerSubCommand(new GroupClearCommand());
        groupCommand.registerSubCommand(new GroupHasPermCommand());
        groupCommand.registerSubCommand(new GroupInfoCommand());
        groupCommand.registerSubCommand(new GroupInheritsPermCommand());
        groupCommand.registerSubCommand(new GroupListNodesCommand());
        groupCommand.registerSubCommand(new GroupSetInheritCommand());
        groupCommand.registerSubCommand(new GroupSetPermissionCommand());
        groupCommand.registerSubCommand(new GroupShowTracksCommand());
        groupCommand.registerSubCommand(new GroupUnsetInheritCommand());
        groupCommand.registerSubCommand(new GroupUnSetPermissionCommand());

        TrackMainCommand trackCommand = new TrackMainCommand();
        this.registerMainCommand(trackCommand);
        trackCommand.registerSubCommand(new TrackAppendCommand());
        trackCommand.registerSubCommand(new TrackClearCommand());
        trackCommand.registerSubCommand(new TrackInfoCommand());
        trackCommand.registerSubCommand(new TrackInsertCommand());
        trackCommand.registerSubCommand(new TrackRemoveCommand());

        this.registerMainCommand(new CreateGroupCommand());
        this.registerMainCommand(new DeleteGroupCommand());
        this.registerMainCommand(new ListGroupsCommand());
        this.registerMainCommand(new CreateTrackCommand());
        this.registerMainCommand(new DeleteTrackCommand());
        this.registerMainCommand(new ListTracksCommand());
        this.registerMainCommand(new DebugCommand());
        this.registerMainCommand(new InfoCommand());
        this.registerMainCommand(new SyncCommand());
    }

    /**
     * Generic on command method to be called from the command executor object of the platform
     * @param sender who sent the command
     * @param args the arguments provided
     * @return if the command was successful (hint: it always is :> )
     */
    public boolean onCommand(Sender sender, List<String> args) {
        if (args.size() == 0) {
            sendCommandUsage(sender);
            return true;
        }

        Optional<MainCommand> o = mainCommands.stream().filter(m -> m.getName().equalsIgnoreCase(args.get(0))).limit(1).findAny();

        if (!o.isPresent()) {
            sendCommandUsage(sender);
            return true;
        }

        final MainCommand main = o.get();
        if (!main.canUse(sender)) {
            sendCommandUsage(sender);
            return true;
        }

        if (main.getRequiredArgsLength() == 0) {
            main.execute(plugin, sender, null);
            return true;
        }

        if (args.size() == 1) {
            main.sendUsage(sender);
            return true;
        }

        main.execute(plugin, sender, new ArrayList<>(args.subList(1, args.size())));
        return true;

    }

    /**
     * Generic tab complete method to be called from the command executor object of the platform
     * @param sender who is tab completing
     * @param args the arguments provided so far
     * @return a list of suggestions
     */
    public List<String> onTabComplete(Sender sender, List<String> args) {
        final List<MainCommand> mains = mainCommands.stream().filter(m -> m.canUse(sender)).collect(Collectors.toList());

        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return mains.stream().map(MainCommand::getName).map(String::toLowerCase).collect(Collectors.toList());
            }

            return mains.stream().map(MainCommand::getName).map(String::toLowerCase).filter(s -> s.startsWith(args.get(0).toLowerCase())).collect(Collectors.toList());
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

    private void sendCommandUsage(Sender sender) {
        Message.INFO_BRIEF.send(sender, plugin.getVersion());

        mainCommands.stream()
                .filter(c -> c.canUse(sender))
                .forEach(c -> Util.sendPluginMessage(sender, "&e-> &d" + c.getUsage()));
    }
}
