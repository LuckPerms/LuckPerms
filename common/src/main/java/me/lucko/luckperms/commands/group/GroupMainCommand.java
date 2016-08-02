package me.lucko.luckperms.commands.group;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.Patterns;

import java.util.ArrayList;
import java.util.List;

public class GroupMainCommand extends MainCommand<Group> {
    public GroupMainCommand() {
        super("Group", "/%s group <group>", 2);
    }

    @Override
    protected void getTarget(String target, LuckPermsPlugin plugin, Sender sender, Callback<Group> onSuccess) {
        if (Patterns.NON_ALPHA_NUMERIC.matcher(target).find()) {
            Message.GROUP_INVALID_ENTRY.send(sender);
            return;
        }

        plugin.getDatastore().loadGroup(target, success -> {
            if (!success) {
                Message.GROUP_NOT_FOUND.send(sender);
                return;
            }

            Group group = plugin.getGroupManager().getGroup(target);
            if (group == null) {
                Message.GROUP_NOT_FOUND.send(sender);
                return;
            }

            onSuccess.onComplete(group);
        });
    }

    @Override
    protected List<String> getObjects(LuckPermsPlugin plugin) {
        return new ArrayList<>(plugin.getGroupManager().getGroups().keySet());
    }
}
