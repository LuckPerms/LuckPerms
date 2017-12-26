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

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.actionlog.ExtendedLogEntry;
import me.lucko.luckperms.common.commands.ArgumentPermissions;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.commands.abstraction.SingleCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.NodeModel;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.webeditor.WebEditorUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ApplyEditsCommand extends SingleCommand {
    public ApplyEditsCommand(LocaleManager locale) {
        super(CommandSpec.APPLY_EDITS.spec(locale), "ApplyEdits", CommandPermission.APPLY_EDITS, Predicates.notInRange(1, 2));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) throws CommandException {
        String code = args.get(0);
        String who = args.size() == 2 ? args.get(1) : null;

        if (code.isEmpty()) {
            Message.APPLY_EDITS_INVALID_CODE.send(sender, code);
            return CommandResult.INVALID_ARGS;
        }

        JsonObject data = WebEditorUtils.getDataFromGist(code);
        if (data == null) {
            Message.APPLY_EDITS_UNABLE_TO_READ.send(sender, code);
            return CommandResult.FAILURE;
        }

        if (who == null) {
            if (!data.has("who") || data.get("who").getAsString().isEmpty()) {
                Message.APPLY_EDITS_NO_TARGET.send(sender);
                return CommandResult.STATE_ERROR;
            }

            who = data.get("who").getAsString();
        }

        PermissionHolder holder = WebEditorUtils.getHolderFromIdentifier(plugin, sender, who);
        if (holder == null) {
            // the #getHolderFromIdentifier method will send the error message onto the sender
            return CommandResult.STATE_ERROR;
        }

        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), holder)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        ExtendedLogEntry.build().actor(sender).acted(holder)
                .action("applyedits", code)
                .build().submit(plugin, sender);

        Set<NodeModel> rawNodes = WebEditorUtils.deserializePermissions(data.getAsJsonArray("nodes"));

        Set<Node> before = new HashSet<>(holder.getEnduringNodes().values());
        Set<Node> nodes = rawNodes.stream().map(NodeModel::toNode).collect(Collectors.toSet());
        holder.setEnduringNodes(nodes);

        Map.Entry<Set<Node>, Set<Node>> diff = diff(before, nodes);
        int additions = diff.getKey().size();
        int deletions = diff.getValue().size();
        String additionsSummary = "addition" + (additions == 1 ? "" : "s");
        String deletionsSummary = "deletion" + (deletions == 1 ? "" : "s");

        Message.APPLY_EDITS_SUCCESS.send(sender, holder.getFriendlyName());
        Message.APPLY_EDITS_SUCCESS_SUMMARY.send(sender, additions, additionsSummary, deletions, deletionsSummary);
        for (Node n : diff.getKey()) {
            Message.APPLY_EDITS_DIFF_ADDED.send(sender, formatNode(n));
        }
        for (Node n : diff.getValue()) {
            Message.APPLY_EDITS_DIFF_REMOVED.send(sender, formatNode(n));
        }

        SharedSubCommand.save(holder, sender, plugin);
        return CommandResult.SUCCESS;
    }

    private static String formatNode(Node n) {
        return n.getPermission() + " &7(" + (n.getValuePrimitive() ? "&a" : "&c") + n.getValuePrimitive() + "&7)" + CommandUtils.getAppendableNodeContextString(n) +
                (n.isTemporary() ? " &7(" + DateUtil.formatDateDiffShort(n.getExpiryUnixTime()) + ")" : "");
    }

    private static Map.Entry<Set<Node>, Set<Node>> diff(Set<Node> before, Set<Node> after) {
        // entries in before but not after are being removed
        // entries in after but not before are being added

        Set<Node> added = new HashSet<>(after);
        added.removeAll(before);

        Set<Node> removed = new HashSet<>(before);
        removed.removeAll(after);

        return Maps.immutableEntry(added, removed);
    }

    @Override
    public boolean shouldDisplay() {
        return false;
    }
}
