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

package me.lucko.luckperms.storage.methods;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.Cleanup;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.core.Node;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.users.User;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static me.lucko.luckperms.core.PermissionHolder.exportToLegacy;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class JSONDatastore extends FlatfileDatastore {
    public JSONDatastore(LuckPermsPlugin plugin, File pluginDir) {
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
        User user = plugin.getUserManager().make(uuid, username);
        boolean success = false;

        File userFile = new File(usersDir, uuid.toString() + ".json");
        if (userFile.exists()) {
            final String[] name = new String[1];
            success = doRead(userFile, reader -> {
                reader.beginObject();
                reader.nextName(); // uuid record
                reader.nextString(); // uuid
                reader.nextName(); // name record
                name[0] = reader.nextString(); // name
                reader.nextName(); // primaryGroup record
                user.setPrimaryGroup(reader.nextString()); // primaryGroup
                reader.nextName(); //perms
                reader.beginObject();
                while (reader.hasNext()) {
                    String node = reader.nextName();
                    boolean b = reader.nextBoolean();
                    user.getNodes().add(Node.fromSerialisedNode(node, b));
                }

                reader.endObject();
                reader.endObject();
                return true;
            });

            if (user.getName().equalsIgnoreCase("null")) {
                user.setName(name[0]);
            } else {
                if (!name[0].equals(user.getName())) {
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
            }

        } else {
            success = true;
        }

        if (success) plugin.getUserManager().updateOrSet(user);
        return success;
    }

    @Override
    public boolean saveUser(User user) {
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
    }

    @Override
    public boolean cleanupUsers() {
        File[] files = usersDir.listFiles((dir, name) -> name.endsWith(".json"));
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
        Group group = plugin.getGroupManager().make(name);

        File groupFile = new File(groupsDir, name + ".json");
        if (!groupFile.exists()) {
            try {
                groupFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            boolean success = doWrite(groupFile, writer -> {
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

            if (!success) return false;
        }

        boolean success = doRead(groupFile, reader -> {
            reader.beginObject();
            reader.nextName(); // name record
            reader.nextString(); // name
            reader.nextName(); //perms
            reader.beginObject();
            while (reader.hasNext()) {
                String node = reader.nextName();
                boolean b = reader.nextBoolean();
                group.getNodes().add(Node.fromSerialisedNode(node, b));
            }

            reader.endObject();
            reader.endObject();
            return true;
        });

        if (success) plugin.getGroupManager().updateOrSet(group);
        return success;
    }

    @Override
    public boolean loadGroup(String name) {
        Group group = plugin.getGroupManager().make(name);

        File groupFile = new File(groupsDir, name + ".json");
        if (!groupFile.exists()) {
            return false;
        }

        boolean success = doRead(groupFile, reader -> {
            reader.beginObject();
            reader.nextName(); // name record
            reader.nextString(); // name
            reader.nextName(); //perms
            reader.beginObject();
            while (reader.hasNext()) {
                String node = reader.nextName();
                boolean b = reader.nextBoolean();
                group.getNodes().add(Node.fromSerialisedNode(node, b));
            }

            reader.endObject();
            reader.endObject();
            return true;
        });

        if (success) plugin.getGroupManager().updateOrSet(group);
        return success;
    }

    @Override
    public boolean loadAllGroups() {
        String[] fileNames = groupsDir.list((dir, name) -> name.endsWith(".json"));
        if (fileNames == null) return false;
        List<String> groups = Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - 5))
                .collect(Collectors.toList());

        plugin.getGroupManager().unloadAll();
        groups.forEach(this::loadGroup);
        return true;
    }

    @Override
    public boolean saveGroup(Group group) {
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
    }

    @Override
    public boolean deleteGroup(Group group) {
        File groupFile = new File(groupsDir, group.getName() + ".json");
        if (groupFile.exists()) {
            groupFile.delete();
        }
        return true;
    }

    @Override
    public boolean createAndLoadTrack(String name) {
        Track track = plugin.getTrackManager().make(name);
        List<String> groups = new ArrayList<>();

        File trackFile = new File(tracksDir, name + ".json");
        if (!trackFile.exists()) {
            try {
                trackFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            boolean success = doWrite(trackFile, writer -> {
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

            if (!success) return false;
        }

        boolean success = doRead(trackFile, reader -> {
            reader.beginObject();
            reader.nextName(); // name record
            reader.nextString(); // name
            reader.nextName(); // groups record
            reader.beginArray();
            while (reader.hasNext()) {
                groups.add(reader.nextString());
            }
            reader.endArray();
            reader.endObject();
            return true;
        });

        track.setGroups(groups);
        if (success) plugin.getTrackManager().updateOrSet(track);
        return success;
    }

    @Override
    public boolean loadTrack(String name) {
        Track track = plugin.getTrackManager().make(name);
        List<String> groups = new ArrayList<>();

        File trackFile = new File(tracksDir, name + ".json");
        if (!trackFile.exists()) {
            return false;
        }

        boolean success = doRead(trackFile, reader -> {
            reader.beginObject();
            reader.nextName(); // name record
            reader.nextString(); // name
            reader.nextName(); // groups record
            reader.beginArray();
            while (reader.hasNext()) {
                groups.add(reader.nextString());
            }
            reader.endArray();
            reader.endObject();
            return true;
        });

        track.setGroups(groups);
        if (success) plugin.getTrackManager().updateOrSet(track);
        return success;
    }

    @Override
    public boolean loadAllTracks() {
        String[] fileNames = tracksDir.list((dir, name) -> name.endsWith(".json"));
        if (fileNames == null) return false;
        List<String> tracks = Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - 5))
                .collect(Collectors.toList());

        plugin.getTrackManager().unloadAll();
        tracks.forEach(this::loadTrack);
        return true;
    }

    @Override
    public boolean saveTrack(Track track) {
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
    }

    @Override
    public boolean deleteTrack(Track track) {
        File trackFile = new File(tracksDir, track.getName() + ".json");
        if (trackFile.exists()) {
            trackFile.delete();
        }
        return true;
    }

    interface WriteOperation {
        boolean onRun(JsonWriter writer) throws IOException;
    }

    interface ReadOperation {
        boolean onRun(JsonReader reader) throws IOException;
    }
}
