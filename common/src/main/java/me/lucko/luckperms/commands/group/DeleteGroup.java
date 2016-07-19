package me.lucko.luckperms.commands.group;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.groups.Group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DeleteGroup extends MainCommand {
    public DeleteGroup() {
        super("DeleteGroup", "/%s deletegroup <group>", 1);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        if (args.size() == 0) {
            sendUsage(sender, label);
            return;
        }

        String groupName = args.get(0).toLowerCase();

        if (groupName.equalsIgnoreCase(plugin.getConfiguration().getDefaultGroupName())) {
            Message.DELETE_GROUP_ERROR_DEFAULT.send(sender);
            return;
        }

        plugin.getDatastore().loadGroup(groupName, success -> {
            if (!success) {
                Message.GROUP_DOES_NOT_EXIST.send(sender);
            } else {

                Group group = plugin.getGroupManager().getGroup(groupName);
                if (group == null) {
                    Message.GROUP_LOAD_ERROR.send(sender);
                } else {
                    plugin.getDatastore().deleteGroup(group, success1 -> {
                        if (!success1) {
                            Message.DELETE_GROUP_ERROR.send(sender);
                        } else {
                            Message.DELETE_SUCCESS.send(sender, groupName);
                            plugin.runUpdateTask();
                        }
                    });
                }
            }
        });
    }

    @Override
    protected List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        final List<String> groups = new ArrayList<>(plugin.getGroupManager().getGroups().keySet());

        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return groups;
            }

            return groups.stream().filter(s -> s.toLowerCase().startsWith(args.get(0).toLowerCase())).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @Override
    public List<SubCommand> getSubCommands() {
        return Collections.emptyList();
    }

    @Override
    protected boolean canUse(Sender sender) {
        return Permission.DELETE_GROUP.isAuthorized(sender);
    }
}
