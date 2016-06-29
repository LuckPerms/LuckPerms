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
import me.lucko.luckperms.commands.user.UserMainCommand;
import me.lucko.luckperms.commands.user.subcommands.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

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
        userCommand.registerSubCommand(new UserGetUUIDCommand());
        userCommand.registerSubCommand(new UserHasPermCommand());
        userCommand.registerSubCommand(new UserInfoCommand());
        userCommand.registerSubCommand(new UserListNodesCommand());
        userCommand.registerSubCommand(new UserRemoveGroupCommand());
        userCommand.registerSubCommand(new UserSetPermissionCommand());
        userCommand.registerSubCommand(new UserSetPrimaryGroupCommand());
        userCommand.registerSubCommand(new UserUnSetPermissionCommand());

        GroupMainCommand groupCommand = new GroupMainCommand();
        this.registerMainCommand(groupCommand);
        groupCommand.registerSubCommand(new GroupClearCommand());
        groupCommand.registerSubCommand(new GroupHasPermCommand());
        groupCommand.registerSubCommand(new GroupInfoCommand());
        groupCommand.registerSubCommand(new GroupListNodesCommand());
        groupCommand.registerSubCommand(new GroupSetInheritCommand());
        groupCommand.registerSubCommand(new GroupSetPermissionCommand());
        groupCommand.registerSubCommand(new GroupUnsetInheritCommand());
        groupCommand.registerSubCommand(new GroupUnSetPermissionCommand());

        this.registerMainCommand(new CreateGroupCommand());
        this.registerMainCommand(new DeleteGroupCommand());
        this.registerMainCommand(new ListGroupsCommand());
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

    private void registerMainCommand(MainCommand command) {
        plugin.getLogger().log(Level.INFO, "[CommandManager] Registered main command '" + command.getName() + "'");
        mainCommands.add(command);
    }

    private void sendCommandUsage(Sender sender) {
        Util.sendPluginMessage(sender, "&6Running &bLuckPerms " + plugin.getVersion() + "&6.");

        mainCommands.stream()
                .filter(c -> c.canUse(sender))
                .forEach(c -> Util.sendPluginMessage(sender, "&e-> &d" + c.getUsage()));
    }
}
