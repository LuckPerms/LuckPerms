package me.lucko.luckperms.commands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.constants.Permission;

import java.util.Collections;
import java.util.List;

/**
 * An extension of {@link MainCommand} for implementations without any subcommands
 */
public class SingleMainCommand extends MainCommand<Object> {
    private final Permission permission;

    public SingleMainCommand(String name, String usage, int requiredArgsLength, Permission permission) {
        super(name, usage, requiredArgsLength);
        this.permission = permission;
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        // Do nothing, allow the implementation to override this
    }

    @Override
    protected void getTarget(String target, LuckPermsPlugin plugin, Sender sender, Callback onSuccess) {
        // Do nothing, don't run callback
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

    @Override
    protected boolean isAuthorized(Sender sender) {
        return permission.isAuthorized(sender);
    }
}
