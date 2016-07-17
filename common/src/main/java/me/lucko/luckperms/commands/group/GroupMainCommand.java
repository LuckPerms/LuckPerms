package me.lucko.luckperms.commands.group;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.groups.Group;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GroupMainCommand extends MainCommand {

    private final List<GroupSubCommand> subCommands = new ArrayList<>();

    public GroupMainCommand() {
        super("Group", "/%s group <group>", 2);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        if (args.size() < 2) {
            sendUsage(sender, label);
            return;
        }

        Optional<GroupSubCommand> o = subCommands.stream().filter(s -> s.getName().equalsIgnoreCase(args.get(1))).limit(1).findAny();

        if (!o.isPresent()) {
            Message.COMMAND_NOT_RECOGNISED.send(sender);
            return;
        }

        final GroupSubCommand sub = o.get();
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

        final String groupName = args.get(0).toLowerCase();
        plugin.getDatastore().loadGroup(groupName, success -> {
            if (!success) {
                Message.GROUP_NOT_FOUND.send(sender);
                return;
            }

            Group group = plugin.getGroupManager().getGroup(groupName);
            if (group == null) {
                Message.GROUP_NOT_FOUND.send(sender);
                return;
            }

            sub.execute(plugin, sender, group, strippedArgs, label);
        });
    }

    @Override
    protected List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        final List<String> groups = new ArrayList<>(plugin.getGroupManager().getGroups().keySet());

        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return groups;
            }

            return groups.stream().filter(s -> s.toLowerCase().startsWith(args.get(0).toLowerCase())).collect(Collectors.toList());
        }

        return onAbstractTabComplete(sender, args, plugin);
    }

    @Override
    public List<? extends SubCommand> getSubCommands() {
        return subCommands;
    }

    public void registerSubCommand(GroupSubCommand subCommand) {
        subCommands.add(subCommand);
    }

}
