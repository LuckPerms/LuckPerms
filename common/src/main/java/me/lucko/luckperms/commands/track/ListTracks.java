package me.lucko.luckperms.commands.track;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SingleMainCommand;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;

import java.util.ArrayList;
import java.util.List;

public class ListTracks extends SingleMainCommand {
    public ListTracks() {
        super("ListTracks", "/%s listtracks", 0);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        plugin.getDatastore().loadAllTracks(success -> {
            if (!success) {
                Message.TRACKS_LOAD_ERROR.send(sender);
            } else {
                Message.TRACKS_LIST.send(sender, Util.listToCommaSep(new ArrayList<>(plugin.getTrackManager().getTracks().keySet())));
            }
        });
    }

    @Override
    protected boolean isAuthorized(Sender sender) {
        return Permission.LIST_TRACKS.isAuthorized(sender);
    }
}
