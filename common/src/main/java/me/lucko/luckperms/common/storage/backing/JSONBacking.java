/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.core.PriorityComparator;
import me.lucko.luckperms.common.core.UserIdentifier;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.managers.impl.GenericUserManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.backing.utils.NodeDataHolder;
import me.lucko.luckperms.common.storage.holder.NodeHeldPermission;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
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

    public boolean readObjectFromFile(File file, Function<JsonObject, Boolean> readOperation) {
        boolean success = false;
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonObject object = gson.fromJson(reader, JsonObject.class);
            success = readOperation.apply(object);
        } catch (Throwable t) {
            plugin.getLog().warn("Exception whilst reading from file: " + file.getAbsolutePath());
            t.printStackTrace();
        }
        return success;
    }

    @Override
    public boolean loadUser(UUID uuid, String username) {
        User user = plugin.getUserManager().getOrMake(UserIdentifier.of(uuid, username));
        user.getIoLock().lock();
        try {
            return call(() -> {
                File userFile = new File(usersDir, uuid.toString() + ".json");
                registerFileAction("users", userFile);

                if (userFile.exists()) {
                    return readObjectFromFile(userFile, object -> {
                        String name = object.get("name").getAsString();
                        user.getPrimaryGroup().setStoredValue(object.get("primaryGroup").getAsString());

                        Set<NodeDataHolder> data = deserializePermissions(object.get("permissions").getAsJsonArray());
                        Set<Node> nodes = data.stream().map(NodeDataHolder::toNode).collect(Collectors.toSet());
                        user.setNodes(nodes);

                        boolean save = plugin.getUserManager().giveDefaultIfNeeded(user, false);

                        if (user.getName() == null || user.getName().equalsIgnoreCase("null")) {
                            user.setName(name);
                        } else {
                            if (!name.equalsIgnoreCase(user.getName())) {
                                save = true;
                            }
                        }

                        if (save) {
                            saveUser(user);
                        }

                        return true;
                    });
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
            return call(() -> {
                File userFile = new File(usersDir, user.getUuid().toString() + ".json");
                registerFileAction("users", userFile);

                if (!GenericUserManager.shouldSave(user)) {
                    if (userFile.exists()) {
                        userFile.delete();
                    }
                    return true;
                }

                if (!userFile.exists()) {
                    try {
                        userFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }

                JsonObject data = new JsonObject();
                data.addProperty("uuid", user.getUuid().toString());
                data.addProperty("name", user.getName());
                data.addProperty("primaryGroup", user.getPrimaryGroup().getStoredValue());

                Set<NodeDataHolder> nodes = user.getNodes().values().stream().map(NodeDataHolder::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
                data.add("permissions", serializePermissions(nodes));

                return writeElementToFile(userFile, data);
            }, false);
        } finally {
            user.getIoLock().unlock();
        }
    }

    @Override
    public boolean cleanupUsers() {
        return call(() -> {
            File[] files = usersDir.listFiles((dir, name1) -> name1.endsWith(".json"));
            if (files == null) return false;

            for (File file : files) {
                registerFileAction("users", file);

                Set<NodeDataHolder> nodes = new HashSet<>();
                readObjectFromFile(file, object -> {
                   nodes.addAll(deserializePermissions(object.get("permissions").getAsJsonArray()));
                   return true;
                });

                boolean shouldDelete = false;
                if (nodes.size() == 1) {
                    for (NodeDataHolder e : nodes) {
                        // There's only one
                        shouldDelete = e.getPermission().equalsIgnoreCase("group.default") && e.isValue();
                    }
                }

                if (shouldDelete) {
                    file.delete();
                }
            }
            return true;
        }, false);
    }

    @Override
    public List<HeldPermission<UUID>> getUsersWithPermission(String permission) {
        ImmutableList.Builder<HeldPermission<UUID>> held = ImmutableList.builder();
        boolean success = call(() -> {
            File[] files = usersDir.listFiles((dir, name1) -> name1.endsWith(".json"));
            if (files == null) return false;

            for (File file : files) {
                registerFileAction("users", file);

                UUID holder = UUID.fromString(file.getName().substring(0, file.getName().length() - 5));
                Set<NodeDataHolder> nodes = new HashSet<>();

                readObjectFromFile(file, object -> {
                    nodes.addAll(deserializePermissions(object.get("permissions").getAsJsonArray()));
                    return true;
                });

                for (NodeDataHolder e : nodes) {
                    if (!e.getPermission().equalsIgnoreCase(permission)) {
                        continue;
                    }

                    held.add(NodeHeldPermission.of(holder, e));
                }
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
            return call(() -> {
                File groupFile = new File(groupsDir, name + ".json");
                registerFileAction("groups", groupFile);

                if (groupFile.exists()) {
                    return readObjectFromFile(groupFile, object -> {
                        Set<NodeDataHolder> data = deserializePermissions(object.get("permissions").getAsJsonArray());
                        Set<Node> nodes = data.stream().map(NodeDataHolder::toNode).collect(Collectors.toSet());
                        group.setNodes(nodes);
                        return true;
                    });
                } else {
                    try {
                        groupFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }

                    JsonObject data = new JsonObject();
                    data.addProperty("name", group.getName());

                    Set<NodeDataHolder> nodes = group.getNodes().values().stream().map(NodeDataHolder::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
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
            return call(() -> {
                File groupFile = new File(groupsDir, name + ".json");
                registerFileAction("groups", groupFile);

                return groupFile.exists() && readObjectFromFile(groupFile, object -> {
                    Set<NodeDataHolder> data = deserializePermissions(object.get("permissions").getAsJsonArray());
                    Set<Node> nodes = data.stream().map(NodeDataHolder::toNode).collect(Collectors.toSet());
                    group.setNodes(nodes);
                    return true;
                });
            }, false);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean saveGroup(Group group) {
        group.getIoLock().lock();
        try {
            return call(() -> {
                File groupFile = new File(groupsDir, group.getName() + ".json");
                registerFileAction("groups", groupFile);

                if (!groupFile.exists()) {
                    try {
                        groupFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }

                JsonObject data = new JsonObject();
                data.addProperty("name", group.getName());
                Set<NodeDataHolder> nodes = group.getNodes().values().stream().map(NodeDataHolder::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
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
        boolean success = call(() -> {
            File[] files = groupsDir.listFiles((dir, name1) -> name1.endsWith(".json"));
            if (files == null) return false;

            for (File file : files) {
                registerFileAction("groups", file);

                String holder = file.getName().substring(0, file.getName().length() - 5);
                Set<NodeDataHolder> nodes = new HashSet<>();
                readObjectFromFile(file, element -> {
                    nodes.addAll(deserializePermissions(element.get("permissions").getAsJsonArray()));
                    return true;
                });

                for (NodeDataHolder e : nodes) {
                    if (!e.getPermission().equalsIgnoreCase(permission)) {
                        continue;
                    }

                    held.add(NodeHeldPermission.of(holder, e));
                }
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
            return call(() -> {
                File trackFile = new File(tracksDir, name + ".json");
                registerFileAction("tracks", trackFile);

                if (trackFile.exists()) {
                    return readObjectFromFile(trackFile, element -> {
                        List<String> groups = new ArrayList<>();
                        for (JsonElement g : element.get("groups").getAsJsonArray()) {
                            groups.add(g.getAsString());
                        }
                        track.setGroups(groups);
                        return true;
                    });
                } else {
                    try {
                        trackFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }

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
            return call(() -> {
                File trackFile = new File(tracksDir, name + ".json");
                registerFileAction("tracks", trackFile);

                return trackFile.exists() && readObjectFromFile(trackFile, element -> {
                    List<String> groups = new ArrayList<>();
                    for (JsonElement g : element.get("groups").getAsJsonArray()) {
                        groups.add(g.getAsString());
                    }
                    track.setGroups(groups);
                    return true;
                });

            }, false);
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean saveTrack(Track track) {
        track.getIoLock().lock();
        try {
            return call(() -> {
                File trackFile = new File(tracksDir, track.getName() + ".json");
                registerFileAction("tracks", trackFile);

                if (!trackFile.exists()) {
                    try {
                        trackFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
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

    public static Set<NodeDataHolder> deserializePermissions(JsonArray permissionsSection) {
        Set<NodeDataHolder> nodes = new HashSet<>();

        for (JsonElement ent : permissionsSection) {
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
            ImmutableSetMultimap context = ImmutableSetMultimap.of();

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
                ImmutableSetMultimap.Builder<String, String> map = ImmutableSetMultimap.builder();

                for (Map.Entry<String, JsonElement> e : contexts.entrySet()) {
                    JsonElement val = e.getValue();
                    if (val.isJsonArray()) {
                        JsonArray vals = val.getAsJsonArray();
                        for (JsonElement element : vals) {
                            map.put(e.getKey(), element.getAsString());
                        }
                    } else {
                        map.put(e.getKey(), val.getAsString());
                    }
                }

                context = map.build();
            }

            nodes.add(NodeDataHolder.of(permission, value, server, world, expiry, context));
        }

        return nodes;
    }

    public static JsonArray serializePermissions(Set<NodeDataHolder> nodes) {
        List<JsonObject> data = new ArrayList<>();

        for (NodeDataHolder node : nodes) {
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
                JsonObject context = new JsonObject();
                Map<String, Collection<String>> map = node.getContexts().asMap();

                for (Map.Entry<String, Collection<String>> e : map.entrySet()) {
                    List<String> vals = new ArrayList<>(e.getValue());
                    int size = vals.size();

                    if (size == 1) {
                        context.addProperty(e.getKey(), vals.get(0));
                    } else if (size > 1) {
                        JsonArray arr = new JsonArray();
                        for (String s : vals) {
                            arr.add(new JsonPrimitive(s));
                        }
                        context.add(e.getKey(), arr);
                    }
                }

                attributes.add("context", context);
            }

            JsonObject perm = new JsonObject();
            perm.add(node.getPermission(), attributes);
            data.add(perm);
        }

        data.sort((o1, o2) -> PriorityComparator.get().compareStrings(
                Iterables.getFirst(o1.entrySet(), null).getKey(),
                Iterables.getFirst(o2.entrySet(), null).getKey()
        ));

        JsonArray arr = new JsonArray();
        for (JsonObject o : data) {
            arr.add(o);
        }

        return arr;
    }
}
