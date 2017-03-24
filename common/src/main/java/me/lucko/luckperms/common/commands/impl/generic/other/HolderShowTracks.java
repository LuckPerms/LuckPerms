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

package me.lucko.luckperms.common.commands.impl.generic.other;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HolderShowTracks<T extends PermissionHolder> extends SubCommand<T> {
    public HolderShowTracks(boolean user) {
        super("showtracks", "Lists the tracks that the object is on",
                user ? Permission.USER_SHOWTRACKS : Permission.GROUP_SHOWTRACKS, Predicates.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, T holder, List<String> args, String label) throws CommandException {
        if (!plugin.getStorage().loadAllTracks().join()) {
            Message.TRACKS_LOAD_ERROR.send(sender);
            return CommandResult.LOADING_ERROR;
        }

        Set<Node> nodes = holder.getNodes().values().stream()
                .filter(Node::isGroupNode)
                .filter(Node::isPermanent)
                .collect(Collectors.toSet());

        StringBuilder sb = new StringBuilder();

        for (Node node : nodes) {
            String name = node.getGroupName();

            plugin.getTrackManager().getAll().values().stream()
                    .filter(t -> t.containsGroup(name))
                    .forEach(t -> sb.append("&a")
                            .append(t.getName())
                            .append(": ")
                            .append(Util.listToArrowSep(t.getGroups(), name))
                            .append(Util.getNodeContextDescription(node))
                            .append("\n")
                    );
        }

        if (sb.length() == 0) {
            Message.LIST_TRACKS_EMPTY.send(sender, holder.getFriendlyName());
            return CommandResult.SUCCESS;
        } else {
            sb.deleteCharAt(sb.length() - 1);
            Message.LIST_TRACKS.send(sender, holder.getFriendlyName(), sb.toString());
            return CommandResult.SUCCESS;
        }
    }
}
