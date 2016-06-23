package me.lucko.luckperms.commands.group;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public abstract class GroupSubCommand extends SubCommand {
    protected GroupSubCommand(String name, String description, String usage, String permission) {
        super(name, description, usage, permission);
    }

    protected abstract void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args);

    protected void saveGroup(Group group, Sender sender, LuckPermsPlugin plugin) {
        plugin.getDatastore().saveGroup(group, success -> {
            if (success) {
                Util.sendPluginMessage(sender, "&7(Group data was saved to the datastore)");
            } else {
                Util.sendPluginMessage(sender, "There was an error whilst saving the group.");
            }

            plugin.runUpdateTask();
        });
    }
}
