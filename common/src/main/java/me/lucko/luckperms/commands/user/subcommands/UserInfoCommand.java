package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Permission;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserInfoCommand extends UserSubCommand {
    public UserInfoCommand() {
        super("info", "Gives info about the user", "/perms user <user> info", Permission.USER_INFO);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args) {
        sender.sendMessage(Util.color(
                Util.PREFIX + "&d-> &eUser: &6" + user.getName() + "\n" +
                Util.PREFIX + "&d-> &eUUID: &6" + user.getUuid() + "\n" +
                Util.PREFIX + "&d-> &eStatus: " + plugin.getPlayerStatus(user.getUuid()) + "\n" +
                Util.PREFIX + "&d-> &eGroups: &6" + Util.listToCommaSep(user.getGroupNames()) + "\n" +
                Util.PREFIX + "&d-> &ePrimary Group: &6" + user.getPrimaryGroup() + "\n" +
                Util.PREFIX + "&d-> &ePermissions: &6" + (user.getNodes().keySet().size() - user.getGroupNames().size()) + "\n" +
                Util.PREFIX + "&d-> &bUse &a/perms user " + user.getName() + " listnodes &bto see all permissions."
        ));
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return false;
    }
}
