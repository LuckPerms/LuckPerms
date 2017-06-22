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

package me.lucko.luckperms.common.storage.backing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Iterator;
import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.core.NodeModel;
import me.lucko.luckperms.common.core.UserIdentifier;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.managers.impl.GenericUserManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.holder.NodeHeldPermission;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class JSONBacking extends FlatfileBacking {
    private final Gson gson;

    public JSONBacking(LuckPermsPlugin plugin, File pluginDir, String dataFolderName) {
        super(plugin, "JSON", pluginDir, ".json", dataFolderName);
        gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public boolean writeElementToFile(File file, JsonElement element) {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(element, writer);
            writer.flush();
            return true;
        } catch (Throwable t) {
            plugin.getLog().warn("Exception whilst writing to file: " + file.getAbsolutePath());
            t.printStackTrace();
            return false;
        }
    }

    public JsonObject readObjectFromFile(File file) {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, JsonObject.class);
        } catch (Throwable t) {
            plugin.getLog().warn("Exception whilst reading from file: " + file.getAbsolutePath());
            t.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean applyBulkUpdate(BulkUpdate bulkUpdate) {
        return call("null", () -> {
            if (bulkUpdate.getDataType().isIncludingUsers()) {
                File[] files = usersDir.listFiles((dir, name1) -> name1.endsWith(".json"));
                if (files == null) return false;

                for (File file : files) {
                    call(file.getName(), () -> {
                        registerFileAction("users", file);

                        JsonObject object = readObjectFromFile(file);

                        Set<NodeModel> nodes = new HashSet<>();
                        nodes.addAll(deserializePermissions(object.get("permissions").getAsJsonArray()));

                        Set<NodeModel> results = nodes.stream()
                                .map(n -> Optional.ofNullable(bulkUpdate.apply(n)))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.toSet());

                        object.add("permissions", serializePermissions(results));

                        writeElementToFile(file, object);
                        return true;
                    }, true);
                }
            }

            if (bulkUpdate.getDataType().isIncludingGroups()) {
                File[] files = groupsDir.listFiles((dir, name1) -> name1.endsWith(".json"));
                if (files == null) return false;

                for (File file : files) {
                    call(file.getName(), () -> {
                        registerFileAction("groups", file);

                        JsonObject object = readObjectFromFile(file);

                        Set<NodeModel> nodes = new HashSet<>();
                        nodes.addAll(deserializePermissions(object.get("permissions").getAsJsonArray()));

                        Set<NodeModel> results = nodes.stream()
                                .map(n -> Optional.ofNullable(bulkUpdate.apply(n)))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.toSet());

                        object.add("permissions", serializePermissions(results));

                        writeElementToFile(file, object);
                        return true;
                    }, true);
                }
            }

            return true;
        }, false);
    }

    @Override
    public boolean loadUser(UUID uuid, String username) {
        User user = plugin.getUserManager().getOrMake(UserIdentifier.of(uuid, username));
        user.getIoLock().lock();
        try {
            return call(uuid.toString(), () -> {
                File userFile = new File(usersDir, uuid.toString() + ".json");
                registerFileAction("users", userFile);

                if (userFile.exists()) {
                    JsonObject object = readObjectFromFile(userFile);
                    String name = object.get("name").getAsString();
                    user.getPrimaryGroup().setStoredValue(object.get("primaryGroup").getAsString());

                    Set<NodeModel> data = deserializePermissions(object.get("permissions").getAsJsonArray());
                    Set<Node> nodes = data.stream().map(NodeModel::toNode).collect(Collectors.toSet());
                    user.setNodes(nodes);
                    user.setName(name, true);

                    boolean save = plugin.getUserManager().giveDefaultIfNeeded(user, false);
                    if (user.getName().isPresent() && (name == null || !user.getName().get().equalsIgnoreCase(name))) {
                        save = true;
                    }

                    if (save) {
                        saveUser(user);
                    }

                    return true;
                } else {
                    if (GenericUserManager.shouldSave(user)) {
                        user.clearNodes();
                        user.getPrimaryGroup().setStoredValue(null);
                        plugin.getUserManager().giveDefaultIfNeeded(user, false);
                    }
                    return true;
                }
            }, false);
        } finally {
            user.getIoLock().unlock();
            user.getRefreshBuffer().requestDirectly();
        }
    }

    @Override
    public boolean saveUser(User user) {
        user.getIoLock().lock();
        try {
            return call(user.getUuid().toString(), () -> {
                File userFile = new File(usersDir, user.getUuid().toString() + ".json");
                registerFileAction("users", userFile);

                if (!GenericUserManager.shouldSave(user)) {
                    if (userFile.exists()) {
                        userFile.delete();
                    }
                    return true;
                }

                if (!userFile.exists()) {
                    userFile.createNewFile();
                }

                JsonObject data = new JsonObject();
                data.addProperty("uuid", user.getUuid().toString());
                data.addProperty("name", user.getName().orElse("null"));
                data.addProperty("primaryGroup", user.getPrimaryGroup().getStoredValue());

                Set<NodeModel> nodes = user.getNodes().values().stream().map(NodeModel::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
                data.add("permissions", serializePermissions(nodes));

                return writeElementToFile(userFile, data);
            }, false);
        } finally {
            user.getIoLock().unlock();
        }
    }

    @Override
    public boolean cleanupUsers() {
        return call("null", () -> {
            File[] files = usersDir.listFiles((dir, name1) -> name1.endsWith(".json"));
            if (files == null) return false;

            for (File file : files) {
                call(file.getName(), () -> {
                    registerFileAction("users", file);

                    JsonObject object = readObjectFromFile(file);

                    Set<NodeModel> nodes = new HashSet<>();
                    nodes.addAll(deserializePermissions(object.get("permissions").getAsJsonArray()));

                    boolean shouldDelete = false;
                    if (nodes.size() == 1) {
                        for (NodeModel e : nodes) {
                            // There's only one
                            shouldDelete = e.getPermission().equalsIgnoreCase("group.default") && e.isValue();
                        }
                    }

                    if (shouldDelete) {
                        file.delete();
                    }
                    return true;
                }, true);
            }
            return true;
        }, false);
    }

    @Override
    public List<HeldPermission<UUID>> getUsersWithPermission(String permission) {
        ImmutableList.Builder<HeldPermission<UUID>> held = ImmutableList.builder();
        boolean success = call("null", () -> {
            File[] files = usersDir.listFiles((dir, name1) -> name1.endsWith(".json"));
            if (files == null) return false;

            for (File file : files) {
                call(file.getName(), () -> {
                    registerFileAction("users", file);

                    UUID holder = UUID.fromString(file.getName().substring(0, file.getName().length() - 5));

                    JsonObject object = readObjectFromFile(file);

                    Set<NodeModel> nodes = new HashSet<>();
                    nodes.addAll(deserializePermissions(object.get("permissions").getAsJsonArray()));

                    for (NodeModel e : nodes) {
                        if (!e.getPermission().equalsIgnoreCase(permission)) {
                            continue;
                        }

                        held.add(NodeHeldPermission.of(holder, e));
                    }
                    return true;
                }, true);
            }
            return true;
        }, false);
        return success ? held.build() : null;
    }

    @Override
    public boolean createAndLoadGroup(String name) {
        Group group = plugin.getGroupManager().getOrMake(name);
        group.getIoLock().lock();
        try {
            return call(name, () -> {
                File groupFile = new File(groupsDir, name + ".json");
                registerFileAction("groups", groupFile);

                if (groupFile.exists()) {
                    JsonObject object = readObjectFromFile(groupFile);
                    Set<NodeModel> data = deserializePermissions(object.get("permissions").getAsJsonArray());
                    Set<Node> nodes = data.stream().map(NodeModel::toNode).collect(Collectors.toSet());
                    group.setNodes(nodes);
                    return true;
                } else {
                    groupFile.createNewFile();

                    JsonObject data = new JsonObject();
                    data.addProperty("name", group.getName());

                    Set<NodeModel> nodes = group.getNodes().values().stream().map(NodeModel::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
                    data.add("permissions", serializePermissions(nodes));

                    return writeElementToFile(groupFile, data);
                }
            }, false);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean loadGroup(String name) {
        Group group = plugin.getGroupManager().getOrMake(name);
        group.getIoLock().lock();
        try {
            return call(name, () -> {
                File groupFile = new File(groupsDir, name + ".json");
                registerFileAction("groups", groupFile);

                if (!groupFile.exists()) {
                    return false;
                }

                JsonObject object = readObjectFromFile(groupFile);
                Set<NodeModel> data = deserializePermissions(object.get("permissions").getAsJsonArray());
                Set<Node> nodes = data.stream().map(NodeModel::toNode).collect(Collectors.toSet());
                group.setNodes(nodes);
                return true;
            }, false);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean saveGroup(Group group) {
        group.getIoLock().lock();
        try {
            return call(group.getName(), () -> {
                File groupFile = new File(groupsDir, group.getName() + ".json");
                registerFileAction("groups", groupFile);

                if (!groupFile.exists()) {
                    groupFile.createNewFile();
                }

                JsonObject data = new JsonObject();
                data.addProperty("name", group.getName());
                Set<NodeModel> nodes = group.getNodes().values().stream().map(NodeModel::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
                data.add("permissions", serializePermissions(nodes));
                return writeElementToFile(groupFile, data);
            }, false);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public List<HeldPermission<String>> getGroupsWithPermission(String permission) {
        ImmutableList.Builder<HeldPermission<String>> held = ImmutableList.builder();
        boolean success = call("null", () -> {
            File[] files = groupsDir.listFiles((dir, name1) -> name1.endsWith(".json"));
            if (files == null) return false;

            for (File file : files) {
                call(file.getName(), () -> {
                    registerFileAction("groups", file);

                    String holder = file.getName().substring(0, file.getName().length() - 5);

                    JsonObject object = readObjectFromFile(file);

                    Set<NodeModel> nodes = new HashSet<>();
                    nodes.addAll(deserializePermissions(object.get("permissions").getAsJsonArray()));

                    for (NodeModel e : nodes) {
                        if (!e.getPermission().equalsIgnoreCase(permission)) {
                            continue;
                        }

                        held.add(NodeHeldPermission.of(holder, e));
                    }
                    return true;
                }, true);
            }
            return true;
        }, false);
        return success ? held.build() : null;
    }

    @Override
    public boolean createAndLoadTrack(String name) {
        Track track = plugin.getTrackManager().getOrMake(name);
        track.getIoLock().lock();
        try {
            return call(name, () -> {
                File trackFile = new File(tracksDir, name + ".json");
                registerFileAction("tracks", trackFile);

                if (trackFile.exists()) {
                    JsonObject object = readObjectFromFile(trackFile);
                    List<String> groups = new ArrayList<>();
                    for (JsonElement g : object.get("groups").getAsJsonArray()) {
                        groups.add(g.getAsString());
                    }
                    track.setGroups(groups);
                    return true;
                } else {
                    trackFile.createNewFile();

                    JsonObject data = new JsonObject();
                    data.addProperty("name", track.getName());
                    JsonArray groups = new JsonArray();
                    for (String s : track.getGroups()) {
                        groups.add(new JsonPrimitive(s));
                    }
                    data.add("groups", groups);

                    return writeElementToFile(trackFile, data);
                }
            }, false);
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean loadTrack(String name) {
        Track track = plugin.getTrackManager().getOrMake(name);
        track.getIoLock().lock();
        try {
            return call(name, () -> {
                File trackFile = new File(tracksDir, name + ".json");
                registerFileAction("tracks", trackFile);

                if (!trackFile.exists()) {
                    return false;
                }

                JsonObject object = readObjectFromFile(trackFile);
                List<String> groups = new ArrayList<>();
                for (JsonElement g : object.get("groups").getAsJsonArray()) {
                    groups.add(g.getAsString());
                }
                track.setGroups(groups);
                return true;

            }, false);
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean saveTrack(Track track) {
        track.getIoLock().lock();
        try {
            return call(track.getName(), () -> {
                File trackFile = new File(tracksDir, track.getName() + ".json");
                registerFileAction("tracks", trackFile);

                if (!trackFile.exists()) {
                    trackFile.createNewFile();
                }

                JsonObject data = new JsonObject();
                data.addProperty("name", track.getName());
                JsonArray groups = new JsonArray();
                for (String s : track.getGroups()) {
                    groups.add(new JsonPrimitive(s));
                }
                data.add("groups", groups);

                return writeElementToFile(trackFile, data);
            }, false);
        } finally {
            track.getIoLock().unlock();
        }
    }

    public static Set<NodeModel> deserializePermissions(JsonArray permissionsSection) {
        Set<NodeModel> nodes = new HashSet<>();

        for (JsonElement ent : permissionsSection) {
            if (ent.isJsonPrimitive() && ent.getAsJsonPrimitive().isString()) {
                String permission = ent.getAsJsonPrimitive().getAsString();
                nodes.add(NodeModel.of(permission, true, "global", "global", 0L, ImmutableContextSet.empty()));
                continue;
            }

            if (!ent.isJsonObject()) {
                continue;
            }

            JsonObject data = ent.getAsJsonObject();
            Map.Entry<String, JsonElement> entry = Iterables.getFirst(data.entrySet(), null);

            if (entry == null || !entry.getValue().isJsonObject()) {
                continue;
            }

            String permission = entry.getKey();
            JsonObject attributes = entry.getValue().getAsJsonObject();

            boolean value = true;
            String server = "global";
            String world = "global";
            long expiry = 0L;
            ImmutableContextSet context = ImmutableContextSet.empty();

            if (attributes.has("value")) {
                value = attributes.get("value").getAsBoolean();
            }
            if (attributes.has("server")) {
                server = attributes.get("server").getAsString();
            }
            if (attributes.has("world")) {
                world = attributes.get("world").getAsString();
            }
            if (attributes.has("expiry")) {
                expiry = attributes.get("expiry").getAsLong();
            }

            if (attributes.has("context") && attributes.get("context").isJsonObject()) {
                JsonObject contexts = attributes.get("context").getAsJsonObject();
                context = NodeModel.deserializeContextSet(contexts).makeImmutable();
            }

            final JsonElement batchAttribute = attributes.get("permissions");
            if (permission.startsWith("luckperms.batch") && batchAttribute != null && batchAttribute.isJsonArray()) {
                for (JsonElement element : batchAttribute.getAsJsonArray()) {
                    nodes.add(NodeModel.of(element.getAsString(), value, server, world, expiry, context));
                }
            } else {
                nodes.add(NodeModel.of(permission, value, server, world, expiry, context));
            }

        }

        return nodes;
    }

    public static JsonArray serializePermissions(Set<NodeModel> nodes) {
        JsonArray arr = new JsonArray();

        for (NodeModel node : nodes) {
            // just a raw, default node.
            boolean single = node.isValue() &&
                    node.getServer().equalsIgnoreCase("global") &&
                    node.getWorld().equalsIgnoreCase("global") &&
                    node.getExpiry() == 0L &&
                    node.getContexts().isEmpty();

            // just add a string to the list.
            if (single) {
                arr.add(new JsonPrimitive(node.getPermission()));
                continue;
            }

            JsonObject attributes = new JsonObject();
            attributes.addProperty("value", node.isValue());

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
                attributes.add("context", node.getContextsAsJson());
            }

            JsonObject perm = new JsonObject();
            perm.add(node.getPermission(), attributes);
            arr.add(perm);
        }

        return arr;
    }
}
