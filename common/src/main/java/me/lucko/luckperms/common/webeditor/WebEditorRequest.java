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

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.ContextSetJsonSerializer;
import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;
import me.lucko.luckperms.common.http.AbstractHttpClient;
import me.lucko.luckperms.common.http.UnsuccessfulRequestException;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.node.utils.NodeJsonSerializer;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.gson.GsonProvider;
import me.lucko.luckperms.common.util.gson.JArray;
import me.lucko.luckperms.common.util.gson.JObject;

import net.luckperms.api.context.ImmutableContextSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Encapsulates a request to the web editor.
 */
public class WebEditorRequest {

    /**
     * Generates a web editor request payload.
     *
     * @param holders the holders to edit
     * @param tracks the tracks to edit
     * @param sender the sender who is creating the session
     * @param cmdLabel the command label used by LuckPerms
     * @param plugin the plugin
     * @return a payload
     */
    public static WebEditorRequest generate(List<PermissionHolder> holders, List<Track> tracks, Sender sender, String cmdLabel, LuckPermsPlugin plugin) {
        Preconditions.checkArgument(!holders.isEmpty(), "holders is empty");

        ImmutableContextSet.Builder potentialContexts = new ImmutableContextSetImpl.BuilderImpl();
        potentialContexts.addAll(plugin.getContextManager().getPotentialContexts());
        for (PermissionHolder holder : holders) {
            holder.normalData().forEach(node -> potentialContexts.addAll(node.getContexts()));
        }

        // form the payload data
        return new WebEditorRequest(holders, tracks, sender, cmdLabel, potentialContexts.build(), plugin);
    }

    /**
     * The encoded json object this payload is made up of
     */
    private final JsonObject payload;

    private WebEditorRequest(List<PermissionHolder> holders, List<Track> tracks, Sender sender, String cmdLabel, ImmutableContextSet potentialContexts, LuckPermsPlugin plugin) {
        this.payload = new JObject()
                .add("metadata", formMetadata(sender, cmdLabel, plugin.getBootstrap().getVersion()))
                .add("permissionHolders", new JArray()
                        .consume(arr -> {
                            for (PermissionHolder holder : holders) {
                                arr.add(formPermissionHolder(holder));
                            }
                        })
                )
                .add("tracks", new JArray()
                        .consume(arr -> {
                            for (Track track : tracks) {
                                arr.add(formTrack(track));
                            }
                        })
                )
                .add("knownPermissions", new JArray().addAll(plugin.getPermissionRegistry().rootAsList()))
                .add("potentialContexts", ContextSetJsonSerializer.serialize(potentialContexts))
                .toJson();
    }

    private static JObject formMetadata(Sender sender, String cmdLabel, String pluginVersion) {
        return new JObject()
                .add("commandAlias", cmdLabel)
                .add("uploader", new JObject()
                        .add("name", sender.getNameWithLocation())
                        .add("uuid", sender.getUniqueId().toString())
                )
                .add("time", System.currentTimeMillis())
                .add("pluginVersion", pluginVersion);
    }

    private static JObject formPermissionHolder(PermissionHolder holder) {
        return new JObject()
                .add("type", holder.getType().toString())
                .add("id", holder.getObjectName())
                .add("displayName", holder.getPlainDisplayName())
                .add("nodes", NodeJsonSerializer.serializeNodes(holder.normalData().asList()));
    }

    private static JObject formTrack(Track track) {
        return new JObject()
                .add("type", "track")
                .add("id", track.getName())
                .add("groups", new JArray().addAll(track.getGroups()));
    }

    public byte[] encode() {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(bytesOut), StandardCharsets.UTF_8)) {
            GsonProvider.prettyPrinting().toJson(this.payload, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytesOut.toByteArray();
    }

    /**
     * Creates a web editor session, and sends the URL to the sender.
     *
     * @param plugin the plugin
     * @param sender the sender creating the session
     * @return the command result
     */
    public CommandResult createSession(LuckPermsPlugin plugin, Sender sender) {
        String pasteId;
        try {
            pasteId = plugin.getBytebin().postContent(encode(), AbstractHttpClient.JSON_TYPE).key();
        } catch (UnsuccessfulRequestException e) {
            Message.EDITOR_HTTP_REQUEST_FAILURE.send(sender, e.getResponse().code(), e.getResponse().message());
            return CommandResult.STATE_ERROR;
        } catch (IOException e) {
            new RuntimeException("Error uploading data to bytebin", e).printStackTrace();
            Message.EDITOR_HTTP_UNKNOWN_FAILURE.send(sender);
            return CommandResult.STATE_ERROR;
        }

        // form a url for the editor
        String url = plugin.getConfiguration().get(ConfigKeys.WEB_EDITOR_URL_PATTERN) + pasteId;
        Message.EDITOR_URL.send(sender, url);
        return CommandResult.SUCCESS;
    }

}
