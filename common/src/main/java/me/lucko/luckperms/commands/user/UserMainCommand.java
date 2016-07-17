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
import java.util.stream.Collectors;

public class UserMainCommand extends MainCommand {

    private final List<UserSubCommand> subCommands = new ArrayList<>();

    public UserMainCommand() {
        super("User", "/%s user <user>", 2);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        if (args.size() < 2) {
            sendUsage(sender, label);
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
            sub.sendUsage(sender, label);
            return;
        }

        final String user = args.get(0);
        UUID u = Util.parseUuid(user);
        if (u != null) {
            runSub(plugin, sender, u, sub, strippedArgs, label);
            return;
        }

        if (user.length() <= 16) {
            Message.USER_ATTEMPTING_LOOKUP.send(sender);

            plugin.getDatastore().getUUID(user, uuid -> {
                if (uuid == null) {
                    Message.USER_NOT_FOUND.send(sender);
                    return;
                }
                runSub(plugin, sender, uuid, sub, strippedArgs, label);
            });
            return;
        }

        Message.USER_INVALID_ENTRY.send(sender, user);
    }

    @Override
    protected List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        final List<String> onlinePlayers = plugin.getPlayerList();

        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return onlinePlayers;
            }

            return onlinePlayers.stream().filter(s -> s.toLowerCase().startsWith(args.get(0).toLowerCase())).collect(Collectors.toList());
        }

        return onAbstractTabComplete(sender, args, plugin);
    }

    private void runSub(LuckPermsPlugin plugin, Sender sender, UUID uuid, UserSubCommand command, List<String> strippedArgs, String label) {
        plugin.getDatastore().loadUser(uuid, success -> {
            if (!success) {
                Message.USER_NOT_FOUND.send(sender);
                return;
            }

            User user = plugin.getUserManager().getUser(uuid);
            if (user == null) {
                Message.USER_NOT_FOUND.send(sender);
            }

            command.execute(plugin, sender, user, strippedArgs, label);
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
