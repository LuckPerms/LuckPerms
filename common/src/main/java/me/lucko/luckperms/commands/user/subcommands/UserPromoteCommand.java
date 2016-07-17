package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserPromoteCommand extends UserSubCommand {
    public UserPromoteCommand() {
        super("promote", "Promotes the user along a track", "/%s user <user> promote <track>", Permission.USER_PROMOTE);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
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
                final String next;
                try {
                    next = track.getNext(old);
                } catch (ObjectLacksException e) {
                    Message.TRACK_DOES_NOT_CONTAIN.send(sender, track.getName(), old);
                    Message.USER_PROMOTE_ERROR_NOT_CONTAIN_GROUP.send(sender);
                    return;
                }

                if (next == null) {
                    Message.USER_PROMOTE_ERROR_ENDOFTRACK.send(sender, track.getName());
                    return;
                }

                plugin.getDatastore().loadGroup(next, success1 -> {
                    if (!success1) {
                        Message.USER_PROMOTE_ERROR_MALFORMED.send(sender, next);
                    } else {
                        Group nextGroup = plugin.getGroupManager().getGroup(next);
                        if (nextGroup == null) {
                            Message.USER_PROMOTE_ERROR_MALFORMED.send(sender, next);
                            return;
                        }

                        try {
                            user.unsetPermission("group." + old);
                            user.addGroup(nextGroup);
                            user.setPrimaryGroup(nextGroup.getName());
                        } catch (ObjectLacksException | ObjectAlreadyHasException ignored) {}

                        Message.USER_PROMOTE_SUCCESS_PROMOTE.send(sender, track.getName(), old, nextGroup.getName());
                        Message.USER_PROMOTE_SUCCESS_REMOVE.send(sender, user.getName(), old, nextGroup.getName(), nextGroup.getName());
                        Message.EMPTY.send(sender, Util.listToArrowSep(track.getGroups(), old, nextGroup.getName(), false));
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

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength != 1;
    }
}
