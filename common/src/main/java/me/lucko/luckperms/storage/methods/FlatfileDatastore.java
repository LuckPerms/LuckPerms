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
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.constants.Constants;
import me.lucko.luckperms.data.Log;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.storage.Datastore;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.users.User;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;
import java.util.logging.Formatter;
import java.util.stream.Collectors;

@SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryLocalVariable"})
public class FlatfileDatastore extends Datastore {
    private static final String LOG_FORMAT = "%s(%s): [%s] %s(%s) --> %s";

    private final Logger actionLogger = Logger.getLogger("lp_actions");
    private Map<String, String> uuidCache = new ConcurrentHashMap<>();

    private final File pluginDir;
    private File usersDir;
    private File groupsDir;
    private File tracksDir;
    private File uuidData;
    private File actionLog;

    public FlatfileDatastore(LuckPermsPlugin plugin, File pluginDir) {
        super(plugin, "Flatfile - JSON");
        this.pluginDir = pluginDir;
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
    public void init() {
        try {
            makeFiles();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        uuidCache.putAll(getUUIDCache());

        try {
            FileHandler fh = new FileHandler(actionLog.getAbsolutePath(), 0, 1, true);
            fh.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return new Date(record.getMillis()).toString() + ": " + record.getMessage() + "\n";
                }
            });
            actionLogger.addHandler(fh);
            actionLogger.setUseParentHandlers(false);
            actionLogger.setLevel(Level.ALL);
            actionLogger.setFilter(record -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setAcceptingLogins(true);
    }

    private void makeFiles() throws IOException {
        File data = new File(pluginDir, "data");
        data.mkdirs();

        usersDir = new File(data, "users");
        usersDir.mkdir();

        groupsDir = new File(data, "groups");
        groupsDir.mkdir();

        tracksDir = new File(data, "tracks");
        tracksDir.mkdir();

        uuidData = new File(data, "uuidcache.txt");
        uuidData.createNewFile();

        actionLog = new File(data, "actions.log");
        actionLog.createNewFile();
    }

    @Override
    public void shutdown() {
        saveUUIDCache(uuidCache);
    }

    @Override
    public boolean logAction(LogEntry entry) {
        actionLogger.info(String.format(LOG_FORMAT,
                (entry.getActor().equals(Constants.getConsoleUUID()) ? "" : entry.getActor() + " "),
                entry.getActorName(),
                Character.toString(entry.getType()),
                (entry.getActed() == null ? "" : entry.getActed().toString() + " "),
                entry.getActedName(),
                entry.getAction())
        );
        return true;
    }

    @Override
    public Log getLog() {
        // TODO Add log viewing support for flatfile D:
        return Log.builder().build();
    }

    @Override
    public boolean loadOrCreateUser(UUID uuid, String username) {
        User user = plugin.getUserManager().make(uuid, username);

        File userFile = new File(usersDir, uuid.toString() + ".json");
        if (!userFile.exists()) {
            try {
                userFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            plugin.getUserManager().giveDefaults(user);

            boolean success = doWrite(userFile, writer -> {
                writer.beginObject();
                writer.name("uuid").value(user.getUuid().toString());
                writer.name("name").value(user.getName());
                writer.name("primaryGroup").value(user.getPrimaryGroup());
                writer.name("perms");
                writer.beginObject();
                for (Map.Entry<String, Boolean> e : user.getNodes().entrySet()) {
                    writer.name(e.getKey()).value(e.getValue().booleanValue());
                }
                writer.endObject();
                writer.endObject();
                return true;
            });

            if (!success) return false;
        }

        final String[] name = new String[1];
        boolean success = doRead(userFile, reader -> {
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
                user.getNodes().put(node, b);
            }

            reader.endObject();
            reader.endObject();
            return true;
        });

        if (!name[0].equals(user.getName())) {
            doWrite(userFile, writer -> {
                writer.beginObject();
                writer.name("uuid").value(user.getUuid().toString());
                writer.name("name").value(user.getName());
                writer.name("primaryGroup").value(user.getPrimaryGroup());
                writer.name("perms");
                writer.beginObject();
                for (Map.Entry<String, Boolean> e : user.getNodes().entrySet()) {
                    writer.name(e.getKey()).value(e.getValue().booleanValue());
                }
                writer.endObject();
                writer.endObject();
                return true;
            });
        }

        if (success) plugin.getUserManager().updateOrSet(user);
        return success;
    }

    @Override
    public boolean loadUser(UUID uuid) {
        User user = plugin.getUserManager().make(uuid);

        File userFile = new File(usersDir, uuid.toString() + ".json");
        if (!userFile.exists()) {
            return false;
        }

        boolean success = doRead(userFile, reader -> {
            reader.beginObject();
            reader.nextName(); // uuid record
            reader.nextString(); // uuid
            reader.nextName(); // name record
            user.setName(reader.nextString()); // name
            reader.nextName(); // primaryGroup record
            user.setPrimaryGroup(reader.nextString()); // primaryGroup
            reader.nextName(); // perms record
            reader.beginObject();
            while (reader.hasNext()) {
                String node = reader.nextName();
                boolean b = reader.nextBoolean();
                user.getNodes().put(node, b);
            }

            reader.endObject();
            reader.endObject();
            return true;
        });

        if (success) plugin.getUserManager().updateOrSet(user);
        return success;
    }

    @Override
    public boolean saveUser(User user) {
        File userFile = new File(usersDir, user.getUuid().toString() + ".json");
        if (!userFile.exists()) {
            try {
                userFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        boolean success = doWrite(userFile, writer -> {
            writer.beginObject();
            writer.name("uuid").value(user.getUuid().toString());
            writer.name("name").value(user.getName());
            writer.name("primaryGroup").value(user.getPrimaryGroup());
            writer.name("perms");
            writer.beginObject();
            for (Map.Entry<String, Boolean> e : user.getNodes().entrySet()) {
                writer.name(e.getKey()).value(e.getValue().booleanValue());
            }
            writer.endObject();
            writer.endObject();
            return true;
        });
        return success;
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
                for (Map.Entry<String, Boolean> e : group.getNodes().entrySet()) {
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
                group.getNodes().put(node, b);
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
                group.getNodes().put(node, b);
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

        boolean success = doWrite(groupFile, writer -> {
            writer.beginObject();
            writer.name("name").value(group.getName());
            writer.name("perms");
            writer.beginObject();
            for (Map.Entry<String, Boolean> e : group.getNodes().entrySet()) {
                writer.name(e.getKey()).value(e.getValue().booleanValue());
            }
            writer.endObject();
            writer.endObject();
            return true;
        });

        return success;
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

        return success;
    }

    @Override
    public boolean deleteTrack(Track track) {
        File trackFile = new File(tracksDir, track.getName() + ".json");
        if (trackFile.exists()) {
            trackFile.delete();
        }
        return true;
    }

    private Map<String, String> getUUIDCache() {
        Map<String, String> cache = new HashMap<>();

        try {
            @Cleanup FileReader fileReader = new FileReader(uuidData);
            @Cleanup BufferedReader bufferedReader = new BufferedReader(fileReader);

            Properties props = new Properties();
            props.load(bufferedReader);
            for (String key : props.stringPropertyNames()) {
                cache.put(key, props.getProperty(key));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return cache;
    }

    private void saveUUIDCache(Map<String, String> cache) {
        try {
            @Cleanup FileWriter fileWriter = new FileWriter(uuidData);
            @Cleanup BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            Properties properties = new Properties();
            properties.putAll(cache);
            properties.store(bufferedWriter, null);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean saveUUIDData(String username, UUID uuid) {
        username = username.toLowerCase();
        uuidCache.put(username, uuid.toString());
        return true;
    }

    @Override
    public UUID getUUID(String username) {
        username = username.toLowerCase();
        if (uuidCache.get(username) == null) return null;
        return UUID.fromString(uuidCache.get(username));
    }

    interface WriteOperation {
        boolean onRun(JsonWriter writer) throws IOException;
    }

    interface ReadOperation {
        boolean onRun(JsonReader reader) throws IOException;
    }
}
