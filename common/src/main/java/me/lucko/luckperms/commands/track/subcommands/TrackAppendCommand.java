package me.lucko.luckperms.commands.track.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.track.TrackSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.tracks.Track;

import java.util.List;

public class TrackAppendCommand extends TrackSubCommand {
    public TrackAppendCommand() {
        super("append", "Appends a group onto the end of the track", "/perms track <track> append <group>", Permission.TRACK_APPEND);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Track track, List<String> args) {
        String groupName = args.get(0).toLowerCase();

        plugin.getDatastore().loadGroup(groupName, success -> {
            if (!success) {
                Message.GROUP_DOES_NOT_EXIST.send(sender);
            } else {
                Group group = plugin.getGroupManager().getGroup(groupName);
                if (group == null) {
                    Message.GROUP_DOES_NOT_EXIST.send(sender);
                    return;
                }

                try {
                    track.appendGroup(group);
                    Message.TRACK_APPEND_SUCCESS.send(sender, group.getName(), track.getName());
                    Message.EMPTY.send(sender, Util.listToArrowSep(track.getGroups(), group.getName()));
                    saveTrack(track, sender, plugin);
                } catch (ObjectAlreadyHasException e) {
                    Message.TRACK_ALREADY_CONTAINS.send(sender, track.getName(), group.getName());
                }
            }
        });
    }

    @Override
    public List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return getGroupTabComplete(args, plugin);
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength != 1;
    }
}
