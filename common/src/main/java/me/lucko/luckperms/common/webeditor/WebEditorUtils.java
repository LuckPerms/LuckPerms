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

package me.lucko.luckperms.common.webeditor;

import lombok.experimental.UtilityClass;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.contexts.ContextSetJsonSerializer;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeModel;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Utility methods for interacting with the LuckPerms web permission editor.
 */
@UtilityClass
public class WebEditorUtils {

    private static final String FILE_NAME = "luckperms-data.json";
    private static final String GIST_API_URL = "https://api.github.com/gists";

    private static final String USER_ID_PATTERN = "user/";
    private static final String GROUP_ID_PATTERN = "group/";

    public static String getHolderIdentifier(PermissionHolder holder) {
        if (holder.getType().isUser()) {
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
            Group holder = plugin.getGroupManager().getIfLoaded(group);
            if (holder == null) {
                Message.APPLY_EDITS_TARGET_GROUP_NOT_EXISTS.send(sender, group);
            }
            return holder;
        } else if (who.startsWith(USER_ID_PATTERN)) {
            String user = who.substring(USER_ID_PATTERN.length());
            UUID uuid = CommandUtils.parseUuid(user);
            if (uuid == null) {
                Message.APPLY_EDITS_TARGET_USER_NOT_UUID.send(sender, user);
                return null;
            }
            plugin.getStorage().loadUser(uuid, null).join();
            User holder = plugin.getUserManager().getIfLoaded(uuid);
            if (holder == null) {
                Message.APPLY_EDITS_TARGET_USER_UNABLE_TO_LOAD.send(sender, uuid.toString());
            }
            return holder;
        } else {
            Message.APPLY_EDITS_TARGET_UNKNOWN.send(sender, who);
            return null;
        }
    }

    public static String postToGist(String content) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(GIST_API_URL).openConnection();
            connection.addRequestProperty("User-Agent", "luckperms");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                StringWriter sw = new StringWriter();
                new JsonWriter(sw).beginObject()
                        .name("description").value("LuckPerms Web Permissions Editor Data")
                        .name("public").value(false)
                        .name("files")
                        .beginObject().name(FILE_NAME)
                        .beginObject().name("content").value(content)
                        .endObject()
                        .endObject()
                        .endObject();

                os.write(sw.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (connection.getResponseCode() >= 400) {
                throw new RuntimeException("Connection returned response code: " + connection.getResponseCode() + " - " + connection.getResponseMessage());
            }

            try (InputStream inputStream = connection.getInputStream()) {
                try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                        JsonObject response = new Gson().fromJson(reader, JsonObject.class);
                        return response.get("id").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static JsonObject getDataFromGist(String id) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(GIST_API_URL + "/" + id).openConnection();
            connection.addRequestProperty("User-Agent", "luckperms");
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() >= 400) {
                throw new RuntimeException("Connection returned response code: " + connection.getResponseCode() + " - " + connection.getResponseMessage());
            }

            try (InputStream inputStream = connection.getInputStream()) {
                try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                        JsonObject response = new Gson().fromJson(reader, JsonObject.class);
                        JsonObject files = response.get("files").getAsJsonObject();
                        JsonObject permsFile = files.get(FILE_NAME).getAsJsonObject();

                        // uh..
                        if (permsFile.get("truncated").getAsBoolean()) {
                            String rawUrlStr = permsFile.get("raw_url").getAsString();
                            URL rawUrl = new URL(rawUrlStr);
                            try (InputStream rawInputStream = rawUrl.openStream()) {
                                try (InputStreamReader rawInputStreamReader = new InputStreamReader(rawInputStream, StandardCharsets.UTF_8)) {
                                    try (BufferedReader rawReader = new BufferedReader(rawInputStreamReader)) {
                                        return new Gson().fromJson(rawReader, JsonObject.class);
                                    }
                                }
                            }
                        } else {
                            String content = permsFile.get("content").getAsString();
                            return new Gson().fromJson(content, JsonObject.class);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static JsonArray serializePermissions(Stream<NodeModel> nodes) {
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

    public static Set<NodeModel> deserializePermissions(JsonArray permissionsSection) {
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
                context = ContextSetJsonSerializer.deserializeContextSet(contexts).makeImmutable();
            }

            nodes.add(NodeModel.of(permission, value, server, world, expiry, context));
        }

        return nodes;
    }

}
