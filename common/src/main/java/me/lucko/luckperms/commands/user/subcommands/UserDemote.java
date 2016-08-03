package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserDemote extends SubCommand<User> {
    public UserDemote() {
        super("demote", "Demotes a user along a track", "/%s user <user> demote <track>", Permission.USER_DEMOTE,
                Predicate.not(1));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        final String trackName = args.get(0).toLowerCase();

        plugin.getDatastore().loadTrack(trackName, success -> {
            if (!success) {
                Message.TRACK_DOES_NOT_EXIST.send(sender);
            } else {
                Track track = plugin.getTrackManager().getTrack(trackName);
                if (track == null) {
                    Message.TRACK_DOES_NOT_EXIST.send(sender);
                    return;
                }

                if (track.getSize() <= 1) {
                    Message.TRACK_EMPTY.send(sender);
                    return;
                }

                final String old = user.getPrimaryGroup();
                final String previous;
                try {
                    previous = track.getPrevious(old);
                } catch (ObjectLacksException e) {
                    Message.TRACK_DOES_NOT_CONTAIN.send(sender, track.getName(), old);
                    Message.USER_DEMOTE_ERROR_NOT_CONTAIN_GROUP.send(sender);
                    return;
                }

                if (previous == null) {
                    Message.USER_DEMOTE_ERROR_ENDOFTRACK.send(sender, track.getName());
                    return;
                }

                plugin.getDatastore().loadGroup(previous, success1 -> {
                    if (!success1) {
                        Message.USER_DEMOTE_ERROR_MALFORMED.send(sender, previous);
                    } else {
                        Group previousGroup = plugin.getGroupManager().getGroup(previous);
                        if (previousGroup == null) {
                            Message.USER_DEMOTE_ERROR_MALFORMED.send(sender, previous);
                            return;
                        }

                        try {
                            user.unsetPermission("group." + old);
                        } catch (ObjectLacksException ignored) {}
                        try {
                            user.addGroup(previousGroup);
                        } catch (ObjectAlreadyHasException ignored) {}
                        user.setPrimaryGroup(previousGroup.getName());

                        Message.USER_DEMOTE_SUCCESS_PROMOTE.send(sender, track.getName(), old, previousGroup.getName());
                        Message.USER_DEMOTE_SUCCESS_REMOVE.send(sender, user.getName(), old, previousGroup.getName(), previousGroup.getName());
                        Message.EMPTY.send(sender, Util.listToArrowSep(track.getGroups(), previousGroup.getName(), old, true));
                        saveUser(user, sender, plugin);
                    }
                });
            }
        });
    }

    @Override
    public List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return getTrackTabComplete(args, plugin);
    }
}
