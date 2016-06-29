package me.lucko.luckperms.commands.group;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.*;

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
                Util.sendPluginMessage(sender, "That group already exists!");
            } else {
                plugin.getDatastore().createAndLoadGroup(groupName, success1 -> {
                    if (!success1) {
                        Util.sendPluginMessage(sender, "There was an error whilst creating the group.");
                    } else {
                        Util.sendPluginMessage(sender, "&b" + groupName + "&a was successfully created.");
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
