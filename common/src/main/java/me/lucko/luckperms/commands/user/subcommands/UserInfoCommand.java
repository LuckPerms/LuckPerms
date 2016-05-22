package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserInfoCommand extends UserSubCommand {
    public UserInfoCommand() {
        super("info", "Gives info about the user",
                "/perms user <user> info", "luckperms.user.info");
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args) {
        final String prefix = plugin.getConfiguration().getPrefix();
        String sb = prefix + "&d-> &eUser: &6" + user.getName() + "\n" +
                prefix + "&d-> &eUUID: &6" + user.getUuid() + "\n" +
                prefix + "&d-> &eStatus: " + plugin.getPlayerStatus(user.getUuid()) + "\n" +
                prefix + "&d-> &eGroups: &6" + Util.listToCommaSep(user.getGroupNames()) + "\n" +
                prefix + "&d-> &ePermissions: &6" + (user.getNodes().keySet().size() - user.getGroupNames().size()) + "\n" +
                prefix + "&d-> &bUse &a/perms user " + user.getName() + " listnodes &bto see all permissions.";

        sender.sendMessage(Util.color(sb));
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return false;
    }
}
