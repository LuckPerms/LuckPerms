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

package me.lucko.luckperms.common.commands.impl.generic.other;

import com.google.common.collect.Maps;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.commands.ArgumentPermissions;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HolderShowTracks<T extends PermissionHolder> extends SubCommand<T> {
    public HolderShowTracks(LocaleManager locale, boolean user) {
        super(CommandSpec.HOLDER_SHOWTRACKS.spec(locale), "showtracks", user ? CommandPermission.USER_SHOW_TRACKS : CommandPermission.GROUP_SHOW_TRACKS, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, T holder, List<String> args, String label) throws CommandException {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), holder)) {
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

        Set<Node> nodes = holder.getEnduringNodes().values().stream()
                .filter(Node::isGroupNode)
                .filter(Node::getValuePrimitive)
                .filter(Node::isPermanent)
                .collect(Collectors.toSet());

        List<Map.Entry<Track, String>> lines = new ArrayList<>();

        for (Node node : nodes) {
            String name = node.getGroupName();
            List<Track> tracks = plugin.getTrackManager().getAll().values().stream()
                    .filter(t -> t.containsGroup(name))
                    .collect(Collectors.toList());

            for (Track t : tracks) {
                lines.add(Maps.immutableEntry(t, CommandUtils.listToArrowSep(t.getGroups(), name) + CommandUtils.getAppendableNodeContextString(node)));
            }
        }

        if (lines.isEmpty()) {
            Message.LIST_TRACKS_EMPTY.send(sender, holder.getFriendlyName());
            return CommandResult.SUCCESS;
        }

        Message.LIST_TRACKS.send(sender, holder.getFriendlyName());
        for (Map.Entry<Track, String> line : lines) {
            Message.LIST_TRACKS_ENTRY.send(sender, line.getKey().getName(), line.getValue());
        }
        return CommandResult.SUCCESS;
    }
}
