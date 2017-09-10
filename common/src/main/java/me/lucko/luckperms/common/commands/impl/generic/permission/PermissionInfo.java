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
import java.util.Map;
import java.util.SortedSet;
import java.util.function.Consumer;

public class PermissionInfo extends SharedSubCommand {
    public PermissionInfo(LocaleManager locale) {
        super(CommandSpec.PERMISSION_INFO.spec(locale), "info", CommandPermission.USER_PERM_INFO, CommandPermission.GROUP_PERM_INFO, Predicates.notInRange(0, 2));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label, CommandPermission permission) throws CommandException {
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

        int page = ArgumentUtils.handleIntOrElse(0, args, 1);

        Map.Entry<Component, String> ent = nodesToMessage(false, filter, holder.getOwnNodesSorted(), holder, label, page, sender.isConsole());
        if (ent.getValue() != null) {
            Message.LISTNODES_WITH_PAGE.send(sender, holder.getFriendlyName(), ent.getValue());
            sender.sendMessage(ent.getKey());
        } else {
            Message.LISTNODES.send(sender, holder.getFriendlyName());
            sender.sendMessage(ent.getKey());
        }

        Map.Entry<Component, String> tempEnt = nodesToMessage(true, filter, holder.getOwnNodesSorted(), holder, label, page, sender.isConsole());
        if (tempEnt.getValue() != null) {
            Message.LISTNODES_TEMP_WITH_PAGE.send(sender, holder.getFriendlyName(), tempEnt.getValue());
            sender.sendMessage(tempEnt.getKey());
        } else {
            Message.LISTNODES_TEMP.send(sender, holder.getFriendlyName());
            sender.sendMessage(tempEnt.getKey());
        }

        return CommandResult.SUCCESS;
    }

    private static Map.Entry<Component, String> nodesToMessage(boolean temp, String filter, SortedSet<LocalizedNode> nodes, PermissionHolder holder, String label, int pageNumber, boolean console) {
        // parse the filter
        String nodeFilter = null;
        Map.Entry<String, String> contextFilter = null;

        if (filter != null) {
            int index = filter.indexOf('=');

            context:
            if (index != -1) {
                String key = filter.substring(0, index);
                if (key.equals("")) {
                    break context;
                }

                String value = filter.substring(index + 1);
                if (value.equals("")) {
                    break context;
                }

                contextFilter = Maps.immutableEntry(key, value);
            }

            if (contextFilter == null) {
                nodeFilter = filter;
            }
        }

        List<Node> l = new ArrayList<>();
        for (Node node : nodes) {
            if (nodeFilter != null && !node.getPermission().startsWith(nodeFilter)) {
                continue;
            }
            if (contextFilter != null && !node.getFullContexts().hasIgnoreCase(contextFilter.getKey(), contextFilter.getValue())) {
                continue;
            }
            if (temp != node.isTemporary()) continue;
            l.add(node);
        }

        if (l.isEmpty()) {
            return Maps.immutableEntry(TextComponent.builder("None").color(TextColor.DARK_AQUA).build(), null);
        }

        int index = pageNumber - 1;
        List<List<Node>> pages = Util.divideList(l, 15);

        if (index < 0 || index >= pages.size()) {
            pageNumber = 1;
            index = 0;
        }

        List<Node> page = pages.get(index);

        TextComponent.Builder message = TextComponent.builder("");
        String title = "&7(showing page &f" + pageNumber + "&7 of &f" + pages.size() + "&7 - &f" + l.size() + "&7 entries";
        if (filter != null) {
            title += " - filtered by &f\"" + filter + "\"&7)";
        } else {
            title += ")";
        }

        for (Node node : page) {
            String s = "&3> " + (node.getValuePrimitive() ? "&a" : "&c") + node.getPermission() + (console ? " &7(" + node.getValuePrimitive() + "&7)" : "") + Util.getAppendableNodeContextString(node) + "\n";
            if (temp) {
                s += "&2-    expires in " + DateUtil.formatDateDiff(node.getExpiryUnixTime()) + "\n";
            }

            message.append(TextUtils.fromLegacy(s, Constants.FORMAT_CHAR).toBuilder().applyDeep(makeFancy(holder, label, node)).build());
        }

        return Maps.immutableEntry(message.build(), title);
    }

    private static Consumer<BuildableComponent.Builder<?, ?>> makeFancy(PermissionHolder holder, String label, Node node) {
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextUtils.fromLegacy(TextUtils.joinNewline(
                "¥3> " + (node.getValuePrimitive() ? "¥a" : "¥c") + node.getPermission(),
                " ",
                "¥7Click to remove this node from " + holder.getFriendlyName()
        ), '¥'));

        boolean group = !(holder instanceof User);
        String command = NodeFactory.nodeAsCommand(node, group ? holder.getObjectName() : holder.getFriendlyName(), group, false)
                .replace("/luckperms", "/" + label);

        return component -> {
            component.hoverEvent(hoverEvent);
            component.clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        };
    }
}
