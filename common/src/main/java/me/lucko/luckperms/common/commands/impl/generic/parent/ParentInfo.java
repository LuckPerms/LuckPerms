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
import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.commands.utils.SortMode;
import me.lucko.luckperms.common.commands.utils.SortType;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.node.NodeWithContextComparator;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.CollationKeyCache;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.TextUtils;

import net.kyori.text.BuildableComponent;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class ParentInfo extends SharedSubCommand {
    public ParentInfo(LocaleManager locale) {
        super(CommandSpec.PARENT_INFO.spec(locale), "info", CommandPermission.USER_PARENT_INFO, CommandPermission.GROUP_PARENT_INFO, Predicates.notInRange(0, 2));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, permission, holder)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        int page = ArgumentUtils.handleIntOrElse(0, args, 1);
        SortMode sortMode = SortMode.determine(args);

        // get the holders nodes
        List<LocalizedNode> nodes = new ArrayList<>(holder.getOwnNodesSorted());

        // remove irrelevant types (these are displayed in the other info commands)
        nodes.removeIf(node -> !node.isGroupNode() || !node.getValuePrimitive());

        // handle empty
        if (nodes.isEmpty()) {
            Message.PARENT_INFO_NO_DATA.send(sender, holder.getFriendlyName());
            return CommandResult.SUCCESS;
        }

        // sort the list alphabetically instead
        if (sortMode.getType() == SortType.ALPHABETICALLY) {
            nodes.sort(ALPHABETICAL_NODE_COMPARATOR);
        }

        // reverse the order if necessary
        if (!sortMode.isAscending()) {
            Collections.reverse(nodes);
        }

        int pageIndex = page - 1;
        List<List<LocalizedNode>> pages = CommandUtils.divideList(nodes, 19);

        if (pageIndex < 0 || pageIndex >= pages.size()) {
            page = 1;
            pageIndex = 0;
        }

        List<LocalizedNode> content = pages.get(pageIndex);

        // send header
        Message.PARENT_INFO.send(sender, holder.getFriendlyName(), page, pages.size(), nodes.size());

        // send content
        for (LocalizedNode node : content) {
            String s = "&3> &a" + node.getGroupName() + CommandUtils.getAppendableNodeContextString(node);
            if (node.isTemporary()) {
                s += "\n&2  expires in " + DateUtil.formatDateDiff(node.getExpiryUnixTime());
            }

            TextComponent message = TextUtils.fromLegacy(s, CommandManager.AMPERSAND_CHAR).toBuilder().applyDeep(makeFancy(holder, label, node)).build();
            sender.sendMessage(message);
        }

        return CommandResult.SUCCESS;
    }

    private static final Comparator<LocalizedNode> ALPHABETICAL_NODE_COMPARATOR = (o1, o2) -> {
        int i = CollationKeyCache.compareStrings(o1.getGroupName(), o2.getGroupName());
        if (i != 0) {
            return i;
        }

        // fallback to priority
        return NodeWithContextComparator.reverse().compare(o1, o2);
    };

    private static Consumer<BuildableComponent.Builder<? ,?>> makeFancy(PermissionHolder holder, String label, Node node) {
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextUtils.fromLegacy(TextUtils.joinNewline(
                "&3> &f" + node.getGroupName(),
                " ",
                "&7Click to remove this parent from " + holder.getFriendlyName()
        ), CommandManager.AMPERSAND_CHAR));

        String command = "/" + label + " " + NodeFactory.nodeAsCommand(node, holder.getType().isGroup() ? holder.getObjectName() : holder.getFriendlyName(), holder.getType(), false);
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command);

        return component -> {
            component.hoverEvent(hoverEvent);
            component.clickEvent(clickEvent);
        };
    }
}
