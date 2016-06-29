package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Permission;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.user.UserSubCommand;
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
            Util.sendPluginMessage(sender, "That group does not exist!");
            return;
        }

        if (user.getPrimaryGroup().equalsIgnoreCase(group.getName())) {
            Util.sendPluginMessage(sender, "The user already has this group set as their primary group.");
            return;
        }

        if (!user.isInGroup(group)) {
            Util.sendPluginMessage(sender, "The user must be a member of the group first! Use &4/perms user <user> addgroup <group>");
            return;
        }

        user.setPrimaryGroup(group.getName());
        Util.sendPluginMessage(sender, "&b" + user.getName() + "&a's primary group was set to &b" + group.getName() + "&a.");

        saveUser(user, sender, plugin);
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength != 1;
    }
}
