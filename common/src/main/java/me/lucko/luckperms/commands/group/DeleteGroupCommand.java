package me.lucko.luckperms.commands.group;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.groups.Group;

import java.util.ArrayList;
import java.util.List;

public class DeleteGroupCommand extends MainCommand {
    public DeleteGroupCommand() {
        super("DeleteGroup", "/perms deletegroup <group>", 1);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        if (!sender.hasPermission("luckperms.deletegroup")) {
            Util.sendPluginMessage(sender, "You do not have permission to use this command!");
            return;
        }

        if (args.size() == 0) {
            sendUsage(sender);
            return;
        }

        String groupName = args.get(0).toLowerCase();

        if (groupName.equalsIgnoreCase(plugin.getConfiguration().getDefaultGroupName())) {
            Util.sendPluginMessage(sender, "You cannot delete the default group.");
            return;
        }

        plugin.getDatastore().loadGroup(groupName, success -> {
            if (!success) {
                Util.sendPluginMessage(sender, "That group does not exist!");
            } else {

                Group group = plugin.getGroupManager().getGroup(groupName);
                if (group == null) {
                    Util.sendPluginMessage(sender, "An unexpected error occurred.");
                } else {
                    plugin.getDatastore().deleteGroup(group, success1 -> {
                        if (!success1) {
                            Util.sendPluginMessage(sender, "There was an error whilst deleting the group.");
                        } else {
                            Util.sendPluginMessage(sender, "&b" + groupName + "&a was successfully deleted.");
                            plugin.runUpdateTask();
                        }
                    });
                }
            }
        });
    }

    @Override
    public List<SubCommand> getSubCommands() {
        return new ArrayList<>();
    }

    @Override
    protected boolean canUse(Sender sender) {
        return sender.hasPermission("luckperms.deletegroup") || sender.hasPermission("luckperms.*");
    }
}
