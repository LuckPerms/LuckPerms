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

package me.lucko.luckperms.common.commands.misc;

import me.lucko.luckperms.common.cacheddata.type.PermissionCache;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.treeview.TreeView;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.util.Uuids;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;

import java.util.List;
import java.util.UUID;

public class TreeCommand extends SingleCommand {
    public TreeCommand(LocaleManager locale) {
        super(CommandSpec.TREE.localize(locale), "Tree", CommandPermission.TREE, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        String selection = ".";
        String player = null;

        if (!args.isEmpty()) {
            selection = args.get(0);
        }
        if (args.size() > 1) {
            player = args.get(1);
        }

        User user;
        if (player != null) {
            UUID u = Uuids.parse(player);
            if (u != null) {
                user = plugin.getUserManager().getIfLoaded(u);
            } else {
                user = plugin.getUserManager().getByUsername(player);
            }

            if (user == null) {
                Message.USER_NOT_ONLINE.send(sender, player);
                return CommandResult.STATE_ERROR;
            }
        } else {
            user = null;
        }

        TreeView view = new TreeView(plugin.getPermissionRegistry(), selection);
        if (!view.hasData()) {
            Message.TREE_EMPTY.send(sender);
            return CommandResult.FAILURE;
        }

        Message.TREE_UPLOAD_START.send(sender);
        PermissionCache permissionData = user == null ? null : user.getCachedData().getPermissionData(plugin.getQueryOptionsForUser(user).orElse(plugin.getContextManager().getStaticQueryOptions()));
        String id = view.uploadPasteData(plugin.getBytebin(), sender, user, permissionData);
        String url = plugin.getConfiguration().get(ConfigKeys.TREE_VIEWER_URL_PATTERN) + "#" + id;

        Message.TREE_URL.send(sender);

        Component message = TextComponent.builder(url).color(TextColor.AQUA)
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(TextComponent.of("Click to open the tree view.").color(TextColor.GRAY)))
                .build();

        sender.sendMessage(message);
        return CommandResult.SUCCESS;
    }
}
