package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.tracks.Track;

import java.util.List;
import java.util.stream.Collectors;

public class GroupShowTracksCommand extends GroupSubCommand {
    public GroupShowTracksCommand() {
        super("showtracks", "Lists the tracks that this group features on", "/perms group <group> showtracks", Permission.GROUP_SHOWTRACKS);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args) {
        plugin.getDatastore().loadAllTracks(success -> {
            if (!success) {
                Message.TRACKS_LOAD_ERROR.send(sender);
                return;
            }

            Message.TRACKS_LIST.send(sender, Util.listToCommaSep(
                    plugin.getTrackManager().getApplicableTracks(group.getName()).stream().map(Track::getName).collect(Collectors.toList())));
        });
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return false;
    }
}
