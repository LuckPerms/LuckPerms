package me.lucko.luckperms.commands.track.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.track.TrackSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.tracks.Track;

import java.util.List;

public class TrackClearCommand extends TrackSubCommand {
    public TrackClearCommand() {
        super("clear", "Clears the groups on the track", "/perms track <track> clear", Permission.TRACK_CLEAR);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Track track, List<String> args) {
        track.clearGroups();
        Message.TRACK_CLEAR.send(sender, track.getName());
        saveTrack(track, sender, plugin);
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return false;
    }
}
