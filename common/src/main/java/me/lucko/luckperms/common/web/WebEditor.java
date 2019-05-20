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

package me.lucko.luckperms.common.web;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.context.ContextSetJsonSerializer;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.model.NodeDataContainer;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Uuids;
import me.lucko.luckperms.common.util.gson.GsonProvider;
import me.lucko.luckperms.common.util.gson.JArray;
import me.lucko.luckperms.common.util.gson.JObject;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Utility methods for interacting with the LuckPerms web permission editor.
 */
public final class WebEditor {
    private WebEditor() {}

    private static final String USER_ID_PATTERN = "user/";
    private static final String GROUP_ID_PATTERN = "group/";

    private static JObject writeData(PermissionHolder holder) {
        return new JObject()
                .add("who", new JObject()
                        .add("id", getHolderIdentifier(holder))
                        .add("friendly", holder.getPlainDisplayName())
                        .consume(obj -> {
                            if (holder.getType() == HolderType.USER) {
                                obj.add("uuid", ((User) holder).getUuid().toString());
                            }
                        }))
                .add("nodes", serializePermissions(holder.enduringData().immutable().values().stream().map(NodeDataContainer::fromNode)));
    }

    public static JsonObject formPayload(List<PermissionHolder> holders, Sender sender, String cmdLabel, LuckPermsPlugin plugin) {
        Preconditions.checkArgument(!holders.isEmpty(), "holders is empty");

        // form the payload data
        return new JObject()
                .add("metadata", new JObject()
                        .add("cmdAlias", cmdLabel)
                        .add("uploader", new JObject()
                                .add("name", sender.getNameWithLocation())
                                .add("uuid", sender.getUuid().toString())
                        )
                        .add("time", System.currentTimeMillis())
                )
                .add("sessions", new JArray()
                        .consume(arr -> {
                            for (PermissionHolder holder : holders) {
                                arr.add(writeData(holder));
                            }
                        })
                )
                .add("knownPermissions", new JArray()
                        .consume(arr -> {
                            for (String perm : plugin.getPermissionRegistry().rootAsList()) {
                                arr.add(perm);
                            }
                        })
                ).toJson();
    }

    private static String getHolderIdentifier(PermissionHolder holder) {
        if (holder.getType() == HolderType.USER) {
            User user = ((User) holder);
            return USER_ID_PATTERN + user.getUuid().toString();
        } else {
            Group group = ((Group) holder);
            return GROUP_ID_PATTERN + group.getName();
        }
    }

    public static PermissionHolder getHolderFromIdentifier(LuckPermsPlugin plugin, Sender sender, String who) {
        if (who.startsWith(GROUP_ID_PATTERN)) {
            String group = who.substring(GROUP_ID_PATTERN.length());
            Group holder = plugin.getStorage().loadGroup(group).join().orElse(null);
            if (holder == null) {
                Message.APPLY_EDITS_TARGET_GROUP_NOT_EXISTS.send(sender, group);
            }
            return holder;
        } else if (who.startsWith(USER_ID_PATTERN)) {
            String user = who.substring(USER_ID_PATTERN.length());
            UUID uuid = Uuids.parse(user);
            if (uuid == null) {
                Message.APPLY_EDITS_TARGET_USER_NOT_UUID.send(sender, user);
                return null;
            }
            User holder = plugin.getStorage().loadUser(uuid, null).join();
            if (holder == null) {
                Message.APPLY_EDITS_TARGET_USER_UNABLE_TO_LOAD.send(sender, uuid.toString());
            }
            return holder;
        } else {
            Message.APPLY_EDITS_TARGET_UNKNOWN.send(sender, who);
            return null;
        }
    }

    public static JsonObject readDataFromBytebin(Bytebin bytebin, String id) {
        Request request = new Request.Builder()
                .url(bytebin.getPasteUrl(id))
                .build();

        try (Response response = HttpClient.makeCall(request)) {
            try (ResponseBody responseBody = response.body()) {
                if (responseBody == null) {
                    throw new RuntimeException("No response");
                }

                try (InputStream inputStream = responseBody.byteStream()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        return GsonProvider.normal().fromJson(reader, JsonObject.class);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonArray serializePermissions(Stream<NodeDataContainer> nodes) {
        JsonArray arr = new JsonArray();
        nodes.forEach(node -> {
            JsonObject attributes = new JsonObject();
            attributes.addProperty("permission", node.getPermission());
            attributes.addProperty("value", node.getValue());

            if (!node.getServer().equals("global")) {
                attributes.addProperty("server", node.getServer());
            }

            if (!node.getWorld().equals("global")) {
                attributes.addProperty("world", node.getWorld());
            }

            if (node.getExpiry() != 0L) {
                attributes.addProperty("expiry", node.getExpiry());
            }

            if (!node.getContexts().isEmpty()) {
                attributes.add("context", ContextSetJsonSerializer.serializeContextSet(node.getContexts()));
            }

            arr.add(attributes);
        });
        return arr;
    }

    public static Set<NodeDataContainer> deserializePermissions(JsonArray permissionsSection) {
        Set<NodeDataContainer> nodes = new HashSet<>();

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
                context = ContextSetJsonSerializer.deserializeContextSet(contexts).immutableCopy();
            }

            nodes.add(NodeDataContainer.of(permission, value, server, world, expiry, context));
        }

        return nodes;
    }

}
