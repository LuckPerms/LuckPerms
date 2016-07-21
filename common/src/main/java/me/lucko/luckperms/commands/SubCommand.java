package me.lucko.luckperms.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.users.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract SubCommand class
 * Doesn't declare any abstract onCommand methods, as sub classes declare their own with parameters unique to the sub command type.
 * For example, see: {@link me.lucko.luckperms.commands.user.UserSubCommand#execute(LuckPermsPlugin, Sender, User, List, String)}
 *
 * SubCommand #execute methods are always called from the {@link MainCommand} class related to them, so abstraction is not needed.
 */
@Getter
@AllArgsConstructor
public abstract class SubCommand {
    private final String name;
    private final String description;
    private final String usage;
    private final Permission permission;

    public abstract boolean isArgLengthInvalid(int argLength);

    public boolean isAuthorized(Sender sender) {
        return permission.isAuthorized(sender);
    }

    public void sendUsage(Sender sender, String label) {
        Util.sendPluginMessage(sender, "&e-> &d" + String.format(getUsage(), label));
    }

    /**
     * Returns a list of suggestions, which are empty by default. Sub classes that give tab complete suggestions override
     * this method to give their own list.
     */
    public List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return Collections.emptyList();
    }

    /* Utility methods used by #onTabComplete implementations in sub classes  */
    protected static List<String> getGroupTabComplete(List<String> args, LuckPermsPlugin plugin) {
        return getTabComplete(new ArrayList<>(plugin.getGroupManager().getGroups().keySet()), args);
    }

    protected static List<String> getTrackTabComplete(List<String> args, LuckPermsPlugin plugin) {
        return getTabComplete(new ArrayList<>(plugin.getTrackManager().getTracks().keySet()), args);
    }

    protected static List<String> getBoolTabComplete(List<String> args) {
        if (args.size() == 2) {
            return Arrays.asList("true", "false");
        } else {
            return Collections.emptyList();
        }
    }

    private static List<String> getTabComplete(List<String> options, List<String> args) {
        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return options;
            }

            return options.stream().filter(s -> s.toLowerCase().startsWith(args.get(0).toLowerCase())).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
