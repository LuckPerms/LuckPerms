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

import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentException;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.node.types.DisplayName;
import me.lucko.luckperms.common.node.types.Weight;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.storage.misc.DataConstraints;
import me.lucko.luckperms.common.util.Predicates;
import net.kyori.adventure.text.Component;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.event.cause.CreationCause;
import net.luckperms.api.model.data.DataType;

import java.util.Locale;

public class CreateGroup extends SingleCommand {
    public CreateGroup() {
        super(CommandSpec.CREATE_GROUP, "CreateGroup", CommandPermission.CREATE_GROUP, Predicates.notInRange(1, 3));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        if (args.isEmpty()) {
            sendUsage(sender, label);
            return;
        }

        String groupName = args.get(0).toLowerCase(Locale.ROOT);
        if (!DataConstraints.GROUP_NAME_TEST.test(groupName)) {
            Message.GROUP_INVALID_ENTRY.send(sender, groupName);
            return;
        }

        if (plugin.getStorage().loadGroup(groupName).join().isPresent()) {
            Message.ALREADY_EXISTS.send(sender, groupName);
            return;
        }

        Integer weight = null;
        try {
            weight = args.getPriority(1);
        } catch (ArgumentException | IndexOutOfBoundsException e) {
            // ignored
        }

        String displayName = null;
        try {
            displayName = args.get(weight != null ? 2 : 1);
        } catch (IndexOutOfBoundsException e) {
            // ignored
        }

        try {
            Group group = plugin.getStorage().createAndLoadGroup(groupName, CreationCause.COMMAND).get();

            if (weight != null) {
                group.setNode(DataType.NORMAL, Weight.builder(weight).build(), false);
            }

            if (displayName != null) {
                group.setNode(DataType.NORMAL, DisplayName.builder(displayName).build(), false);
            }

            StorageAssistant.save(group, sender, plugin);
        } catch (Exception e) {
            plugin.getLogger().warn("Error whilst creating group", e);
            Message.CREATE_ERROR.send(sender, Component.text(groupName));
            return;
        }

        Message.CREATE_SUCCESS.send(sender, Component.text(groupName));

        LoggedAction.build().source(sender).targetName(groupName).targetType(Action.Target.Type.GROUP)
                .description("create")
                .build().submit(plugin, sender);
    }
}
