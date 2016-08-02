package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.tracks.Track;

import java.util.List;
import java.util.stream.Collectors;

public class GroupShowTracks extends SubCommand<Group> {
    public GroupShowTracks() {
        super("showtracks", "Lists the tracks that this group features on", "/%s group <group> showtracks",
                Permission.GROUP_SHOWTRACKS, Predicate.alwaysFalse());
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        plugin.getDatastore().loadAllTracks(success -> {
            if (!success) {
                Message.TRACKS_LOAD_ERROR.send(sender);
                return;
            }

            Message.TRACKS_LIST.send(sender, Util.listToCommaSep(
                    plugin.getTrackManager().getApplicableTracks(group.getName()).stream().map(Track::getName).collect(Collectors.toList())));
        });
    }
}
