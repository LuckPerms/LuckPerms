package me.lucko.luckperms.commands;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.constants.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public abstract class MainCommand<T> {

    /**
     * The name of the main command
     */
    private final String name;

    /**
     * The command usage
     */
    private final String usage;

    /**
     * How many arguments are required for the command to run
     */
    private final int requiredArgsLength;

    /**
     * A list of the sub commands under this main command
     */
    @Getter
    private final List<SubCommand<T>> subCommands = new ArrayList<>();

    /**
     * Called when this main command is ran
     * @param plugin a link to the main plugin instance
     * @param sender the sender to executed the command
     * @param args the stripped arguments given
     * @param label the command label used
     */
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        if (args.size() < 2) {
            sendUsage(sender, label);
            return;
        }

        Optional<SubCommand<T>> o = subCommands.stream().filter(s -> s.getName().equalsIgnoreCase(args.get(1))).limit(1).findAny();

        if (!o.isPresent()) {
            Message.COMMAND_NOT_RECOGNISED.send(sender);
            return;
        }

        final SubCommand<T> sub = o.get();
        if (!sub.isAuthorized(sender)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        List<String> strippedArgs = new ArrayList<>();
        if (args.size() > 2) {
            strippedArgs.addAll(args.subList(2, args.size()));
        }

        if (sub.getIsArgumentInvalid().test(strippedArgs.size())) {
            sub.sendUsage(sender, label);
            return;
        }

        final String name = args.get(0).toLowerCase();
        getTarget(name, plugin, sender, t -> sub.execute(plugin, sender, t, strippedArgs, label));
    }

    /**
     * Gets the object the command is acting upon, and runs the callback if successful
     * @param target the name of the object to be looked up
     * @param plugin a link to the main plugin instance
     * @param sender the user who send the command (used to send error messages if the lookup was unsuccessful)
     * @param onSuccess the callback to run when the lookup is completed
     */
    protected abstract void getTarget(String target, LuckPermsPlugin plugin, Sender sender, Callback<T> onSuccess);

    /**
     * Get a list of objects for tab completion
     * @param plugin a link to the main plugin instance
     * @return a list of strings
     */
    protected abstract List<String> getObjects(LuckPermsPlugin plugin);

    /**
     * Send the command usage to a sender
     * @param sender the sender to send the usage to
     * @param label the command label used
     */
    protected void sendUsage(Sender sender, String label) {
        if (getSubCommands().isEmpty()) {
            Util.sendPluginMessage(sender, "&e-> &d" + String.format(getUsage(), label));
            return;
        }

        List<SubCommand> subs = getSubCommands().stream().filter(s -> s.isAuthorized(sender)).collect(Collectors.toList());
        if (subs.size() > 0) {
            Util.sendPluginMessage(sender, "&e" + getName() + " Sub Commands:");

            for (SubCommand s : subs) {
                s.sendUsage(sender, label);
            }

        } else {
            Message.COMMAND_NO_PERMISSION.send(sender);
        }
    }

    /**
     * If a sender has permission to use this command
     * @param sender the sender trying to use the command
     * @return true if the sender can use the command
     */
    protected boolean isAuthorized(Sender sender) {
        return getSubCommands().stream().filter(sc -> sc.isAuthorized(sender)).count() != 0;
    }

    protected List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        final List<String> objects = getObjects(plugin);

        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return objects;
            }

            return objects.stream().filter(s -> s.toLowerCase().startsWith(args.get(0).toLowerCase())).collect(Collectors.toList());
        }

        final List<SubCommand<T>> subs = getSubCommands().stream().filter(s -> s.isAuthorized(sender)).collect(Collectors.toList());
        if (args.size() == 2) {
            if (args.get(1).equalsIgnoreCase("")) {
                return subs.stream().map(SubCommand::getName).map(String::toLowerCase).collect(Collectors.toList());
            }

            return subs.stream().map(SubCommand::getName).map(String::toLowerCase)
                    .filter(s -> s.toLowerCase().startsWith(args.get(1).toLowerCase())).collect(Collectors.toList());
        }

        Optional<SubCommand<T>> o = subs.stream().filter(s -> s.getName().equalsIgnoreCase(args.get(1))).limit(1).findAny();
        if (!o.isPresent()) {
            return Collections.emptyList();
        }

        return o.get().onTabComplete(sender, args.subList(2, args.size()), plugin);
    }

    public void registerSubCommand(SubCommand<T> subCommand) {
        subCommands.add(subCommand);
    }
}
