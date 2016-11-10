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

package me.lucko.luckperms.common.commands.group;

import com.google.common.collect.ImmutableList;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Command;
import me.lucko.luckperms.common.commands.MainCommand;
import me.lucko.luckperms.common.commands.generic.meta.CommandMeta;
import me.lucko.luckperms.common.commands.generic.other.HolderShowTracks;
import me.lucko.luckperms.common.commands.generic.parent.CommandParent;
import me.lucko.luckperms.common.commands.generic.permission.CommandPermission;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.groups.Group;

import java.util.ArrayList;
import java.util.List;

public class GroupMainCommand extends MainCommand<Group> {
    public GroupMainCommand() {
        super("Group", "Group commands", "/%s group <group>", 2, ImmutableList.<Command<Group, ?>>builder()
                .add(new GroupInfo())
                .add(new CommandPermission<>(false))
                .add(new CommandParent<>(false))
                .add(new CommandMeta<>(false))
                .add(new HolderShowTracks<>(false))
                .add(new GroupBulkChange())
                .add(new GroupClear())
                .add(new GroupRename())
                .add(new GroupClone())
                .build()
        );
    }

    @Override
    protected Group getTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        if (!plugin.getDatastore().loadGroup(target).getUnchecked()) {
            Message.GROUP_NOT_FOUND.send(sender);
            return null;
        }

        Group group = plugin.getGroupManager().get(target);

        if (group == null) {
            Message.GROUP_NOT_FOUND.send(sender);
        }

        return group;
    }

    @Override
    protected void cleanup(Group group, LuckPermsPlugin plugin) {

    }

    @Override
    protected List<String> getTargets(LuckPermsPlugin plugin) {
        return new ArrayList<>(plugin.getGroupManager().getAll().keySet());
    }
}
