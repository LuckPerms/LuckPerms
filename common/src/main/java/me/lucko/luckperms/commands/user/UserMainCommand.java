package me.lucko.luckperms.commands.user;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;
import java.util.UUID;

public class UserMainCommand extends MainCommand<User> {
    public UserMainCommand() {
        super("User", "/%s user <user>", 2);
    }

    @Override
    protected void getTarget(String target, LuckPermsPlugin plugin, Sender sender, Callback<User> onSuccess) {
        UUID u = Util.parseUuid(target);
        if (u != null) {
            runSub(plugin, sender, u, onSuccess);
            return;
        }

        if (target.length() <= 16) {
            if (Patterns.NON_USERNAME.matcher(target).find()) {
                Message.USER_INVALID_ENTRY.send(sender, target);
                return;
            }

            Message.USER_ATTEMPTING_LOOKUP.send(sender);

            plugin.getDatastore().getUUID(target, uuid -> {
                if (uuid == null) {
                    Message.USER_NOT_FOUND.send(sender);
                    return;
                }

                runSub(plugin, sender, uuid, onSuccess);
            });
            return;
        }

        Message.USER_INVALID_ENTRY.send(sender, target);
    }

    private void runSub(LuckPermsPlugin plugin, Sender sender, UUID uuid, Callback<User> onSuccess) {
        plugin.getDatastore().loadUser(uuid, success -> {
            if (!success) {
                Message.USER_NOT_FOUND.send(sender);
                return;
            }

            User user = plugin.getUserManager().getUser(uuid);
            if (user == null) {
                Message.USER_NOT_FOUND.send(sender);
                return;
            }

            onSuccess.onComplete(user);
            plugin.getUserManager().cleanupUser(user);
        });
    }

    @Override
    protected List<String> getObjects(LuckPermsPlugin plugin) {
        return plugin.getPlayerList();
    }
}
