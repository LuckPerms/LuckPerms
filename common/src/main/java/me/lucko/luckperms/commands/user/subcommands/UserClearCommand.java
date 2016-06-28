package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserClearCommand extends UserSubCommand {
    public UserClearCommand() {
        super("clear", "Clears a users permissions and groups",
                "/perms user <user> clear", "luckperms.user.clear");
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args) {
        user.clearNodes();
        plugin.getUserManager().giveDefaults(user);
        Util.sendPluginMessage(sender, "&b" + user.getName() + "&a's permissions were cleared.");

        saveUser(user, sender, plugin);
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return false;
    }
}
