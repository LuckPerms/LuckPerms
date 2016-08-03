package me.lucko.luckperms.commands.group;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SingleMainCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;

public class CreateGroup extends SingleMainCommand {
    public CreateGroup() {
        super("CreateGroup", "/%s creategroup <group>", 1, Permission.CREATE_GROUP);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        if (args.size() == 0) {
            sendUsage(sender, label);
            return;
        }

        String groupName = args.get(0).toLowerCase();

        if (groupName.length() > 36) {
            Message.GROUP_NAME_TOO_LONG.send(sender, groupName);
            return;
        }

        if (Patterns.NON_ALPHA_NUMERIC.matcher(groupName).find()) {
            Message.GROUP_INVALID_ENTRY.send(sender);
            return;
        }

        plugin.getDatastore().loadGroup(groupName, success -> {
            if (success) {
                Message.GROUP_ALREADY_EXISTS.send(sender);
            } else {
                plugin.getDatastore().createAndLoadGroup(groupName, success1 -> {
                    if (!success1) {
                        Message.CREATE_GROUP_ERROR.send(sender);
                    } else {
                        Message.CREATE_SUCCESS.send(sender, groupName);
                        plugin.runUpdateTask();
                    }
                });
            }
        });
    }
}
