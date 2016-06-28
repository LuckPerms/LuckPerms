package me.lucko.luckperms.commands;

import lombok.Getter;
import me.lucko.luckperms.LuckPermsPlugin;

import java.util.List;
import java.util.stream.Collectors;

public abstract class MainCommand {

    @Getter
    private final String name;

    @Getter
    private final String usage;

    @Getter
    private final int requiredArgsLength;

    protected MainCommand(String name, String usage, int requiredArgsLength) {
        this.name = name;
        this.usage = usage;
        this.requiredArgsLength = requiredArgsLength;
    }

    protected abstract void execute(LuckPermsPlugin plugin, Sender sender, List<String> args);
    public abstract List<? extends SubCommand> getSubCommands();

    protected void sendUsage(Sender sender) {
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
        for (SubCommand sc : getSubCommands()) {
            if (sc.isAuthorized(sender)) {
                return true;
            }
        }
        return false;
    }
}
