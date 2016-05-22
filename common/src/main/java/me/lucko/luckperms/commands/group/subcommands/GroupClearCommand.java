package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public class GroupClearCommand extends GroupSubCommand {
    public GroupClearCommand() {
        super("clear", "Clears a groups permissions",
                "/perms group <group> clear", "luckperms.group.clear");
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args) {
        group.clearNodes();
        Util.sendPluginMessage(sender, "&b" + group.getName() + "&a's permissions were cleared.");

        saveGroup(group, sender, plugin);
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return true;
    }
}
