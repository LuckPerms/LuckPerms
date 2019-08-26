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

import me.lucko.luckperms.api.node.Node;
import me.lucko.luckperms.api.node.NodeBuilder;
import me.lucko.luckperms.common.context.ContextSetJsonSerializer;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.node.factory.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
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
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility methods for interacting with the LuckPerms web permission editor.
 */
public final class WebEditor {
    private WebEditor() {}

    private static JObject writeData(PermissionHolder holder) {
        return new JObject()
                .add("type", holder.getType().toString())
                .add("id", holder.getObjectName())
                .add("displayName", holder.getPlainDisplayName())
                .add("nodes", serializePermissions(holder.normalData().immutable().values()));
    }

    private static JObject writeData(Track track) {
        return new JObject()
                .add("type", "track")
                .add("id", track.getName())
                .add("groups", new JArray().consume(a -> track.getGroups().forEach(a::add)));
    }

    public static JsonObject formPayload(List<PermissionHolder> holders, List<Track> tracks, Sender sender, String cmdLabel, LuckPermsPlugin plugin) {
        Preconditions.checkArgument(!holders.isEmpty(), "holders is empty");

        // form the payload data
        return new JObject()
                .add("metadata", new JObject()
                        .add("commandAlias", cmdLabel)
                        .add("uploader", new JObject()
                                .add("name", sender.getNameWithLocation())
                                .add("uuid", sender.getUuid().toString())
                        )
                        .add("time", System.currentTimeMillis())
                        .add("pluginVersion", plugin.getBootstrap().getVersion())
                )
                .add("permissionHolders", new JArray()
                        .consume(arr -> {
                            for (PermissionHolder holder : holders) {
                                arr.add(writeData(holder));
                            }
                        })
                )
                .add("tracks", new JArray()
                        .consume(arr -> {
                            for (Track track : tracks) {
                                arr.add(writeData(track));
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

    private static JsonArray serializePermissions(Collection<Node> nodes) {
        JsonArray arr = new JsonArray();
        for (Node node : nodes) {
            JsonObject attributes = new JsonObject();

            attributes.addProperty("type", node.getType().name().toLowerCase());
            attributes.addProperty("key", node.getKey());
            attributes.addProperty("value", node.getValue());

            Instant expiry = node.getExpiry();
            if (expiry != null) {
                attributes.addProperty("expiry", expiry.getEpochSecond());
            }

            if (!node.getContexts().isEmpty()) {
                attributes.add("context", ContextSetJsonSerializer.serializeContextSet(node.getContexts()));
            }

            arr.add(attributes);
        }
        return arr;
    }

    public static Set<Node> deserializePermissions(JsonArray arr) {
        Set<Node> nodes = new HashSet<>();
        for (JsonElement ent : arr) {
            JsonObject attributes = ent.getAsJsonObject();

            String key = attributes.get("key").getAsString();
            boolean value = attributes.get("value").getAsBoolean();

            NodeBuilder<?, ?> builder = NodeFactory.builder(key).value(value);

            if (attributes.has("expiry")) {
                builder.expiry(attributes.get("expiry").getAsLong());
            }

            if (attributes.has("context")) {
                builder.context(ContextSetJsonSerializer.deserializeContextSet(attributes.get("context")));
            }

            nodes.add(builder.build());
        }
        return nodes;
    }

}
