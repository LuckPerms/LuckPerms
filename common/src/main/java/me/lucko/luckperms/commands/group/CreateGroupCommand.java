package me.lucko.luckperms.commands.group;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Messages;
import me.lucko.luckperms.constants.Permission;

import java.util.ArrayList;
import java.util.List;

public class CreateGroupCommand extends MainCommand {
    public CreateGroupCommand() {
        super("CreateGroup", "/perms creategroup <group>", 1);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        if (args.size() == 0) {
            sendUsage(sender);
            return;
        }

        String groupName = args.get(0).toLowerCase();
        plugin.getDatastore().loadGroup(groupName, success -> {
            if (success) {
                Messages.GROUP_ALREADY_EXISTS.send(sender);
            } else {
                plugin.getDatastore().createAndLoadGroup(groupName, success1 -> {
                    if (!success1) {
                        Messages.CREATE_GROUP_ERROR.send(sender);
                    } else {
                        Messages.CREATE_GROUP_SUCCESS.send(sender, groupName);
                        plugin.runUpdateTask();
                    }
                });
            }
        });
    }

    @Override
    public List<SubCommand> getSubCommands() {
        return new ArrayList<>();
    }

    @Override
    protected boolean canUse(Sender sender) {
        return Permission.CREATE_GROUP.isAuthorized(sender);
    }
}
