package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.users.User;

import java.util.List;
import java.util.stream.Collectors;

public class UserShowTracksCommand extends UserSubCommand {
    public UserShowTracksCommand() {
        super("showtracks", "Lists the tracks that this user's primary group features on", "/perms user <user> showtracks", Permission.USER_SHOWTRACKS);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args) {
        plugin.getDatastore().loadAllTracks(success -> {
            if (!success) {
                Message.TRACKS_LOAD_ERROR.send(sender);
                return;
            }

            Message.USER_SHOWTRACKS_INFO.send(sender, user.getPrimaryGroup(), user.getName());
            Message.TRACKS_LIST.send(sender, Util.listToCommaSep(
                    plugin.getTrackManager().getApplicableTracks(user.getPrimaryGroup()).stream().map(Track::getName).collect(Collectors.toList())));
        });
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return false;
    }
}
