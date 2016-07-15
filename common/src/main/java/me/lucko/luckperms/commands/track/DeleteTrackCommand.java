package me.lucko.luckperms.commands.track;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.tracks.Track;

import java.util.ArrayList;
import java.util.List;

public class DeleteTrackCommand extends MainCommand {
    public DeleteTrackCommand() {
        super("DeleteTrack", "/perms deletetrack <track>", 1);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        if (args.size() == 0) {
            sendUsage(sender);
            return;
        }

        String trackName = args.get(0).toLowerCase();

        plugin.getDatastore().loadTrack(trackName, success -> {
            if (!success) {
                Message.TRACK_DOES_NOT_EXIST.send(sender);
            } else {

                Track track = plugin.getTrackManager().getTrack(trackName);
                if (track == null) {
                    Message.TRACK_LOAD_ERROR.send(sender);
                } else {
                    plugin.getDatastore().deleteTrack(track, success1 -> {
                        if (!success1) {
                            Message.DELETE_TRACK_ERROR.send(sender);
                        } else {
                            Message.DELETE_SUCCESS.send(sender, trackName);
                            plugin.runUpdateTask();
                        }
                    });
                }
            }
        });
    }

    @Override
    public List<SubCommand> getSubCommands() {
        return new ArrayList<>();
    }

    @Override
    protected boolean canUse(Sender sender) {
        return Permission.DELETE_TRACK.isAuthorized(sender);
    }
}
