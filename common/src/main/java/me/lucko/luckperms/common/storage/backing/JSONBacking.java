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
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.UserIdentifier;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.managers.GroupManager;
import me.lucko.luckperms.common.managers.TrackManager;
import me.lucko.luckperms.common.managers.impl.GenericUserManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.holder.NodeHeldPermission;
import me.lucko.luckperms.common.utils.ThrowingFunction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static me.lucko.luckperms.common.core.model.PermissionHolder.exportToLegacy;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class JSONBacking extends FlatfileBacking {
    private static <T> T call(Callable<T> c, T def) {
        try {
            return c.call();
        } catch (Exception e) {
            e.printStackTrace();
            return def;
        }
    }

    public JSONBacking(LuckPermsPlugin plugin, File pluginDir) {
        super(plugin, "JSON", pluginDir);
    }

    private boolean fileToWriter(File file, ThrowingFunction<JsonWriter, Boolean> writeOperation) {
        boolean success = false;
        try {
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                try (OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                    try (BufferedWriter bufferedWriter = new BufferedWriter(outputWriter)) {
                        try (JsonWriter jsonWriter = new JsonWriter(bufferedWriter)) {
                            jsonWriter.setIndent("    ");
                            success = writeOperation.apply(jsonWriter);
                            jsonWriter.flush();
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLog().warn("Exception whilst writing to file: " + file.getAbsolutePath());
            e.printStackTrace();
        }
        return success;
    }

    private boolean fileToReader(File file, ThrowingFunction<JsonReader, Boolean> readOperation) {
        boolean success = false;
        try {
            try (FileInputStream fileInput = new FileInputStream(file)) {
                try (InputStreamReader inputReader = new InputStreamReader(fileInput, StandardCharsets.UTF_8)) {
                    try (BufferedReader bufferedReader = new BufferedReader(inputReader)) {
                        try (JsonReader jsonReader = new JsonReader(bufferedReader)) {
                            success = readOperation.apply(jsonReader);
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLog().warn("Exception whilst reading from file: " + file.getAbsolutePath());
            e.printStackTrace();
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
                if (userFile.exists()) {
                    return fileToReader(userFile, reader -> {
                        reader.beginObject();
                        reader.nextName(); // uuid record
                        reader.nextString(); // uuid
                        reader.nextName(); // name record
                        String name = reader.nextString(); // name
                        reader.nextName(); // primaryGroup record
                        user.setPrimaryGroup(reader.nextString()); // primaryGroup
                        reader.nextName(); // perms
                        reader.beginObject();
                        Map<String, Boolean> map = new HashMap<>();
                        while (reader.hasNext()) {
                            String node = reader.nextName();
                            boolean b = reader.nextBoolean();
                            map.put(node, b);
                        }
                        user.setNodes(map);
                        reader.endObject();
                        reader.endObject();

                        boolean save = plugin.getUserManager().giveDefaultIfNeeded(user, false);

                        if (user.getName() == null || user.getName().equalsIgnoreCase("null")) {
                            user.setName(name);
                        } else {
                            if (!name.equalsIgnoreCase(user.getName())) {
                                save = true;
                            }
                        }

                        if (save) {
                            fileToWriter(userFile, writer -> {
                                writer.beginObject();
                                writer.name("uuid").value(user.getUuid().toString());
                                writer.name("name").value(user.getName());
                                writer.name("primaryGroup").value(user.getPrimaryGroup());
                                writer.name("perms");
                                writer.beginObject();
                                for (Map.Entry<String, Boolean> e : exportToLegacy(user.getNodes()).entrySet()) {
                                    writer.name(e.getKey()).value(e.getValue().booleanValue());
                                }
                                writer.endObject();
                                writer.endObject();
                                return true;
                            });
                        }
                        return true;
                    });
                } else {
                    if (GenericUserManager.shouldSave(user)) {
                        user.clearNodes();
                        user.setPrimaryGroup(null);
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

                return fileToWriter(userFile, writer -> {
                    writer.beginObject();
                    writer.name("uuid").value(user.getUuid().toString());
                    writer.name("name").value(user.getName());
                    writer.name("primaryGroup").value(user.getPrimaryGroup());
                    writer.name("perms");
                    writer.beginObject();
                    for (Map.Entry<String, Boolean> e : exportToLegacy(user.getNodes()).entrySet()) {
                        writer.name(e.getKey()).value(e.getValue().booleanValue());
                    }
                    writer.endObject();
                    writer.endObject();
                    return true;
                });
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
                Map<String, Boolean> nodes = new HashMap<>();
                fileToReader(file, reader -> {
                    reader.beginObject();
                    reader.nextName(); // uuid record
                    reader.nextString(); // uuid
                    reader.nextName(); // name record
                    reader.nextString(); // name
                    reader.nextName(); // primaryGroup record
                    reader.nextString(); // primaryGroup
                    reader.nextName(); //perms
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String node = reader.nextName();
                        boolean b = reader.nextBoolean();
                        nodes.put(node, b);
                    }

                    reader.endObject();
                    reader.endObject();
                    return true;
                });

                boolean shouldDelete = false;
                if (nodes.size() == 1) {
                    for (Map.Entry<String, Boolean> e : nodes.entrySet()) {
                        // There's only one
                        shouldDelete = e.getKey().equalsIgnoreCase("group.default") && e.getValue();
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
    public Set<UUID> getUniqueUsers() {
        String[] fileNames = usersDir.list((dir, name) -> name.endsWith(".json"));
        if (fileNames == null) return null;
        return Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - 5))
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }

    @Override
    public List<HeldPermission<UUID>> getUsersWithPermission(String permission) {
        ImmutableList.Builder<HeldPermission<UUID>> held = ImmutableList.builder();
        boolean success = call(() -> {
            File[] files = usersDir.listFiles((dir, name1) -> name1.endsWith(".json"));
            if (files == null) return false;

            for (File file : files) {
                UUID holder = UUID.fromString(file.getName().substring(0, file.getName().length() - 5));
                Map<String, Boolean> nodes = new HashMap<>();
                fileToReader(file, reader -> {
                    reader.beginObject();
                    reader.nextName(); // uuid record
                    reader.nextString(); // uuid
                    reader.nextName(); // name record
                    reader.nextString(); // name
                    reader.nextName(); // primaryGroup record
                    reader.nextString(); // primaryGroup
                    reader.nextName(); //perms
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String node = reader.nextName();
                        boolean b = reader.nextBoolean();
                        nodes.put(node, b);
                    }

                    reader.endObject();
                    reader.endObject();
                    return true;
                });

                for (Map.Entry<String, Boolean> e : nodes.entrySet()) {
                    Node node = NodeFactory.fromSerialisedNode(e.getKey(), e.getValue());
                    if (!node.getPermission().equalsIgnoreCase(permission)) {
                        continue;
                    }

                    held.add(NodeHeldPermission.of(holder, node));
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
                if (groupFile.exists()) {
                    return fileToReader(groupFile, reader -> {
                        reader.beginObject();
                        reader.nextName(); // name record
                        reader.nextString(); // name
                        reader.nextName(); //perms
                        reader.beginObject();
                        Map<String, Boolean> map = new HashMap<>();
                        while (reader.hasNext()) {
                            String node = reader.nextName();
                            boolean b = reader.nextBoolean();
                            map.put(node, b);
                        }
                        group.setNodes(map);

                        reader.endObject();
                        reader.endObject();
                        return true;
                    });
                } else {
                    try {
                        groupFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }

                    return fileToWriter(groupFile, writer -> {
                        writer.beginObject();
                        writer.name("name").value(group.getName());
                        writer.name("perms");
                        writer.beginObject();
                        for (Map.Entry<String, Boolean> e : exportToLegacy(group.getNodes()).entrySet()) {
                            writer.name(e.getKey()).value(e.getValue().booleanValue());
                        }
                        writer.endObject();
                        writer.endObject();
                        return true;
                    });
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
                return groupFile.exists() && fileToReader(groupFile, reader -> {
                    reader.beginObject();
                    reader.nextName(); // name record
                    reader.nextString(); // name
                    reader.nextName(); // perms
                    reader.beginObject();
                    Map<String, Boolean> map = new HashMap<>();
                    while (reader.hasNext()) {
                        String node = reader.nextName();
                        boolean b = reader.nextBoolean();
                        map.put(node, b);
                    }
                    group.setNodes(map);
                    reader.endObject();
                    reader.endObject();
                    return true;
                });
            }, false);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean loadAllGroups() {
        String[] fileNames = groupsDir.list((dir, name) -> name.endsWith(".json"));
        if (fileNames == null) return false;
        List<String> groups = Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - 5))
                .collect(Collectors.toList());

        groups.forEach(this::loadGroup);

        GroupManager gm = plugin.getGroupManager();
        gm.getAll().values().stream()
                .filter(g -> !groups.contains(g.getName()))
                .forEach(gm::unload);
        return true;
    }

    @Override
    public boolean saveGroup(Group group) {
        group.getIoLock().lock();
        try {
            return call(() -> {
                File groupFile = new File(groupsDir, group.getName() + ".json");
                if (!groupFile.exists()) {
                    try {
                        groupFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }

                return fileToWriter(groupFile, writer -> {
                    writer.beginObject();
                    writer.name("name").value(group.getName());
                    writer.name("perms");
                    writer.beginObject();
                    for (Map.Entry<String, Boolean> e : exportToLegacy(group.getNodes()).entrySet()) {
                        writer.name(e.getKey()).value(e.getValue().booleanValue());
                    }
                    writer.endObject();
                    writer.endObject();
                    return true;
                });
            }, false);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean deleteGroup(Group group) {
        group.getIoLock().lock();
        try {
            return call(() -> {
                File groupFile = new File(groupsDir, group.getName() + ".json");
                if (groupFile.exists()) {
                    groupFile.delete();
                }
                return true;
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
                String holder = file.getName().substring(0, file.getName().length() - 5);
                Map<String, Boolean> nodes = new HashMap<>();
                fileToReader(file, reader -> {
                    reader.beginObject();
                    reader.nextName(); // name record
                    reader.nextString(); // name
                    reader.nextName(); // perms
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String node = reader.nextName();
                        boolean b = reader.nextBoolean();
                        nodes.put(node, b);
                    }

                    reader.endObject();
                    reader.endObject();
                    return true;
                });

                for (Map.Entry<String, Boolean> e : nodes.entrySet()) {
                    Node node = NodeFactory.fromSerialisedNode(e.getKey(), e.getValue());
                    if (!node.getPermission().equalsIgnoreCase(permission)) {
                        continue;
                    }

                    held.add(NodeHeldPermission.of(holder, node));
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
                if (trackFile.exists()) {
                    return fileToReader(trackFile, reader -> {
                        reader.beginObject();
                        reader.nextName(); // name record
                        reader.nextString(); // name
                        reader.nextName(); // groups record
                        reader.beginArray();
                        List<String> groups = new ArrayList<>();
                        while (reader.hasNext()) {
                            groups.add(reader.nextString());
                        }
                        track.setGroups(groups);
                        reader.endArray();
                        reader.endObject();
                        return true;
                    });
                } else {
                    try {
                        trackFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }

                    return fileToWriter(trackFile, writer -> {
                        writer.beginObject();
                        writer.name("name").value(track.getName());
                        writer.name("groups");
                        writer.beginArray();
                        for (String s : track.getGroups()) {
                            writer.value(s);
                        }
                        writer.endArray();
                        writer.endObject();
                        return true;
                    });
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
                return trackFile.exists() && fileToReader(trackFile, reader -> {
                    reader.beginObject();
                    reader.nextName(); // name record
                    reader.nextString(); // name
                    reader.nextName(); // groups
                    reader.beginArray();
                    List<String> groups = new ArrayList<>();
                    while (reader.hasNext()) {
                        groups.add(reader.nextString());
                    }
                    track.setGroups(groups);
                    reader.endArray();
                    reader.endObject();
                    return true;
                });

            }, false);
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean loadAllTracks() {
        String[] fileNames = tracksDir.list((dir, name) -> name.endsWith(".json"));
        if (fileNames == null) return false;
        List<String> tracks = Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - 5))
                .collect(Collectors.toList());

        tracks.forEach(this::loadTrack);

        TrackManager tm = plugin.getTrackManager();
        tm.getAll().values().stream()
                .filter(t -> !tracks.contains(t.getName()))
                .forEach(tm::unload);
        return true;
    }

    @Override
    public boolean saveTrack(Track track) {
        track.getIoLock().lock();
        try {
            return call(() -> {
                File trackFile = new File(tracksDir, track.getName() + ".json");
                if (!trackFile.exists()) {
                    try {
                        trackFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }

                return fileToWriter(trackFile, writer -> {
                    writer.beginObject();
                    writer.name("name").value(track.getName());
                    writer.name("groups");
                    writer.beginArray();
                    for (String s : track.getGroups()) {
                        writer.value(s);
                    }
                    writer.endArray();
                    writer.endObject();
                    return true;
                });
            }, false);
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean deleteTrack(Track track) {
        track.getIoLock().lock();
        try {
            return call(() -> {
                File trackFile = new File(tracksDir, track.getName() + ".json");
                if (trackFile.exists()) {
                    trackFile.delete();
                }
                return true;
            }, false);
        } finally {
            track.getIoLock().unlock();
        }
    }
}
