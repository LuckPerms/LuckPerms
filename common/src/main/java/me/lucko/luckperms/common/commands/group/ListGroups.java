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

package me.lucko.luckperms.common.commands.group;

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;

import java.util.List;
import java.util.stream.Collectors;

public class ListGroups extends SingleCommand {
    public ListGroups() {
        super(CommandSpec.LIST_GROUPS, "ListGroups", CommandPermission.LIST_GROUPS, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        try {
            plugin.getStorage().loadAllGroups().get();
        } catch (Exception e) {
            plugin.getLogger().warn("Error whilst loading groups", e);
            Message.GROUPS_LOAD_ERROR.send(sender);
            return CommandResult.LOADING_ERROR;
        }

        Message.GROUPS_LIST.send(sender);
        plugin.getGroupManager().getAll().values().stream()
                .sorted((o1, o2) -> {
                    int i = Integer.compare(o2.getWeight().orElse(0), o1.getWeight().orElse(0));
                    return i != 0 ? i : o1.getName().compareToIgnoreCase(o2.getName());
                })
                .forEach(group -> {
                    List<String> tracks = plugin.getTrackManager().getAll().values().stream().filter(t -> t.containsGroup(group)).map(Track::getName).collect(Collectors.toList());
                    Message.GROUPS_LIST_ENTRY.send(sender, group, group.getWeight().orElse(0), tracks);
                });

        return CommandResult.SUCCESS;
    }
}
