package me.lucko.luckperms.commands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.data.Callback;

import java.util.Collections;
import java.util.List;

/**
 * An extension of {@link MainCommand} for implementations without any subcommands
 */
public class SingleMainCommand extends MainCommand<Object> {
    public SingleMainCommand(String name, String usage, int requiredArgsLength) {
        super(name, usage, requiredArgsLength);
    }

    @Override
    protected void getTarget(String target, LuckPermsPlugin plugin, Sender sender, Callback onSuccess) {

    }

    @Override
    protected List<String> getObjects(LuckPermsPlugin plugin) {
        return Collections.emptyList();
    }

    @Override
    protected List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return Collections.emptyList();
    }

    @Override
    public List<SubCommand<Object>> getSubCommands() {
        return Collections.emptyList();
    }
}
