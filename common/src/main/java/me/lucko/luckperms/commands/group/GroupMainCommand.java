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

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.Patterns;

import java.util.ArrayList;
import java.util.List;

public class GroupMainCommand extends MainCommand<Group> {
    public GroupMainCommand() {
        super("Group", "/%s group <group>", 2);
    }

    @Override
    protected void getTarget(String target, LuckPermsPlugin plugin, Sender sender, Callback<Group> onSuccess) {
        if (Patterns.NON_ALPHA_NUMERIC.matcher(target).find()) {
            Message.GROUP_INVALID_ENTRY.send(sender);
            return;
        }

        plugin.getDatastore().loadGroup(target, success -> {
            if (!success) {
                Message.GROUP_NOT_FOUND.send(sender);
                return;
            }

            Group group = plugin.getGroupManager().getGroup(target);
            if (group == null) {
                Message.GROUP_NOT_FOUND.send(sender);
                return;
            }

            onSuccess.onComplete(group);
        });
    }

    @Override
    protected List<String> getObjects(LuckPermsPlugin plugin) {
        return new ArrayList<>(plugin.getGroupManager().getGroups().keySet());
    }
}
