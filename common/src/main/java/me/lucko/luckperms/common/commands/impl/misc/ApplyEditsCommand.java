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

import com.google.common.base.Splitter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.commands.abstraction.SingleCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeModel;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ApplyEditsCommand extends SingleCommand {
    public ApplyEditsCommand() {
        super("ApplyEdits", "Applies permission changes made from the web editor",
                "/%s applyedits <code> [target]", Permission.APPLY_EDITS, Predicates.notInRange(1, 2),
                Arg.list(
                        Arg.create("code", true, "the unique code for the data"),
                        Arg.create("target", false, "who to apply the data to")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) throws CommandException {
        String code = args.get(0);
        String who = args.size() == 2 ? args.get(1) : null;

        if (!code.contains("/")) {
            Message.APPLY_EDITS_INVALID_CODE.send(sender, code);
            return CommandResult.INVALID_ARGS;
        }

        Iterator<String> codeParts = Splitter.on('/').limit(2).split(code).iterator();
        String part1 = codeParts.next();
        String part2 = codeParts.next();

        if (part1.isEmpty() || part2.isEmpty()) {
            Message.APPLY_EDITS_INVALID_CODE.send(sender, code);
            return CommandResult.INVALID_ARGS;
        }

        String url = "https://gist.githubusercontent.com/anonymous/" + part1 + "/raw/" + part2 + "/luckperms-data.json";
        JsonObject data;

        try {
            data = read(url);
        } catch (Exception e) {
            e.printStackTrace();
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

        PermissionHolder holder;

        if (who.startsWith("group/")) {
            String group = who.substring("group/".length());
            holder = plugin.getGroupManager().getIfLoaded(group);

            if (holder == null) {
                Message.APPLY_EDITS_TARGET_GROUP_NOT_EXISTS.send(sender, group);
                return CommandResult.STATE_ERROR;
            }
        } else if (who.startsWith("user/")) {
            String user = who.substring("user/".length());
            UUID uuid = Util.parseUuid(user);
            if (uuid == null) {
                Message.APPLY_EDITS_TARGET_USER_NOT_UUID.send(sender, user);
                return CommandResult.STATE_ERROR;
            }
            holder = plugin.getUserManager().getIfLoaded(uuid);
            if (holder == null) {
                plugin.getStorage().loadUser(uuid, null).join();
            }
            holder = plugin.getUserManager().getIfLoaded(uuid);
            if (holder == null) {
                Message.APPLY_EDITS_TARGET_USER_UNABLE_TO_LOAD.send(sender, uuid.toString());
                return CommandResult.STATE_ERROR;
            }
        } else {
            Message.APPLY_EDITS_TARGET_UNKNOWN.send(sender, who);
            return CommandResult.STATE_ERROR;
        }

        Set<NodeModel> nodes = deserializePermissions(data.getAsJsonArray("nodes"));
        holder.setNodes(nodes.stream().map(NodeModel::toNode).collect(Collectors.toSet()));
        Message.APPLY_EDITS_SUCCESS.send(sender, nodes.size(), holder.getFriendlyName());
        SharedSubCommand.save(holder, sender, plugin);
        return CommandResult.SUCCESS;
    }

    @Override
    public boolean shouldDisplay() {
        return false;
    }

    private static JsonObject read(String address) throws IOException {
        URL url = new URL(address);
        try (InputStream in = url.openStream(); InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, JsonObject.class);
        }
    }

    private static Set<NodeModel> deserializePermissions(JsonArray permissionsSection) {
        Set<NodeModel> nodes = new HashSet<>();

        for (JsonElement ent : permissionsSection) {
            if (!ent.isJsonObject()) {
                continue;
            }

            JsonObject data = ent.getAsJsonObject();

            String permission = data.get("permission").getAsString();
            boolean value = true;
            String server = "global";
            String world = "global";
            long expiry = 0L;
            ImmutableContextSet context = ImmutableContextSet.empty();

            if (data.has("value")) {
                value = data.get("value").getAsBoolean();
            }
            if (data.has("server")) {
                server = data.get("server").getAsString();
            }
            if (data.has("world")) {
                world = data.get("world").getAsString();
            }
            if (data.has("expiry")) {
                expiry = data.get("expiry").getAsLong();
            }

            if (data.has("context") && data.get("context").isJsonObject()) {
                JsonObject contexts = data.get("context").getAsJsonObject();
                context = NodeModel.deserializeContextSet(contexts).makeImmutable();
            }

            nodes.add(NodeModel.of(permission, value, server, world, expiry, context));
        }

        return nodes;
    }
}
