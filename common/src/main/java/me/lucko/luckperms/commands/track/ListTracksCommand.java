package me.lucko.luckperms.commands.track;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;

import java.util.ArrayList;
import java.util.List;

public class ListTracksCommand extends MainCommand {
    public ListTracksCommand() {
        super("ListTracks", "/perms listtracks", 0);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        plugin.getDatastore().loadAllTracks(success -> {
            if (!success) {
                Message.TRACKS_LOAD_ERROR.send(sender);
            } else {
                Message.TRACKS_LIST.send(sender, Util.listToCommaSep(new ArrayList<>(plugin.getTrackManager().getTracks().keySet())));
            }
        });
    }

    @Override
    public List<SubCommand> getSubCommands() {
        return new ArrayList<>();
    }

    @Override
    protected boolean canUse(Sender sender) {
        return Permission.LIST_TRACKS.isAuthorized(sender);
    }
}
