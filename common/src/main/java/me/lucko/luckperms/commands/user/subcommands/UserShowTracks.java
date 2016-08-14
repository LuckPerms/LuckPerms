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
import me.lucko.luckperms.commands.*;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.users.User;

import java.util.List;
import java.util.stream.Collectors;

public class UserShowTracks extends SubCommand<User> {
    public UserShowTracks() {
        super("showtracks", "Lists the tracks that this user's primary group features on", "/%s user <user> showtracks",
                Permission.USER_SHOWTRACKS, Predicate.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        if (!plugin.getDatastore().loadAllTracks()) {
            Message.TRACKS_LOAD_ERROR.send(sender);
            return CommandResult.LOADING_ERROR;
        }

        Message.USER_SHOWTRACKS_INFO.send(sender, user.getPrimaryGroup(), user.getName());
        Message.TRACKS_LIST.send(sender,
                Util.listToCommaSep(plugin.getTrackManager().getApplicableTracks(user.getPrimaryGroup()).stream()
                        .map(Track::getName)
                        .collect(Collectors.toList())
                )
        );

        return CommandResult.SUCCESS;
    }
}
