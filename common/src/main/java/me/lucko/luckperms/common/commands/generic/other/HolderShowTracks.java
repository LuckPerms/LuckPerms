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

package me.lucko.luckperms.common.commands.generic.other;

import com.google.common.collect.Maps;

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;

import net.kyori.adventure.text.Component;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HolderShowTracks<T extends PermissionHolder> extends ChildCommand<T> {
    public HolderShowTracks(HolderType type) {
        super(CommandSpec.HOLDER_SHOWTRACKS, "showtracks", type == HolderType.USER ? CommandPermission.USER_SHOW_TRACKS : CommandPermission.GROUP_SHOW_TRACKS, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, T target, ArgumentList args, String label) {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        try {
            plugin.getStorage().loadAllTracks().get();
        } catch (Exception e) {
            e.printStackTrace();
            Message.TRACKS_LOAD_ERROR.send(sender);
            return CommandResult.LOADING_ERROR;
        }

        List<Map.Entry<Track, Component>> lines = new ArrayList<>();

        if (target.getType() == HolderType.USER) {
            // if the holder is a user, we want to query parent groups for tracks
            Set<InheritanceNode> nodes = target.normalData().inheritanceAsList().stream()
                    .filter(Node::getValue)
                    .filter(n -> !n.hasExpiry())
                    .collect(Collectors.toSet());

            for (InheritanceNode node : nodes) {
                String groupName = node.getGroupName();
                List<Track> tracks = plugin.getTrackManager().getAll().values().stream()
                        .filter(t -> t.containsGroup(groupName))
                        .collect(Collectors.toList());

                for (Track track : tracks) {
                    Component line = Component.text()
                            .append(Message.formatContextSetBracketed(node.getContexts(), Component.empty()))
                            .append(Component.newline())
                            .append(Message.formatTrackPath(track.getGroups(), groupName))
                            .build();

                    lines.add(Maps.immutableEntry(track, line));
                }
            }
        } else {
            // otherwise, just lookup for the actual group
            String groupName = ((Group) target).getName();
            List<Track> tracks = plugin.getTrackManager().getAll().values().stream()
                    .filter(t -> t.containsGroup(groupName))
                    .collect(Collectors.toList());

            for (Track track : tracks) {
                lines.add(Maps.immutableEntry(track, Message.formatTrackPath(track.getGroups(), groupName)));
            }
        }

        if (lines.isEmpty()) {
            Message.LIST_TRACKS_EMPTY.send(sender, target.getFormattedDisplayName());
            return CommandResult.SUCCESS;
        }

        Message.LIST_TRACKS.send(sender, target.getFormattedDisplayName());
        for (Map.Entry<Track, Component> line : lines) {
            Message.LIST_TRACKS_ENTRY.send(sender, line.getKey().getName(), line.getValue());
        }
        return CommandResult.SUCCESS;
    }
}
