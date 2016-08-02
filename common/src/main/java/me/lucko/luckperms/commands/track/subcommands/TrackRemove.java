package me.lucko.luckperms.commands.track.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.tracks.Track;

import java.util.List;

public class TrackRemove extends SubCommand<Track> {
    public TrackRemove() {
        super("remove", "Removes a group from the track", "/%s track <track> remove <group>", Permission.TRACK_REMOVE,
                Predicate.notOneOf(new Integer[]{1}));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, Track track, List<String> args, String label) {
        String groupName = args.get(0).toLowerCase();
        try {
            track.removeGroup(groupName);
            Message.TRACK_REMOVE_SUCCESS.send(sender, groupName, track.getName());
            saveTrack(track, sender, plugin);
        } catch (ObjectLacksException e) {
            Message.TRACK_DOES_NOT_CONTAIN.send(sender, track.getName(), groupName);
        }
    }

    @Override
    public List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return getGroupTabComplete(args, plugin);
    }
}
