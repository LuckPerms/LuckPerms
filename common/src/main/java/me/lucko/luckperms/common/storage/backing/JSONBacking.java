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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.Cleanup;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.groups.GroupManager;
import me.lucko.luckperms.common.tracks.Track;
import me.lucko.luckperms.common.tracks.TrackManager;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.common.users.UserIdentifier;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static me.lucko.luckperms.common.core.PermissionHolder.exportToLegacy;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class JSONBacking extends FlatfileBacking {
    public JSONBacking(LuckPermsPlugin plugin, File pluginDir) {
        super(plugin, "Flatfile - JSON", pluginDir);
    }

    private boolean doWrite(File file, WriteOperation writeOperation) {
        boolean success = false;
        try {
            @Cleanup FileWriter fileWriter = new FileWriter(file);
            @Cleanup BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            @Cleanup JsonWriter jsonWriter = new JsonWriter(bufferedWriter);
            jsonWriter.setIndent("    ");
            success = writeOperation.onRun(jsonWriter);
            jsonWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return success;
    }

    private boolean doRead(File file, ReadOperation readOperation) {
        boolean success = false;
        try {
            @Cleanup FileReader fileReader = new FileReader(file);
            @Cleanup BufferedReader bufferedReader = new BufferedReader(fileReader);
            @Cleanup JsonReader jsonReader = new JsonReader(bufferedReader);
            success = readOperation.onRun(jsonReader);
        } catch (IOException e) {
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
                    return doRead(userFile, reader -> {
                        reader.beginObject();
                        reader.nextName(); // uuid record
                        reader.nextString(); // uuid
                        reader.nextName(); // name record
                        String name1 = reader.nextString(); // name
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
                            user.setName(name1);
                        } else {
                            if (!name1.equals(user.getName())) {
                                save = true;
                            }
                        }

                        if (save) {
                            doWrite(userFile, writer -> {
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
                    if (plugin.getUserManager().shouldSave(user)) {
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
                if (!plugin.getUserManager().shouldSave(user)) {
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

                return doWrite(userFile, writer -> {
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
                doRead(file, reader -> {
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
    public boolean createAndLoadGroup(String name) {
        Group group = plugin.getGroupManager().getOrMake(name);
        group.getIoLock().lock();
        try {
            return call(() -> {
                File groupFile = new File(groupsDir, name + ".json");
                if (groupFile.exists()) {
                    return doRead(groupFile, reader -> {
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

                    return doWrite(groupFile, writer -> {
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
                return groupFile.exists() && doRead(groupFile, reader -> {
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

                return doWrite(groupFile, writer -> {
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
    public boolean createAndLoadTrack(String name) {
        Track track = plugin.getTrackManager().getOrMake(name);
        track.getIoLock().lock();
        try {
            return call(() -> {
                File trackFile = new File(tracksDir, name + ".json");
                if (trackFile.exists()) {
                    return doRead(trackFile, reader -> {
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

                    return doWrite(trackFile, writer -> {
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
                return trackFile.exists() && doRead(trackFile, reader -> {
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

                return doWrite(trackFile, writer -> {
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

    private static <T> T call(Callable<T> c, T def) {
        try {
            return c.call();
        } catch (Exception e) {
            e.printStackTrace();
            return def;
        }
    }

    interface WriteOperation {
        boolean onRun(JsonWriter writer) throws IOException;
    }

    interface ReadOperation {
        boolean onRun(JsonReader reader) throws IOException;
    }
}
