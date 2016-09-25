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

package me.lucko.luckperms.commands.group;

import com.google.common.collect.ImmutableList;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.group.subcommands.*;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.groups.Group;

import java.util.ArrayList;
import java.util.List;

public class GroupMainCommand extends MainCommand<Group> {
    public GroupMainCommand() {
        super("Group", "/%s group <group>", 2, ImmutableList.<SubCommand<Group>>builder()
            .add(new GroupInfo())
            .add(new GroupListNodes())
            .add(new GroupListParents())
            .add(new GroupHasPerm())
            .add(new GroupInheritsPerm())
            .add(new GroupSetPermission())
            .add(new GroupUnSetPermission())
            .add(new GroupSetInherit())
            .add(new GroupUnsetInherit())
            .add(new GroupSetTempPermission())
            .add(new GroupUnsetTempPermission())
            .add(new GroupSetTempInherit())
            .add(new GroupUnsetTempInherit())
            .add(new GroupShowTracks())
            .add(new GroupChatMeta())
            .add(new GroupAddPrefix())
            .add(new GroupAddSuffix())
            .add(new GroupRemovePrefix())
            .add(new GroupRemoveSuffix())
            .add(new GroupAddTempPrefix())
            .add(new GroupAddTempSuffix())
            .add(new GroupRemoveTempPrefix())
            .add(new GroupRemoveTempSuffix())
            .add(new GroupBulkChange())
            .add(new GroupClear())
            .add(new GroupRename())
            .build()
        );
    }

    @Override
    protected Group getTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        if (!plugin.getDatastore().loadGroup(target)) {
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
    protected List<String> getObjects(LuckPermsPlugin plugin) {
        return new ArrayList<>(plugin.getGroupManager().getAll().keySet());
    }
}
