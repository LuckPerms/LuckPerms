package me.lucko.luckperms.commands.track;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.utils.Patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TrackMainCommand extends MainCommand {

    private final List<TrackSubCommand> subCommands = new ArrayList<>();

    public TrackMainCommand() {
        super("Track", "/%s track <track>", 2);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        if (args.size() < 2) {
            sendUsage(sender, label);
            return;
        }

        Optional<TrackSubCommand> o = subCommands.stream().filter(s -> s.getName().equalsIgnoreCase(args.get(1))).limit(1).findAny();

        if (!o.isPresent()) {
            Message.COMMAND_NOT_RECOGNISED.send(sender);
            return;
        }

        final TrackSubCommand sub = o.get();
        if (!sub.isAuthorized(sender)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        List<String> strippedArgs = new ArrayList<>();
        if (args.size() > 2) {
            strippedArgs.addAll(args.subList(2, args.size()));
        }

        if (sub.isArgLengthInvalid(strippedArgs.size())) {
            sub.sendUsage(sender, label);
            return;
        }

        final String trackName = args.get(0).toLowerCase();

        if (Patterns.NON_ALPHA_NUMERIC.matcher(trackName).find()) {
            Message.TRACK_INVALID_ENTRY.send(sender);
            return;
        }

        plugin.getDatastore().loadTrack(trackName, success -> {
            if (!success) {
                Message.TRACK_NOT_FOUND.send(sender);
                return;
            }

            Track track = plugin.getTrackManager().getTrack(trackName);
            if (track == null) {
                Message.TRACK_NOT_FOUND.send(sender);
                return;
            }

            sub.execute(plugin, sender, track, strippedArgs, label);
        });
    }

    @Override
    protected List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        final List<String> tracks = new ArrayList<>(plugin.getTrackManager().getTracks().keySet());

        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return tracks;
            }

            return tracks.stream().filter(s -> s.toLowerCase().startsWith(args.get(0).toLowerCase())).collect(Collectors.toList());
        }

        return onAbstractTabComplete(sender, args, plugin);
    }

    @Override
    public List<? extends SubCommand> getSubCommands() {
        return subCommands;
    }

    public void registerSubCommand(TrackSubCommand subCommand) {
        subCommands.add(subCommand);
    }

}
