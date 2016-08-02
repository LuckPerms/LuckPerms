package me.lucko.luckperms.commands.track;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.tracks.Track;

import java.util.ArrayList;
import java.util.List;

public class TrackMainCommand extends MainCommand<Track> {
    public TrackMainCommand() {
        super("Track", "/%s track <track>", 2);
    }

    @Override
    protected void getTarget(String target, LuckPermsPlugin plugin, Sender sender, Callback<Track> onSuccess) {
        plugin.getDatastore().loadTrack(target, success -> {
            if (!success) {
                Message.TRACK_NOT_FOUND.send(sender);
                return;
            }

            Track track = plugin.getTrackManager().getTrack(target);
            if (track == null) {
                Message.TRACK_NOT_FOUND.send(sender);
                return;
            }

            onSuccess.onComplete(track);
        });
    }

    @Override
    protected List<String> getObjects(LuckPermsPlugin plugin) {
        return new ArrayList<>(plugin.getTrackManager().getTracks().keySet());
    }
}
