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

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneOptions;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.data.Log;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.storage.Datastore;
import me.lucko.luckperms.storage.DatastoreConfiguration;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.tracks.TrackManager;
import me.lucko.luckperms.users.User;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static me.lucko.luckperms.core.PermissionHolder.exportToLegacy;

@SuppressWarnings("unchecked")
public class MongoDBDatastore extends Datastore {

    private final DatastoreConfiguration configuration;
    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoDBDatastore(LuckPermsPlugin plugin, DatastoreConfiguration configuration) {
        super(plugin, "MongoDB");
        this.configuration = configuration;
    }

    @Override
    public void init() {
        MongoCredential credential = MongoCredential.createCredential(
                configuration.getUsername(),
                configuration.getDatabase(),
                configuration.getPassword().toCharArray()
        );

        ServerAddress address = new ServerAddress(
                configuration.getAddress().split(":")[0],
                Integer.parseInt(configuration.getAddress().split(":")[1])
        );

        mongoClient = new MongoClient(address, Collections.singletonList(credential));
        database = mongoClient.getDatabase(configuration.getDatabase());
        setAcceptingLogins(true);
    }

    @Override
    public void shutdown() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Override
    public boolean logAction(LogEntry entry) {
        return call(() -> {
            MongoCollection<Document> c = database.getCollection("action");

            Document doc = new Document()
                    .append("timestamp", entry.getTimestamp())
                    .append("actor", entry.getActor())
                    .append("actorName", entry.getActorName())
                    .append("type", Character.toString(entry.getType()))
                    .append("actedName", entry.getActedName())
                    .append("action", entry.getAction());

            if (entry.getActed() != null) {
                doc.append("acted", entry.getActed());
            }

            c.insertOne(doc, new InsertOneOptions());
            return true;
        }, false);
    }

    @Override
    public Log getLog() {
        return call(() -> {
            final Log.Builder log = Log.builder();
            MongoCollection<Document> c = database.getCollection("action");

            try (MongoCursor<Document> cursor = c.find().iterator()) {
                while (cursor.hasNext()) {
                    Document d = cursor.next();

                    UUID actedUuid = null;
                    if (d.containsKey("acted")) {
                        actedUuid = d.get("acted", UUID.class);
                    }

                    LogEntry e = new LogEntry(
                            d.getLong("timestamp"),
                            d.get("actor", UUID.class),
                            d.getString("actorName"),
                            d.getString("type").toCharArray()[0],
                            actedUuid,
                            d.getString("actedName"),
                            d.getString("action")
                    );
                    log.add(e);
                }
            }

            return log.build();
        }, null);
    }

    @Override
    public boolean loadUser(UUID uuid, String username) {
        User user = plugin.getUserManager().make(uuid, username);
        boolean success =  call(() -> {
            MongoCollection<Document> c = database.getCollection("users");

            try (MongoCursor<Document> cursor = c.find(new Document("_id", user.getUuid())).iterator()) {
                if (cursor.hasNext()) {
                    Document d = cursor.next();
                    user.setPrimaryGroup(d.getString("primaryGroup"));
                    user.setNodes(revert((Map<String, Boolean>) d.get("perms")));

                    if (user.getName().equalsIgnoreCase("null")) {
                        user.setName(d.getString("name"));
                    } else {
                        if (!d.getString("name").equals(user.getName())) {
                            c.replaceOne(new Document("_id", user.getUuid()), fromUser(user));
                        }
                    }
                }
            }
            return true;
        }, false);

        if (success) plugin.getUserManager().updateOrSet(user);
        return success;
    }

    @Override
    public boolean saveUser(User user) {
        if (!plugin.getUserManager().shouldSave(user)) {
            boolean success = call(() -> {
                MongoCollection<Document> c = database.getCollection("users");
                return c.deleteOne(new Document("_id", user.getUuid())).wasAcknowledged();
            }, false);
            return success;
        }

        boolean success = call(() -> {
            MongoCollection<Document> c = database.getCollection("users");
            try (MongoCursor<Document> cursor = c.find(new Document("_id", user.getUuid())).iterator()) {
                if (!cursor.hasNext()) {
                    c.insertOne(fromUser(user));
                } else {
                    c.replaceOne(new Document("_id", user.getUuid()), fromUser(user));
                }
            }
            return true;
        }, false);
        return success;
    }

    @Override
    public boolean cleanupUsers() {
        return true; // TODO
    }

    @Override
    public Set<UUID> getUniqueUsers() {
        Set<UUID> uuids = new HashSet<>();
        boolean success = call(() -> {
            MongoCollection<Document> c = database.getCollection("users");

            try (MongoCursor<Document> cursor = c.find().iterator()) {
                while (cursor.hasNext()) {
                    Document d = cursor.next();
                    uuids.add(UUID.fromString(d.getString("_id")));
                }
            }

            return true;
        }, false);

        return success ? uuids : null;
    }

    @Override
    public boolean createAndLoadGroup(String name) {
        Group group = plugin.getGroupManager().make(name);
        boolean success =  call(() -> {
            MongoCollection<Document> c = database.getCollection("groups");

            try (MongoCursor<Document> cursor = c.find(new Document("_id", group.getName())).iterator()) {
                if (!cursor.hasNext()) {
                    c.insertOne(fromGroup(group));
                } else {
                    Document d = cursor.next();
                    group.setNodes(revert((Map<String, Boolean>) d.get("perms")));
                }
            }
            return true;
        }, false);

        if (success) plugin.getGroupManager().updateOrSet(group);
        return success;
    }

    @Override
    public boolean loadGroup(String name) {
        Group group = plugin.getGroupManager().make(name);
        boolean success = call(() -> {
            MongoCollection<Document> c = database.getCollection("groups");

            try (MongoCursor<Document> cursor = c.find(new Document("_id", group.getName())).iterator()) {
                if (cursor.hasNext()) {
                    Document d = cursor.next();
                    group.setNodes(revert((Map<String, Boolean>) d.get("perms")));
                    return true;
                }
                return false;
            }
        }, false);

        if (success) plugin.getGroupManager().updateOrSet(group);
        return success;
    }

    @Override
    public boolean loadAllGroups() {
        List<Group> groups = new ArrayList<>();
        boolean success = call(() -> {
            MongoCollection<Document> c = database.getCollection("groups");

            try (MongoCursor<Document> cursor = c.find().iterator()) {
                while (cursor.hasNext()) {
                    Document d = cursor.next();
                    Group group = plugin.getGroupManager().make(d.getString("_id"));
                    group.setNodes(revert((Map<String, Boolean>) d.get("perms")));
                    groups.add(group);
                }
            }

            return true;
        }, false);

        if (success) {
            GroupManager gm = plugin.getGroupManager();
            gm.unloadAll();
            groups.forEach(gm::set);
        }
        return success;
    }

    @Override
    public boolean saveGroup(Group group) {
        return call(() -> {
            MongoCollection<Document> c = database.getCollection("groups");
            return c.replaceOne(new Document("_id", group.getName()), fromGroup(group)).wasAcknowledged();
        }, false);
    }

    @Override
    public boolean deleteGroup(Group group) {
        boolean success = call(() -> {
            MongoCollection<Document> c = database.getCollection("groups");
            return c.deleteOne(new Document("_id", group.getName())).wasAcknowledged();
        }, false);

        if (success) plugin.getGroupManager().unload(group);
        return success;
    }

    @Override
    public boolean createAndLoadTrack(String name) {
        Track track = plugin.getTrackManager().make(name);
        boolean success =  call(() -> {
            MongoCollection<Document> c = database.getCollection("tracks");

            try (MongoCursor<Document> cursor = c.find(new Document("_id", track.getName())).iterator()) {
                if (!cursor.hasNext()) {
                    c.insertOne(fromTrack(track));
                } else {
                    Document d = cursor.next();
                    track.setGroups((List<String>) d.get("groups"));
                }
            }
            return true;
        }, false);

        if (success) plugin.getTrackManager().updateOrSet(track);
        return success;
    }

    @Override
    public boolean loadTrack(String name) {
        Track track = plugin.getTrackManager().make(name);
        boolean success = call(() -> {
            MongoCollection<Document> c = database.getCollection("tracks");

            try (MongoCursor<Document> cursor = c.find(new Document("_id", track.getName())).iterator()) {
                if (cursor.hasNext()) {
                    Document d = cursor.next();
                    track.setGroups((List<String>) d.get("groups"));
                    return true;
                }
                return false;
            }
        }, false);

        if (success) plugin.getTrackManager().updateOrSet(track);
        return success;
    }

    @Override
    public boolean loadAllTracks() {
        List<Track> tracks = new ArrayList<>();
        boolean success = call(() -> {
            MongoCollection<Document> c = database.getCollection("tracks");

            try (MongoCursor<Document> cursor = c.find().iterator()) {
                while (cursor.hasNext()) {
                    Document d = cursor.next();
                    Track track = plugin.getTrackManager().make(d.getString("_id"));
                    track.setGroups((List<String>) d.get("groups"));
                    tracks.add(track);
                }
            }

            return true;
        }, false);

        if (success) {
            TrackManager tm = plugin.getTrackManager();
            tm.unloadAll();
            tracks.forEach(tm::set);
        }
        return success;
    }

    @Override
    public boolean saveTrack(Track track) {
        return call(() -> {
            MongoCollection<Document> c = database.getCollection("tracks");
            return c.replaceOne(new Document("_id", track.getName()), fromTrack(track)).wasAcknowledged();
        }, false);
    }

    @Override
    public boolean deleteTrack(Track track) {
        boolean success = call(() -> {
            MongoCollection<Document> c = database.getCollection("tracks");
            return c.deleteOne(new Document("_id", track.getName())).wasAcknowledged();
        }, false);

        if (success) plugin.getTrackManager().unload(track);
        return success;
    }

    @Override
    public boolean saveUUIDData(String username, UUID uuid) {
        return call(() -> {
            MongoCollection<Document> c = database.getCollection("uuid");

            try (MongoCursor<Document> cursor = c.find(new Document("_id", uuid)).iterator()) {
                if (cursor.hasNext()) {
                    c.replaceOne(new Document("_id", uuid), new Document("_id", uuid).append("name", username.toLowerCase()));
                } else {
                    c.insertOne(new Document("_id", uuid).append("name", username.toLowerCase()));
                }
            }

            return true;
        }, false);
    }

    @Override
    public UUID getUUID(String username) {
        return call(() -> {
            MongoCollection<Document> c = database.getCollection("uuid");

            try (MongoCursor<Document> cursor = c.find(new Document("name", username.toLowerCase())).iterator()) {
                if (cursor.hasNext()) {
                    return cursor.next().get("_id", UUID.class);
                }
            }
            return null;
        }, null);
    }

    @Override
    public String getName(UUID uuid) {
        return call(() -> {
            MongoCollection<Document> c = database.getCollection("uuid");

            try (MongoCursor<Document> cursor = c.find(new Document("_id", uuid)).iterator()) {
                if (cursor.hasNext()) {
                    return cursor.next().get("name", String.class);
                }
            }
            return null;
        }, null);
    }

    private static <T> T call(Callable<T> c, T def) {
        try {
            return c.call();
        } catch (Exception e) {
            e.printStackTrace();
            return def;
        }
    }

    /*  MongoDB does not allow '.' or '$' in key names.
        See: https://docs.mongodb.com/manual/reference/limits/#Restrictions-on-Field-Names
        The following two methods convert the node maps so they can be stored. */
    private static <V> Map<String, V> convert(Map<String, V> map) {
        return map.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey().replace(".", "[**DOT**]").replace("$", "[**DOLLAR**]"), e.getValue()))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    private static <V> Map<String, V> revert(Map<String, V> map) {
        return map.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey().replace("[**DOT**]", ".").replace("[**DOLLAR**]", "$"), e.getValue()))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    private static Document fromUser(User user) {
        Document main = new Document("_id", user.getUuid())
                .append("name", user.getName())
                .append("primaryGroup", user.getPrimaryGroup());

        Document perms = new Document();
        for (Map.Entry<String, Boolean> e : convert(exportToLegacy(user.getNodes())).entrySet()) {
            perms.append(e.getKey(), e.getValue());
        }

        main.append("perms", perms);
        return main;
    }

    private static Document fromGroup(Group group) {
        Document main = new Document("_id", group.getName());

        Document perms = new Document();
        for (Map.Entry<String, Boolean> e : convert(exportToLegacy(group.getNodes())).entrySet()) {
            perms.append(e.getKey(), e.getValue());
        }

        main.append("perms", perms);
        return main;
    }

    private static Document fromTrack(Track track) {
        return new Document("_id", track.getName()).append("groups", track.getGroups());
    }
}
