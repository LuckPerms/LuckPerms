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

package me.lucko.luckperms.common.commands.impl.misc;

import me.lucko.luckperms.api.caching.PermissionData;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SingleCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.treeview.TreeView;
import me.lucko.luckperms.common.treeview.TreeViewBuilder;
import me.lucko.luckperms.common.utils.Predicates;

import io.github.mkremins.fanciful.ChatColor;
import io.github.mkremins.fanciful.FancyMessage;

import java.util.List;
import java.util.UUID;

public class TreeCommand extends SingleCommand {
    public TreeCommand() {
        super("Tree", "Generate a tree view of permissions",
                "/%s tree [selection] [max level] [player]", Permission.TREE, Predicates.alwaysFalse(),
                Arg.list(
                        Arg.create("selection", false, "the root of the tree. specify \".\" to include all permissions"),
                        Arg.create("max level", false, "how many branch levels should be returned"),
                        Arg.create("player", false, "the name of an online player to check against")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) throws CommandException {
        String selection = ".";
        int maxLevel = 5;
        String player = null;

        if (args.size() > 0) {
            selection = args.get(0);
        }
        if (args.size() > 1) {
            maxLevel = ArgumentUtils.handleIntOrElse(1, args, 5);
        }
        if (args.size() > 2) {
            player = args.get(2);
        }

        if (player != null) {
            User user;
            UUID u = Util.parseUuid(player);
            if (u != null) {
                user = plugin.getUserManager().get(u);
            } else {
                user = plugin.getUserManager().getByUsername(player);
            }

            if (user == null) {
                Message.USER_NOT_ONLINE.send(sender, player);
                return CommandResult.STATE_ERROR;
            }

            PermissionData permissionData = user.getUserData().getPermissionData(plugin.getContextForUser(user));
            TreeView view = TreeViewBuilder.newBuilder().rootPosition(selection).maxLevels(maxLevel).build(plugin.getPermissionVault());

            if (!view.hasData()) {
                Message.TREE_EMPTY.send(sender);
                return CommandResult.FAILURE;
            }

            Message.TREE_UPLOAD_START.send(sender);

            String url = view.uploadPasteData(plugin.getVersion(), user.getFriendlyName(), permissionData);
            if (url == null) {
                url = "null";
            }

            Message.TREE_URL.send(sender);
            sender.sendMessage(new FancyMessage(url).color(ChatColor.getByChar('b')).link(url));
            return CommandResult.SUCCESS;
        }

        TreeView view = TreeViewBuilder.newBuilder().rootPosition(selection).maxLevels(maxLevel).build(plugin.getPermissionVault());

        if (!view.hasData()) {
            Message.TREE_EMPTY.send(sender);
            return CommandResult.FAILURE;
        }

        Message.TREE_UPLOAD_START.send(sender);

        String url = view.uploadPasteData(plugin.getVersion());
        if (url == null) {
            url = "null";
        }

        Message.TREE_URL.send(sender);
        sender.sendMessage(new FancyMessage(url).color(ChatColor.getByChar('b')).link(url));
        return CommandResult.SUCCESS;
    }
}
