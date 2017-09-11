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

package me.lucko.luckperms.common.commands.impl.generic.parent;

import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.commands.ArgumentPermissions;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.CommandPermission;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.TextUtils;

import net.kyori.text.BuildableComponent;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.function.Consumer;

public class ParentInfo extends SharedSubCommand {
    public ParentInfo(LocaleManager locale) {
        super(CommandSpec.PARENT_INFO.spec(locale), "info", CommandPermission.USER_PARENT_INFO, CommandPermission.GROUP_PARENT_INFO, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, permission, holder)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        Component ent = permGroupsToMessage(holder.getOwnNodesSorted(), holder, label);
        Message.LISTNODES.send(sender, holder.getFriendlyName());
        sender.sendMessage(ent);

        Component tempEnt = tempGroupsToMessage(holder.getOwnNodesSorted(), holder, label);
        Message.LISTNODES_TEMP.send(sender, holder.getFriendlyName());
        sender.sendMessage(tempEnt);

        return CommandResult.SUCCESS;
    }

    private static Component permGroupsToMessage(SortedSet<LocalizedNode> nodes, PermissionHolder holder, String label) {
        List<Node> page = new ArrayList<>();
        for (Node node : nodes) {
            if (!node.isGroupNode()) continue;
            if (node.isTemporary()) continue;
            page.add(node);
        }

        if (page.isEmpty()) {
            return TextComponent.builder("None").color(TextColor.DARK_AQUA).build();
        }

        TextComponent.Builder message = TextComponent.builder("");
        for (Node node : page) {
            String s = "&3> &a" + node.getGroupName() + Util.getAppendableNodeContextString(node) + "\n";
            message.append(TextUtils.fromLegacy(s, Constants.FORMAT_CHAR).toBuilder().applyDeep(makeFancy(holder, label, node)).build());
        }
        return message.build();
    }

    private static Component tempGroupsToMessage(SortedSet<LocalizedNode> nodes, PermissionHolder holder, String label) {
        List<Node> page = new ArrayList<>();
        for (Node node : nodes) {
            if (!node.isGroupNode()) continue;
            if (node.isPermanent()) continue;
            page.add(node);
        }

        if (page.isEmpty()) {
            return TextComponent.builder("None").color(TextColor.DARK_AQUA).build();
        }

        TextComponent.Builder message = TextComponent.builder("");
        for (Node node : page) {
            String s = "&3> &a" + node.getPermission() + Util.getAppendableNodeContextString(node) + "\n&2-    expires in " + DateUtil.formatDateDiff(node.getExpiryUnixTime()) + "\n";
            message.append(TextUtils.fromLegacy(s, Constants.FORMAT_CHAR).toBuilder().applyDeep(makeFancy(holder, label, node)).build());
        }
        return message.build();
    }

    private static Consumer<BuildableComponent.Builder<? ,?>> makeFancy(PermissionHolder holder, String label, Node node) {
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextUtils.fromLegacy(TextUtils.joinNewline(
                "&3> &f" + node.getGroupName(),
                " ",
                "&7Click to remove this parent from " + holder.getFriendlyName()
        ), Constants.FORMAT_CHAR));

        boolean group = !(holder instanceof User);
        String command = NodeFactory.nodeAsCommand(node, group ? holder.getObjectName() : holder.getFriendlyName(), group, false)
                .replace("/luckperms", "/" + label);

        return component -> {
            component.hoverEvent(hoverEvent);
            component.clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        };
    }
}
