package me.lucko.luckperms.commands.user;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.users.User;

import java.util.List;

public abstract class UserSubCommand extends SubCommand {
    protected UserSubCommand(String name, String description, String usage, String permission) {
        super(name, description, usage, permission);
    }

    protected abstract void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args);

    @Override
    public boolean isAuthorized(Sender sender) {
        return sender.hasPermission(getPermission()) || sender.hasPermission("luckperms.user.*") || sender.hasPermission("luckperms.*");
    }

    protected void saveUser(User user, Sender sender, LuckPermsPlugin plugin) {
        user.refreshPermissions();

        plugin.getDatastore().saveUser(user, success -> {
            if (success) {
                Util.sendPluginMessage(sender, "&7(User data was saved to the datastore)");
            } else {
                Util.sendPluginMessage(sender, "There was an error whilst saving the user.");
            }
        });
    }
}
