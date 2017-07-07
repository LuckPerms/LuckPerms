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

package me.lucko.luckperms.common.commands.impl.generic.permission;

import com.google.common.collect.Maps;

import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.commands.ArgumentPermissions;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.TextUtils;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.serializer.ComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.function.Consumer;

public class PermissionInfo extends SharedSubCommand {
    public PermissionInfo(LocaleManager locale) {
        super(CommandSpec.PERMISSION_INFO.spec(locale), "info", Permission.USER_PERM_INFO, Permission.GROUP_PERM_INFO, Predicates.notInRange(0, 2));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label, Permission permission) throws CommandException {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, permission, holder)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        String filter = null;
        if (args.size() == 1) {
            // it might be a filter, if it's a number, then it relates to a page.
            try {
                Integer.parseInt(args.get(0));
            } catch (NumberFormatException e) {
                // it's not a number, so assume it's the filter.
                filter = args.get(0);
            }
        } else if (args.size() == 2) {
            filter = args.get(1);
        }

        if (sender.getUuid().equals(Constants.CONSOLE_UUID)) {
            Message.LISTNODES.send(sender, holder.getFriendlyName());
            sender.sendMessage(Util.color(permNodesToString(filter, holder.mergePermissionsToSortedSet())));
        } else {
            int page = ArgumentUtils.handleIntOrElse(0, args, 1);

            Map.Entry<Component, String> ent = permNodesToMessage(filter, holder.mergePermissionsToSortedSet(), holder, label, page);
            if (ent.getValue() != null) {
                Message.LISTNODES_WITH_PAGE.send(sender, holder.getFriendlyName(), ent.getValue());
                sender.sendMessage(ent.getKey());
            } else {
                Message.LISTNODES.send(sender, holder.getFriendlyName());
                sender.sendMessage(ent.getKey());
            }
        }

        Message.LISTNODES_TEMP.send(sender, holder.getFriendlyName(), tempNodesToString(filter, holder.mergePermissionsToSortedSet()));
        return CommandResult.SUCCESS;
    }

    private static Map.Entry<Component, String> permNodesToMessage(String filter, SortedSet<LocalizedNode> nodes, PermissionHolder holder, String label, int pageNumber) {
        List<Node> l = new ArrayList<>();
        for (Node node : nodes) {
            if (filter != null && !node.getPermission().startsWith(filter)) {
                continue;
            }
            if (node.isTemporary()) {
                continue;
            }

            l.add(node);
        }

        if (l.isEmpty()) {
            return Maps.immutableEntry(new TextComponent("None").color('3'), null);
        }

        int index = pageNumber - 1;
        List<List<Node>> pages = Util.divideList(l, 15);

        if (index < 0 || index >= pages.size()) {
            pageNumber = 1;
            index = 0;
        }

        List<Node> page = pages.get(index);

        TextComponent message = new TextComponent("");
        String title = "&7(showing page &f" + pageNumber + "&7 of &f" + pages.size() + "&7 - &f" + nodes.size() + "&7 entries";
        if (filter != null) {
            title += " - filtered by &f\"" + filter + "\"&7)";
        } else {
            title += ")";
        }

        for (Node node : page) {
            String s = "&3> " + (node.getValue() ? "&a" : "&c") + node.getPermission() + Util.getAppendableNodeContextString(node) + "\n";
            message.append(ComponentSerializer.parseFromLegacy(s, Constants.FORMAT_CHAR).applyRecursively(makeFancy(holder, label, node)));
        }

        return Maps.immutableEntry(message, title);
    }

    private static Consumer<Component> makeFancy(PermissionHolder holder, String label, Node node) {
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentSerializer.parseFromLegacy(TextUtils.joinNewline(
                "&3> " + (node.getValue() ? "&a" : "&c") + node.getPermission(),
                " ",
                "&7Click to remove this node from " + holder.getFriendlyName()
        ), Constants.FORMAT_CHAR));

        boolean group = !(holder instanceof User);
        String command = NodeFactory.nodeAsCommand(node, group ? holder.getObjectName() : holder.getFriendlyName(), group)
                .replace("/luckperms", "/" + label)
                .replace("permission set", "permission unset")
                .replace("parent add", "parent remove")
                .replace(" true", "")
                .replace(" false", "");

        return component -> {
            component.hoverEvent(hoverEvent);
            component.clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        };
    }

    private static String permNodesToString(String filter, SortedSet<LocalizedNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (Node node : nodes) {
            if (filter != null && !node.getPermission().startsWith(filter)) {
                continue;
            }
            if (node.isTemporary()) {
                continue;
            }

            sb.append("&3> ")
                    .append(node.getValue() ? "&a" : "&c")
                    .append(node.getPermission())
                    .append(" ").append("&7(").append(node.getValue()).append("&7)")
                    .append(Util.getAppendableNodeContextString(node))
                    .append("\n");
        }
        return sb.length() == 0 ? "&3None" : sb.toString();
    }

    private static String tempNodesToString(String filter, SortedSet<LocalizedNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (Node node : nodes) {
            if (filter != null && !node.getPermission().startsWith(filter)) {
                continue;
            }
            if (!node.isTemporary()) {
                continue;
            }

            sb.append("&3> ")
                    .append(node.getValue() ? "&a" : "&c")
                    .append(node.getPermission())
                    .append(Util.getAppendableNodeContextString(node))
                    .append("\n&2-    expires in ")
                    .append(DateUtil.formatDateDiff(node.getExpiryUnixTime()))
                    .append("\n");
        }
        return sb.length() == 0 ? "&3None" : sb.toString();
    }
}
