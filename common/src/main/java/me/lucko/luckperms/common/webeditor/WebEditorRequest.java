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
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.matcher.ConstraintNodeMatcher;
import me.lucko.luckperms.common.node.utils.NodeJsonSerializer;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.storage.misc.NodeEntry;
import me.lucko.luckperms.common.util.gson.GsonProvider;
import me.lucko.luckperms.common.util.gson.JArray;
import me.lucko.luckperms.common.util.gson.JObject;
import me.lucko.luckperms.common.verbose.event.MetaCheckEvent;

import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.node.Node;
import net.luckperms.api.query.QueryOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/**
 * Encapsulates a request to the web editor.
 */
public class WebEditorRequest {

    public static final int MAX_USERS = 500;

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
            GsonProvider.normal().toJson(this.payload, writer);
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

    public static void includeMatchingGroups(List<? super Group> holders, Predicate<? super Group> filter, LuckPermsPlugin plugin) {
        plugin.getGroupManager().getAll().values().stream()
                .filter(filter)
                .sorted(Comparator
                        .<Group>comparingInt(g -> g.getWeight().orElse(0)).reversed()
                        .thenComparing(Group::getName, String.CASE_INSENSITIVE_ORDER)
                )
                .forEach(holders::add);
    }

    public static void includeMatchingUsers(List<? super User> holders, ConstraintNodeMatcher<Node> matcher, boolean includeOffline, LuckPermsPlugin plugin) {
        includeMatchingUsers(holders, matcher == null ? Collections.emptyList() : Collections.singleton(matcher), includeOffline, plugin);
    }

    public static void includeMatchingUsers(List<? super User> holders, Collection<ConstraintNodeMatcher<Node>> matchers, boolean includeOffline, LuckPermsPlugin plugin) {
        Map<UUID, User> users = new LinkedHashMap<>(plugin.getUserManager().getAll());

        if (!matchers.isEmpty()) {
            users.values().removeIf(user -> {
                for (ConstraintNodeMatcher<Node> matcher : matchers) {
                    if (user.normalData().asList().stream().anyMatch(matcher)) {
                        return false;
                    }
                }
                return true;
            });
        }

        if (includeOffline && users.size() < MAX_USERS) {
            if (matchers.isEmpty()) {
                findMatchingOfflineUsers(users, null, plugin);
            } else {
                for (ConstraintNodeMatcher<Node> matcher : matchers) {
                    if (users.size() < MAX_USERS) {
                        findMatchingOfflineUsers(users, matcher, plugin);
                    } else {
                        break;
                    }
                }
            }
        }

        users.values().stream()
                .sorted(Comparator
                        // sort firstly by the users relative weight (depends on the groups they inherit)
                        .<User>comparingInt(u -> u.getCachedData().getMetaData(QueryOptions.nonContextual()).getWeight(MetaCheckEvent.Origin.INTERNAL)).reversed()
                        // then, prioritise users we actually have a username for
                        .thenComparing(u -> u.getUsername().isPresent(), ((Comparator<Boolean>) Boolean::compare).reversed())
                        // then sort according to their username
                        .thenComparing(User::getPlainDisplayName, String.CASE_INSENSITIVE_ORDER)
                )
                .forEach(holders::add);
    }

    private static void findMatchingOfflineUsers(Map<UUID, User> users, ConstraintNodeMatcher<Node> matcher, LuckPermsPlugin plugin) {
        Stream<UUID> stream;
        if (matcher == null) {
            stream = plugin.getStorage().getUniqueUsers().join().stream();
        } else {
            stream = plugin.getStorage().searchUserNodes(matcher).join().stream()
                    .map(NodeEntry::getHolder)
                    .distinct();
        }

        stream.filter(uuid -> !users.containsKey(uuid))
                .sorted()
                .limit(MAX_USERS - users.size())
                .forEach(uuid -> {
                    User user = plugin.getStorage().loadUser(uuid, null).join();
                    if (user != null) {
                        users.put(uuid, user);
                    }
                    plugin.getUserManager().getHouseKeeper().cleanup(uuid);
                });
    }

}
