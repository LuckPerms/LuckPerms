/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.common.commands;

import lombok.Getter;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.tracks.Track;
import me.lucko.luckperms.common.users.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Abstract SubCommand class
 */
@Getter
public abstract class SubCommand<T> extends Command<T, Void> {

    public SubCommand(String name, String description, Permission permission, Predicate<Integer> argumentCheck, List<Arg> args) {
        super(name, description, permission, argumentCheck, args);
    }

    /**
     * Send the command usage to a sender
     * @param sender the sender to send the usage to
     */
    @Override
    public void sendUsage(Sender sender, String label) {
        String usage = "";
        if (getArgs().isPresent()) {
            usage += "&3 - &7";
            for (Arg arg : getArgs().get()) {
                usage += arg.asPrettyString() + " ";
            }
        }

        Util.sendPluginMessage(sender, "&3> &a" + getName().toLowerCase() + usage);
    }

    @Override
    public void sendDetailedUsage(Sender sender, String label) {
        Util.sendPluginMessage(sender, "&3&lCommand Usage &3- &b" + getName());
        Util.sendPluginMessage(sender, "&b> &7" + getDescription());
        if (getArgs().isPresent()) {
            Util.sendPluginMessage(sender, "&3Arguments:");
            for (Arg arg : getArgs().get()) {
                Util.sendPluginMessage(sender, "&b- " + arg.asPrettyString() + "&3 -> &7" + arg.getDescription());
            }
        }
    }


    /*
     * ----------------------------------------------------------------------------------
     * Utility methods used by #tabComplete and #execute implementations in sub classes
     * ----------------------------------------------------------------------------------
     */

    public static List<String> getGroupTabComplete(List<String> args, LuckPermsPlugin plugin) {
        return getTabComplete(new ArrayList<>(plugin.getGroupManager().getAll().keySet()), args);
    }

    public static List<String> getTrackTabComplete(List<String> args, LuckPermsPlugin plugin) {
        return getTabComplete(new ArrayList<>(plugin.getTrackManager().getAll().keySet()), args);
    }

    public static List<String> getBoolTabComplete(List<String> args) {
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

    public static void save(User user, Sender sender, LuckPermsPlugin plugin) {
        boolean success = plugin.getDatastore().force().saveUser(user).getUnchecked();
        user.getRefreshBuffer().requestDirectly();

        if (success) {
            Message.USER_SAVE_SUCCESS.send(sender);
        } else {
            Message.USER_SAVE_ERROR.send(sender);
        }
    }

    public static void save(Group group, Sender sender, LuckPermsPlugin plugin) {
        boolean success = plugin.getDatastore().force().saveGroup(group).getUnchecked();
        plugin.getUpdateTaskBuffer().requestDirectly();

        if (success) {
            Message.GROUP_SAVE_SUCCESS.send(sender);
        } else {
            Message.GROUP_SAVE_ERROR.send(sender);
        }
    }

    public static void save(Track track, Sender sender, LuckPermsPlugin plugin) {
        boolean success = plugin.getDatastore().force().saveTrack(track).getUnchecked();
        plugin.getUpdateTaskBuffer().requestDirectly();

        if (success) {
            Message.TRACK_SAVE_SUCCESS.send(sender);
        } else {
            Message.TRACK_SAVE_ERROR.send(sender);
        }
    }
}
