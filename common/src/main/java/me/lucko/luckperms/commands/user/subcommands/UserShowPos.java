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

package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserShowPos extends SubCommand<User> {
    public UserShowPos() {
        super("showpos", "Shows a users position on a track", "/%s user <user> showpos <track>", Permission.USER_SHOWPOS,
                Predicate.not(1));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        final String trackName = args.get(0).toLowerCase();

        plugin.getDatastore().loadTrack(trackName, success -> {
            if (!success) {
                Message.TRACK_DOES_NOT_EXIST.send(sender);
            } else {
                Track track = plugin.getTrackManager().getTrack(trackName);
                if (track == null) {
                    Message.TRACK_DOES_NOT_EXIST.send(sender);
                    return;
                }

                if (track.getSize() <= 1) {
                    Message.TRACK_EMPTY.send(sender);
                    return;
                }

                if (!track.containsGroup(user.getPrimaryGroup())) {
                    Message.TRACK_DOES_NOT_CONTAIN.send(sender, track.getName(), user.getPrimaryGroup());
                    return;
                }

                Message.USER_SHOWPOS.send(sender, user.getName(), track.getName(), Util.listToArrowSep(track.getGroups(), user.getPrimaryGroup()));
            }
        });
    }

    @Override
    public List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return getTrackTabComplete(args, plugin);
    }
}
