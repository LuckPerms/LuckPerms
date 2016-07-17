package me.lucko.luckperms.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.constants.Message;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public abstract class MainCommand {

    private final String name;
    private final String usage;
    private final int requiredArgsLength;

    protected abstract void execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label);
    protected abstract List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin);
    public abstract List<? extends SubCommand> getSubCommands();

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

    protected boolean canUse(Sender sender) {
        return getSubCommands().stream().filter(sc -> sc.isAuthorized(sender)).count() != 0;
    }

    protected List<String> onAbstractTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        final List<SubCommand> subs = getSubCommands().stream().filter(s -> s.isAuthorized(sender)).collect(Collectors.toList());
        if (args.size() == 2) {
            if (args.get(1).equalsIgnoreCase("")) {
                return subs.stream().map(SubCommand::getName).map(String::toLowerCase).collect(Collectors.toList());
            }

            return subs.stream().map(SubCommand::getName).map(String::toLowerCase)
                    .filter(s -> s.toLowerCase().startsWith(args.get(1).toLowerCase())).collect(Collectors.toList());
        }

        Optional<SubCommand> o = subs.stream().filter(s -> s.getName().equalsIgnoreCase(args.get(1))).limit(1).findAny();
        if (!o.isPresent()) {
            return Collections.emptyList();
        }

        return o.get().onTabComplete(sender, args.subList(2, args.size()), plugin);
    }
}
