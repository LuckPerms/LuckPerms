package me.lucko.luckperms.commands.track;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SingleMainCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;

public class CreateTrack extends SingleMainCommand {
    public CreateTrack() {
        super("CreateTrack", "/%s createtrack <track>", 1, Permission.CREATE_TRACK);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        if (args.size() == 0) {
            sendUsage(sender, label);
            return;
        }

        String trackName = args.get(0).toLowerCase();

        if (trackName.length() > 36) {
            Message.TRACK_NAME_TOO_LONG.send(sender, trackName);
            return;
        }

        if (Patterns.NON_ALPHA_NUMERIC.matcher(trackName).find()) {
            Message.TRACK_INVALID_ENTRY.send(sender);
            return;
        }

        plugin.getDatastore().loadTrack(trackName, success -> {
            if (success) {
                Message.TRACK_ALREADY_EXISTS.send(sender);
            } else {
                plugin.getDatastore().createAndLoadTrack(trackName, success1 -> {
                    if (!success1) {
                        Message.CREATE_TRACK_ERROR.send(sender);
                    } else {
                        Message.CREATE_SUCCESS.send(sender, trackName);
                        plugin.runUpdateTask();
                    }
                });
            }
        });
    }
}
