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

package me.lucko.luckperms.common.commands.impl.generic.other;

import com.google.common.base.Splitter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;

import me.lucko.luckperms.common.commands.ArgumentPermissions;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.constants.CommandPermission;
import me.lucko.luckperms.common.contexts.ContextSetJsonSerializer;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeModel;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

public class HolderEditor<T extends PermissionHolder> extends SubCommand<T> {
    private static final String USER_ID_PATTERN = "user/";
    private static final String GROUP_ID_PATTERN = "group/";
    private static final String FILE_NAME = "luckperms-data.json";

    public HolderEditor(LocaleManager locale, boolean user) {
        super(CommandSpec.HOLDER_EDITOR.spec(locale), "editor", user ? CommandPermission.USER_EDITOR : CommandPermission.GROUP_EDITOR, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, T holder, List<String> args, String label) throws CommandException {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), holder)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        // form the payload data
        JsonObject payload = new JsonObject();
        payload.addProperty("who", id(holder));
        payload.addProperty("whoFriendly", holder.getFriendlyName());
        if (holder instanceof User) {
            payload.addProperty("whoUuid", ((User) holder).getUuid().toString());
        }
        payload.addProperty("cmdAlias", label);
        payload.addProperty("uploadedBy", sender.getNameWithLocation());
        payload.addProperty("uploadedByUuid", sender.getUuid().toString());
        payload.addProperty("time", System.currentTimeMillis());

        // attach the holders permissions
        payload.add("nodes", serializePermissions(holder.getEnduringNodes().values().stream().map(NodeModel::fromNode)));

        // attach an array of all permissions known to the server, to use for tab completion in the editor
        JsonArray knownPermsArray = new JsonArray();
        for (String perm : plugin.getPermissionVault().rootAsList()) {
            knownPermsArray.add(new JsonPrimitive(perm));
        }
        payload.add("knownPermissions", knownPermsArray);

        // upload the payload data to gist
        String dataUrl = paste(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(payload));
        if (dataUrl == null) {
            Message.EDITOR_UPLOAD_FAILURE.send(sender);
            return CommandResult.STATE_ERROR;
        }

        // extract the information we need from the gist url
        List<String> parts = Splitter.on('/').splitToList(dataUrl);
        String id = "?" + parts.get(4) + "/" + parts.get(6);

        // form a url for the editor
        String url = plugin.getConfiguration().get(ConfigKeys.WEB_EDITOR_URL_PATTERN) + id;

        Message.EDITOR_URL.send(sender);

        Component message = TextComponent.builder(url).color(TextColor.AQUA)
                .clickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.of("Click to open the editor.").color(TextColor.GRAY)))
                .build();

        sender.sendMessage(message);
        return CommandResult.SUCCESS;
    }

    private static String id(PermissionHolder holder) {
        if (holder instanceof User) {
            User user = ((User) holder);
            return USER_ID_PATTERN + user.getUuid().toString();
        } else {
            Group group = ((Group) holder);
            return GROUP_ID_PATTERN + group.getName();
        }
    }

    private static String paste(String content) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("https://api.github.com/gists").openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
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
                try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    JsonObject response = new Gson().fromJson(reader, JsonObject.class);
                    return response.get("files").getAsJsonObject().get(FILE_NAME).getAsJsonObject().get("raw_url").getAsString();
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

    private static JsonArray serializePermissions(Stream<NodeModel> nodes) {
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
}
