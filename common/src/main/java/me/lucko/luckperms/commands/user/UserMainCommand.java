package me.lucko.luckperms.commands.user;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.users.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserMainCommand extends MainCommand{

    private final List<UserSubCommand> subCommands = new ArrayList<>();

    public UserMainCommand() {
        super("User", "/perms user <user>", 2);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        if (args.size() < 2) {
            sendUsage(sender);
            return;
        }

        Optional<UserSubCommand> o = subCommands.stream().filter(s -> s.getName().equalsIgnoreCase(args.get(1))).limit(1).findAny();

        if (!o.isPresent()) {
            Message.COMMAND_NOT_RECOGNISED.send(sender);
            return;
        }

        final UserSubCommand sub = o.get();
        if (!sub.isAuthorized(sender)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        // The arguments to be passed onto the sub command
        List<String> strippedArgs = new ArrayList<>();
        if (args.size() > 2) {
            strippedArgs.addAll(args.subList(2, args.size()));
        }

        if (sub.isArgLengthInvalid(strippedArgs.size())) {
            sub.sendUsage(sender);
            return;
        }

        final String user = args.get(0);
        UUID u = Util.parseUuid(user);
        if (u != null) {
            runSub(plugin, sender, u, sub, strippedArgs);
            return;
        }

        if (user.length() <= 16) {
            Message.USER_ATTEMPTING_LOOKUP.send(sender);

            plugin.getDatastore().getUUID(user, uuid -> {
                if (uuid == null) {
                    Message.USER_NOT_FOUND.send(sender);
                    return;
                }
                runSub(plugin, sender, uuid, sub, strippedArgs);
            });
            return;
        }

        Message.USER_INVALID_ENTRY.send(sender, user);
    }

    private void runSub(LuckPermsPlugin plugin, Sender sender, UUID uuid, UserSubCommand command, List<String> strippedArgs) {
        plugin.getDatastore().loadUser(uuid, success -> {
            if (!success) {
                Message.USER_NOT_FOUND.send(sender);
                return;
            }

            User user = plugin.getUserManager().getUser(uuid);
            if (user == null) {
                Message.USER_NOT_FOUND.send(sender);
            }

            command.execute(plugin, sender, user, strippedArgs);
            plugin.getUserManager().cleanupUser(user);
        });
    }

    @Override
    public List<? extends SubCommand> getSubCommands() {
        return subCommands;
    }

    public void registerSubCommand(UserSubCommand subCommand) {
        subCommands.add(subCommand);
    }
}
