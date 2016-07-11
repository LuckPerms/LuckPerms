package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.constants.Messages;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserSetPrimaryGroupCommand extends UserSubCommand {
    public UserSetPrimaryGroupCommand() {
        super("setprimarygroup", "Sets a users primary group",
                "/perms user <user> setprimarygroup <group>", Permission.USER_SETPRIMARYGROUP);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args) {
        Group group = plugin.getGroupManager().getGroup(args.get(0).toLowerCase());
        if (group == null) {
            Messages.GROUP_DOES_NOT_EXIST.send(sender);
            return;
        }

        if (user.getPrimaryGroup().equalsIgnoreCase(group.getName())) {
            Messages.USER_PRIMARYGROUP_ERROR_ALREADYHAS.send(sender);
            return;
        }

        if (!user.isInGroup(group)) {
            Messages.USER_PRIMARYGROUP_ERROR_NOTMEMBER.send(sender);
            return;
        }

        user.setPrimaryGroup(group.getName());
        Messages.USER_PRIMARYGROUP_SUCCESS.send(sender, user.getName(), group.getName());

        saveUser(user, sender, plugin);
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength != 1;
    }
}
