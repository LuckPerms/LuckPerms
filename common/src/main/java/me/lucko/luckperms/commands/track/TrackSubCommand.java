package me.lucko.luckperms.commands.track;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.tracks.Track;

import java.util.List;

public abstract class TrackSubCommand extends SubCommand {
    public TrackSubCommand(String name, String description, String usage, Permission permission) {
        super(name, description, usage, permission);
    }

    protected abstract void execute(LuckPermsPlugin plugin, Sender sender, Track track, List<String> args);

    protected void saveTrack(Track track, Sender sender, LuckPermsPlugin plugin) {
        plugin.getDatastore().saveTrack(track, success -> {
            if (success) {
                Message.TRACK_SAVE_SUCCESS.send(sender);
            } else {
                Message.TRACK_SAVE_ERROR.send(sender);
            }

            plugin.runUpdateTask();
        });
    }
}
