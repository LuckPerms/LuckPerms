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

import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.matcher.ConstraintNodeMatcher;
import me.lucko.luckperms.common.node.matcher.StandardNodeMatchers;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.webeditor.WebEditorRequest;
import me.lucko.luckperms.common.webeditor.WebEditorSession;
import net.luckperms.api.node.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HolderEditor<T extends PermissionHolder> extends ChildCommand<T> {
    public HolderEditor(HolderType type) {
        super(CommandSpec.HOLDER_EDITOR, "editor", type == HolderType.USER ? CommandPermission.USER_EDITOR : CommandPermission.GROUP_EDITOR, Predicates.alwaysFalse());
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, T target, ArgumentList args, String label) {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), target) || ArgumentPermissions.checkGroup(plugin, sender, target, ImmutableContextSetImpl.EMPTY)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        List<PermissionHolder> holders = new ArrayList<>();

        // also include users who are a member of the group
        if (target instanceof Group) {
            Group group = (Group) target;
            ConstraintNodeMatcher<Node> matcher = StandardNodeMatchers.key(Inheritance.key(group.getName()));
            WebEditorRequest.includeMatchingUsers(holders, matcher, true, plugin);
        }

        // include the original holder too
        holders.add(target);

        // remove holders which the sender doesn't have perms to view
        holders.removeIf(h -> ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), h) || ArgumentPermissions.checkGroup(plugin, sender, h, ImmutableContextSetImpl.EMPTY));

        // they don't have perms to view any of them
        if (holders.isEmpty()) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        Message.EDITOR_START.send(sender);

        WebEditorSession.create(holders, Collections.emptyList(), sender, label, plugin).open();
    }

}
