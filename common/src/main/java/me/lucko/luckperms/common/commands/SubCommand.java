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

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public abstract class SubCommand<T> {

    /**
     * The name of the sub command
     */
    private final String name;

    /**
     * A brief description of what the sub command does
     */
    private final String description;

    /**
     * The permission needed to use this command
     */
    private final Permission permission;

    /**
     * Predicate to test if the argument length given is invalid
     */
    private final Predicate<? super Integer> isArgumentInvalid;

    private final ImmutableList<Arg> args;

    /**
     * Called when this sub command is ran
     * @param plugin a link to the main plugin instance
     * @param sender the sender to executed the command
     * @param t the object the command is operating on
     * @param args the stripped arguments given
     * @param label the command label used
     */
    public abstract CommandResult execute(LuckPermsPlugin plugin, Sender sender, T t, List<String> args, String label);

    /**
     * Returns a list of suggestions, which are empty by default. Sub classes that give tab complete suggestions override
     * this method to give their own list.
     * @param plugin the plugin instance
     * @param sender who is tab completing
     * @param args the arguments so far
     * @return a list of suggestions
     */
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return Collections.emptyList();
    }

    /**
     * Send the command usage to a sender
     * @param sender the sender to send the usage to
     */
    public void sendUsage(Sender sender) {
        String usage = "";
        if (args != null) {
            usage += "&3 - &7";
            for (Arg arg : args) {
                usage += arg.asPrettyString() + " ";
            }
        }

        Util.sendPluginMessage(sender, "&3> &a" + getName().toLowerCase() + usage);
    }

    public void sendDetailedUsage(Sender sender) {
        Util.sendPluginMessage(sender, "&3&lCommand Usage &3- &b" + getName());
        Util.sendPluginMessage(sender, "&b> &7" + getDescription());
        if (args != null) {
            Util.sendPluginMessage(sender, "&3Arguments:");
            for (Arg arg : args) {
                Util.sendPluginMessage(sender, "&b- " + arg.asPrettyString() + "&3 -> &7" + arg.getDescription());
            }
        }
    }

    /**
     * If a sender has permission to use this command
     * @param sender the sender trying to use the command
     * @return true if the sender can use the command
     */
    public boolean isAuthorized(Sender sender) {
        return permission.isAuthorized(sender);
    }


    /*
     * ----------------------------------------------------------------------------------
     * Utility methods used by #onTabComplete and #execute implementations in sub classes
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
        plugin.doAsync(() -> {
            boolean success = plugin.getDatastore().saveUser(user).getUnchecked();
            user.getRefreshBuffer().request().getUnchecked();

            if (success) {
                Message.USER_SAVE_SUCCESS.send(sender);
            } else {
                Message.USER_SAVE_ERROR.send(sender);
            }
        });

    }

    public static void save(Group group, Sender sender, LuckPermsPlugin plugin) {
        plugin.doAsync(() -> {
            boolean success = plugin.getDatastore().saveGroup(group).getUnchecked();
            plugin.getUpdateTaskBuffer().request().getUnchecked();

            if (success) {
                Message.GROUP_SAVE_SUCCESS.send(sender);
            } else {
                Message.GROUP_SAVE_ERROR.send(sender);
            }
        });

    }

    public static void save(Track track, Sender sender, LuckPermsPlugin plugin) {
        plugin.doAsync(() -> {
            boolean success = plugin.getDatastore().saveTrack(track).getUnchecked();
            plugin.getUpdateTaskBuffer().request().getUnchecked();

            if (success) {
                Message.TRACK_SAVE_SUCCESS.send(sender);
            } else {
                Message.TRACK_SAVE_ERROR.send(sender);
            }
        });

    }
}
