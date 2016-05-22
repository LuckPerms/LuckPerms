package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserAddGroupCommand extends UserSubCommand {
    public UserAddGroupCommand() {
        super("addgroup", "Adds the user to a group",
                "/perms user <user> addgroup <group> [server]", "luckperms.user.addgroup");
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args) {
        String group = args.get(0).toLowerCase();

        String server;
        if (args.size() != 1) {
            server = args.get(1);
        } else {
            server = "global";
        }

        Group group1 = plugin.getGroupManager().getGroup(group);
        if (group1 == null) {
            Util.sendPluginMessage(sender, "That group does not exist!");
            return;
        }

        try {
            user.addGroup(group1, server);
            Util.sendPluginMessage(sender, "&b" + user.getName() + "&a successfully added to group &b" + group + "&a on the server &b" + server + "&a.");
        } catch (ObjectAlreadyHasException e) {
            Util.sendPluginMessage(sender, "The user is already a member of that group.");
        }
        saveUser(user, sender, plugin);
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return (argLength != 1 && argLength != 2);
    }
}
