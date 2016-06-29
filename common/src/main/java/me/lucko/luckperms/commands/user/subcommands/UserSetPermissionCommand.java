package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserSetPermissionCommand extends UserSubCommand {
    public UserSetPermissionCommand() {
        super("set", "Sets a permission for a user",
                "/perms user <user> set <node> <true|false> [server]", "luckperms.user.setpermission");
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args) {
        String node = args.get(0);
        String bool = args.get(1);

        if (node.contains("/")) {
            sendUsage(sender);
            return;
        }

        if (node.matches("group\\..*")) {
            Util.sendPluginMessage(sender, "Use the addgroup command instead of specifying the node.");
            return;
        }

        if (!bool.equalsIgnoreCase("true") && !bool.equalsIgnoreCase("false")) {
            sendUsage(sender);
            return;
        }

        boolean b = Boolean.parseBoolean(bool);

        try {
            if (args.size() == 3) {
                final String server = args.get(2).toLowerCase();
                user.setPermission(node, b, server);
                Util.sendPluginMessage(sender, "&aSet &b" + node + "&a to " + bool + " for &b" + user.getName() + "&a on server &b" + server + "&a.");
            } else {
                user.setPermission(node, b);
                Util.sendPluginMessage(sender, "&aSet &b" + node + "&a to " + bool + " for &b" + user.getName() + "&a.");
            }
        } catch (ObjectAlreadyHasException e) {
            Util.sendPluginMessage(sender, "That user already has this permission!");
        }

        saveUser(user, sender, plugin);
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength < 2;
    }
}
