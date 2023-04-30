/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
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

package me.lucko.luckperms.common.commands.user;

import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.abstraction.CommandException;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.tabcomplete.TabCompleter;
import me.lucko.luckperms.common.command.tabcomplete.TabCompletions;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.storage.misc.DataConstraints;
import me.lucko.luckperms.common.util.Predicates;
import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.track.PromotionResult;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

public class UserPromote extends ChildCommand<User> {
    public UserPromote() {
        super(CommandSpec.USER_PROMOTE, "promote", CommandPermission.USER_PROMOTE, Predicates.alwaysFalse());
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, User target, ArgumentList args, String label) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        boolean addToFirst = !args.remove("--dont-add-to-first");

        // if args is empty - use the only/default track
        if (args.isEmpty()) {
            Set<String> tracks = plugin.getTrackManager().getAll().keySet();
            if (tracks.size() == 1) {
                args.add(tracks.iterator().next());
            } else if (tracks.contains("default")) {
                args.add("default");
            } else {
                Message.USER_TRACK_ERROR_AMBIGUOUS_TRACK_SELECTION.send(sender);
                return;
            }
        }

        final String trackName = args.get(0).toLowerCase(Locale.ROOT);
        if (!DataConstraints.TRACK_NAME_TEST.test(trackName)) {
            Message.TRACK_INVALID_ENTRY.send(sender, trackName);
            return;
        }

        Track track = StorageAssistant.loadTrack(trackName, sender, plugin);
        if (track == null) {
            return;
        }

        if (track.getSize() <= 1) {
            Message.TRACK_EMPTY.send(sender, track.getName());
            return;
        }

        boolean dontShowTrackProgress = args.remove("-s");
        MutableContextSet context = args.getContextOrDefault(1, plugin);

        if (ArgumentPermissions.checkContext(plugin, sender, getPermission().get(), context)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        Predicate<String> nextGroupPermissionChecker = s ->
                !ArgumentPermissions.checkArguments(plugin, sender, getPermission().get(), track.getName(), s) &&
                !ArgumentPermissions.checkGroup(plugin, sender, s, context);

        PromotionResult result = track.promote(target, context, nextGroupPermissionChecker, sender, addToFirst);
        switch (result.getStatus()) {
            case MALFORMED_TRACK:
                Message.USER_PROMOTE_ERROR_MALFORMED.send(sender, result.getGroupTo().get());
                return;
            case UNDEFINED_FAILURE:
                Message.COMMAND_NO_PERMISSION.send(sender);
                return;
            case AMBIGUOUS_CALL:
                Message.TRACK_AMBIGUOUS_CALL.send(sender, target);
                return;
            case END_OF_TRACK:
                Message.USER_PROMOTE_ERROR_ENDOFTRACK.send(sender, track.getName(), target);
                return;

            case ADDED_TO_FIRST_GROUP: {
                if (!addToFirst && !result.getGroupTo().isPresent()) {
                    Message.USER_PROMOTE_NOT_ON_TRACK.send(sender, target, track.getName());
                    return;
                }

                Message.USER_TRACK_ADDED_TO_FIRST.send(sender, target, track.getName(), result.getGroupTo().get(), context);

                LoggedAction.build().source(sender).target(target)
                        .description("promote", track.getName(), context)
                        .build().submit(plugin, sender);

                StorageAssistant.save(target, sender, plugin);
                return;
            }

            case SUCCESS: {
                String groupFrom = result.getGroupFrom().get();
                String groupTo = result.getGroupTo().get();

                Message.USER_PROMOTE_SUCCESS.send(sender, target, track.getName(), groupFrom, groupTo, context);
                if (!dontShowTrackProgress) {
                    Message.TRACK_PATH_HIGHLIGHTED_PROGRESSION.send(sender, track.getGroups(), groupFrom, groupTo, false);
                }

                LoggedAction.build().source(sender).target(target)
                        .description("promote", track.getName(), context)
                        .build().submit(plugin, sender);

                StorageAssistant.save(target, sender, plugin);
                return;
            }

            default:
                throw new AssertionError("Unknown status: " + result.getStatus());
        }
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.tracks(plugin))
                .from(1, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
