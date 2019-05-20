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

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.lucko.luckperms.api.node.Node;
import me.lucko.luckperms.common.actionlog.ExtendedLogEntry;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.utils.MessageUtils;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.NodeMapType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.model.NodeDataContainer;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.DurationFormatter;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.web.WebEditor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ApplyEditsCommand extends SingleCommand {
    public ApplyEditsCommand(LocaleManager locale) {
        super(CommandSpec.APPLY_EDITS.localize(locale), "ApplyEdits", CommandPermission.APPLY_EDITS, Predicates.notInRange(1, 2));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        String code = args.get(0);

        if (code.isEmpty()) {
            Message.APPLY_EDITS_INVALID_CODE.send(sender, code);
            return CommandResult.INVALID_ARGS;
        }

        JsonObject data = WebEditor.readDataFromBytebin(plugin.getBytebin(), code);
        if (data == null) {
            Message.APPLY_EDITS_UNABLE_TO_READ.send(sender, code);
            return CommandResult.FAILURE;
        }

        boolean success = false;

        if (data.has("tabs") && data.get("tabs").isJsonArray()) {
            JsonArray rows = data.get("tabs").getAsJsonArray();
            for (JsonElement row : rows) {
                if (read(row.getAsJsonObject(), sender, plugin)) {
                    success = true;
                }
            }
        } else {
            success = read(data, sender, plugin);
        }

        if (!success) {
            Message.APPLY_EDITS_TARGET_NO_CHANGES_PRESENT.send(sender);
        }

        return CommandResult.SUCCESS;
    }

    private boolean read(JsonObject data, Sender sender, LuckPermsPlugin plugin) {
        if (!data.has("who") || data.get("who").getAsString().isEmpty()) {
            Message.APPLY_EDITS_NO_TARGET.send(sender);
            return false;
        }

        String who = data.get("who").getAsString();
        PermissionHolder holder = WebEditor.getHolderFromIdentifier(plugin, sender, who);
        if (holder == null) {
            // the #getHolderFromIdentifier method will send the error message onto the sender
            return false;
        }

        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), holder)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return false;
        }

        Set<NodeDataContainer> nodes = WebEditor.deserializePermissions(data.getAsJsonArray("nodes"));

        Set<Node> before = new HashSet<>(holder.enduringData().immutable().values());
        Set<Node> after = nodes.stream().map(NodeDataContainer::toNode).collect(Collectors.toSet());

        Map.Entry<Set<Node>, Set<Node>> diff = diff(before, after);
        Set<Node> diffAdded = diff.getKey();
        Set<Node> diffRemoved = diff.getValue();

        int additions = diffAdded.size();
        int deletions = diffRemoved.size();

        if (additions == 0 && deletions == 0) {
            return false;
        }

        holder.setNodes(NodeMapType.ENDURING, after);

        for (Node n : diffAdded) {
            ExtendedLogEntry.build().actor(sender).acted(holder)
                    .action("webeditor", "add", n.getKey(), n.getValue(), n.getContexts())
                    .build().submit(plugin, sender);
        }
        for (Node n : diffRemoved) {
            ExtendedLogEntry.build().actor(sender).acted(holder)
                    .action("webeditor", "remove", n.getKey(), n.getValue(), n.getContexts())
                    .build().submit(plugin, sender);
        }

        String additionsSummary = "addition" + (additions == 1 ? "" : "s");
        String deletionsSummary = "deletion" + (deletions == 1 ? "" : "s");

        Message.APPLY_EDITS_SUCCESS.send(sender, holder.getFormattedDisplayName());
        Message.APPLY_EDITS_SUCCESS_SUMMARY.send(sender, additions, additionsSummary, deletions, deletionsSummary);
        for (Node n : diffAdded) {
            Message.APPLY_EDITS_DIFF_ADDED.send(sender, formatNode(plugin.getLocaleManager(), n));
        }
        for (Node n : diffRemoved) {
            Message.APPLY_EDITS_DIFF_REMOVED.send(sender, formatNode(plugin.getLocaleManager(), n));
        }
        StorageAssistant.save(holder, sender, plugin);
        return true;
    }

    private static String formatNode(LocaleManager localeManager, Node n) {
        return n.getKey() + " &7(" + (n.getValue() ? "&a" : "&c") + n.getValue() + "&7)" + MessageUtils.getAppendableNodeContextString(localeManager, n) +
                (n.hasExpiry() ? " &7(" + DurationFormatter.CONCISE.formatDateDiff(n.getExpiry().getEpochSecond()) + ")" : "");
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
