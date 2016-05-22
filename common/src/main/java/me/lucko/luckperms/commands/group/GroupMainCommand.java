package me.lucko.luckperms.commands.group;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.groups.Group;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GroupMainCommand extends MainCommand {

    private final List<GroupSubCommand> subCommands = new ArrayList<>();

    public GroupMainCommand() {
        super("Group", "/perms group <group>", 2);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        if (args.size() <= 1) {
            sendUsage(sender);
            return;
        }

        List<String> strippedArgs = new ArrayList<>();
        if (args.size() > 2) {
            strippedArgs.addAll(args.subList(2, args.size()));
        }

        String c = args.get(1);
        GroupSubCommand tempSub = null;

        for (GroupSubCommand s : subCommands) {
            if (s.getName().equalsIgnoreCase(c)) {
                tempSub = s;
                break;
            }
        }

        final GroupSubCommand sub = tempSub;

        if (sub == null) {
            Util.sendPluginMessage(sender, "Command not recognised.");
            return;
        }


        if (!sub.isAuthorized(sender)) {
            Util.sendPluginMessage(sender, "You do not have permission to use this command!");
            return;
        }

        String g = args.get(0).toLowerCase();

        plugin.getDatastore().loadGroup(g, success -> {
            if (!success) {
                Util.sendPluginMessage(sender, "&eGroup could not be found.");
                return;
            }

            Group group = plugin.getGroupManager().getGroup(g);
            if (group == null) {
                Util.sendPluginMessage(sender, "&eGroup could not be found.");
                return;
            }

            if (sub.isArgLengthInvalid(strippedArgs.size())) {
                sub.sendUsage(sender);
                return;
            }

            sub.execute(plugin, sender, group, strippedArgs);
        });
    }

    @Override
    public List<? extends SubCommand> getSubCommands() {
        return subCommands;
    }

    public void registerSubCommand(GroupSubCommand subCommand) {
        subCommands.add(subCommand);
    }

    @Override
    protected void sendUsage(Sender sender) {
        List<SubCommand> subs = getSubCommands().stream().filter(s -> s.isAuthorized(sender)).collect(Collectors.toList());
        if (subs.size() > 0) {
            Util.sendPluginMessage(sender, "&e" + getName() + " Sub Commands:");

            for (SubCommand s : subs) {
                s.sendUsage(sender);
            }

        } else {
            Util.sendPluginMessage(sender, "You do not have permission to use this command!");
        }
    }
}
