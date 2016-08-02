package me.lucko.luckperms.commands.track.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.tracks.Track;

import java.util.List;

public class TrackInfo extends SubCommand<Track> {
    public TrackInfo() {
        super("info", "Gives info about the track", "/%s track <track> info", Permission.TRACK_INFO,
                Predicate.alwaysFalse());
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, Track track, List<String> args, String label) {
        Message.TRACK_INFO.send(sender, track.getName(), Util.listToArrowSep(track.getGroups()));
    }
}
