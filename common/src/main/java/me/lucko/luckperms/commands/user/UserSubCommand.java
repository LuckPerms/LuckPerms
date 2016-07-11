package me.lucko.luckperms.commands.user;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Messages;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.users.User;

import java.util.List;

public abstract class UserSubCommand extends SubCommand {
    protected UserSubCommand(String name, String description, String usage, Permission permission) {
        super(name, description, usage, permission);
    }

    protected abstract void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args);

    protected void saveUser(User user, Sender sender, LuckPermsPlugin plugin) {
        user.refreshPermissions();

        plugin.getDatastore().saveUser(user, success -> {
            if (success) {
                Messages.USER_SAVE_SUCCESS.send(sender);
            } else {
                Messages.USER_SAVE_ERROR.send(sender);
            }
        });
    }
}
