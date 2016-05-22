package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public class GroupSetInheritCommand extends GroupSubCommand {
    public GroupSetInheritCommand() {
        super("setinherit", "Sets another group for this group to inherit permissions from",
                "/perms group <group> setinherit <group> [server]", "luckperms.group.setinherit");
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args) {
        String groupName = args.get(0).toLowerCase();

        plugin.getDatastore().loadGroup(groupName, success -> {
            if (!success) {
                Util.sendPluginMessage(sender, groupName + " does not exist!");
            } else {
                final String node = "luckperms.group." + groupName;

                try {
                    if (args.size() == 2) {
                        final String server = args.get(1).toLowerCase();
                        group.setPermission(node, true, server);
                        Util.sendPluginMessage(sender, "&b" + group.getName() + "&a now inherits permissions from &b" + groupName + "&a on server &b" + server + "&a.");
                    } else {
                        group.setPermission(node, true);
                        Util.sendPluginMessage(sender, "&b" + group.getName() + "&a now inherits permissions from &b" + groupName + "&a.");
                    }

                    saveGroup(group, sender, plugin);
                } catch (ObjectAlreadyHasException e) {
                    Util.sendPluginMessage(sender, group.getName() + " already inherits '" + groupName + "'.");
                }
            }
        });
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength == 0;
    }
}
