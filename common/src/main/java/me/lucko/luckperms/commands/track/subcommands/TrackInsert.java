package me.lucko.luckperms.commands.track.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.tracks.Track;

import java.util.List;

public class TrackInsert extends SubCommand<Track> {
    public TrackInsert() {
        super("insert", "Inserts a group at a given position along the track",
                "/%s track <track> insert <group> <position>", Permission.TRACK_INSERT, Predicate.notOneOf(new Integer[]{2}));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, Track track, List<String> args, String label) {
        String groupName = args.get(0).toLowerCase();
        int pos;
        try {
            pos = Integer.parseInt(args.get(1));
        } catch (NumberFormatException e) {
            Message.TRACK_INSERT_ERROR_NUMBER.send(sender, args.get(1));
            return;
        }

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
                    track.insertGroup(group, pos - 1);
                    Message.TRACK_INSERT_SUCCESS.send(sender, group.getName(), track.getName(), pos);
                    Message.EMPTY.send(sender, Util.listToArrowSep(track.getGroups(), group.getName()));
                    saveTrack(track, sender, plugin);
                } catch (ObjectAlreadyHasException e) {
                    Message.TRACK_ALREADY_CONTAINS.send(sender, track.getName(), group.getName());
                } catch (IndexOutOfBoundsException e) {
                    Message.TRACK_INSERT_ERROR_INVALID_POS.send(sender, pos);
                }
            }
        });
    }

    @Override
    public List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return getGroupTabComplete(args, plugin);
    }
}
