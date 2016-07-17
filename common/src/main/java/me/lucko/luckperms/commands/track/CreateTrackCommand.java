package me.lucko.luckperms.commands.track;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.utils.Patterns;

import java.util.Collections;
import java.util.List;

public class CreateTrackCommand extends MainCommand {
    public CreateTrackCommand() {
        super("CreateTrack", "/perms createtrack <track>", 1);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        if (args.size() == 0) {
            sendUsage(sender);
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

    @Override
    protected List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return Collections.emptyList();
    }

    @Override
    public List<? extends SubCommand> getSubCommands() {
        return Collections.emptyList();
    }

    @Override
    protected boolean canUse(Sender sender) {
        return Permission.CREATE_TRACK.isAuthorized(sender);
    }
}
