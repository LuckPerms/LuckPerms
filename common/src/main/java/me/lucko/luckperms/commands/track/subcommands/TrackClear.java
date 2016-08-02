package me.lucko.luckperms.commands.track.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.tracks.Track;

import java.util.List;

public class TrackClear extends SubCommand<Track> {
    public TrackClear() {
        super("clear", "Clears the groups on the track", "/%s track <track> clear", Permission.TRACK_CLEAR,
                Predicate.alwaysFalse());
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, Track track, List<String> args, String label) {
        track.clearGroups();
        Message.TRACK_CLEAR.send(sender, track.getName());
        saveTrack(track, sender, plugin);
    }
}
