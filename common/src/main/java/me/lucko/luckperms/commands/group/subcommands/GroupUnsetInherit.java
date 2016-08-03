package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;

public class GroupUnsetInherit extends SubCommand<Group> {
    public GroupUnsetInherit() {
        super("unsetinherit", "Unsets another group for this group to inherit permissions from",
                "/%s group <group> unsetinherit <group> [server] [world]", Permission.GROUP_UNSETINHERIT,
                Predicate.notInRange(1, 3));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        String groupName = args.get(0).toLowerCase();

        if (groupName.contains("/") || groupName.contains("$")) {
            sendUsage(sender, label);
            return;
        }

        try {
            if (args.size() >= 2) {
                final String server = args.get(1).toLowerCase();
                if (Patterns.NON_ALPHA_NUMERIC.matcher(server).find()) {
                    Message.SERVER_INVALID_ENTRY.send(sender);
                    return;
                }

                if (args.size() == 2) {
                    group.unsetPermission("group." + groupName, server);
                    Message.GROUP_UNSETINHERIT_SERVER_SUCCESS.send(sender, group.getName(), groupName, server);
                } else {
                    final String world = args.get(2).toLowerCase();
                    group.unsetPermission("group." + groupName, server, world);
                    Message.GROUP_UNSETINHERIT_SERVER_WORLD_SUCCESS.send(sender, group.getName(), groupName, server, world);
                }

            } else {
                group.unsetPermission("group." + groupName);
                Message.GROUP_UNSETINHERIT_SUCCESS.send(sender, group.getName(), groupName);
            }

            saveGroup(group, sender, plugin);
        } catch (ObjectLacksException e) {
            Message.GROUP_DOES_NOT_INHERIT.send(sender, group.getName(), groupName);
        }
    }

    @Override
    public List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return getGroupTabComplete(args, plugin);
    }
}
