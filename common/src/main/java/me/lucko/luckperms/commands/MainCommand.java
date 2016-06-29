package me.lucko.luckperms.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.lucko.luckperms.LuckPermsPlugin;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public abstract class MainCommand {

    private final String name;
    private final String usage;
    private final int requiredArgsLength;

    protected abstract void execute(LuckPermsPlugin plugin, Sender sender, List<String> args);
    public abstract List<? extends SubCommand> getSubCommands();

    protected void sendUsage(Sender sender) {
        if (getSubCommands().isEmpty()) {
            Util.sendPluginMessage(sender, "&e-> &d" + getUsage());
            return;
        }

        List<SubCommand> subs = getSubCommands().stream().filter(s -> s.isAuthorized(sender)).collect(Collectors.toList());
        if (subs.size() > 0) {
            Util.sendPluginMessage(sender, "&e" + getName() + " Sub Commands:");

            for (SubCommand s : subs) {
                s.sendUsage(sender);
            }

        } else {
            Util.sendPluginMessage(sender, "You do not have permission to use this command!");
        }
    }

    protected boolean canUse(Sender sender) {
        return getSubCommands().stream().filter(sc -> sc.isAuthorized(sender)).count() != 0;
    }
}
