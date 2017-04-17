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
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneOptions;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.NodeModel;
import me.lucko.luckperms.common.core.UserIdentifier;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.Log;
import me.lucko.luckperms.common.managers.GroupManager;
import me.lucko.luckperms.common.managers.TrackManager;
import me.lucko.luckperms.common.managers.impl.GenericUserManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.DatastoreConfiguration;
import me.lucko.luckperms.common.storage.holder.NodeHeldPermission;

import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class MongoDBBacking extends AbstractBacking {

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

    private static final Function<String, String> CONVERT_STRING = s -> s.replace(".", "[**DOT**]").replace("$", "[**DOLLAR**]");
    private static final Function<String, String> REVERT_STRING = s -> s.replace("[**DOT**]", ".").replace("[**DOLLAR**]", "$");

    private static <V> Map<String, V> convert(Map<String, V> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(e -> CONVERT_STRING.apply(e.getKey()), Map.Entry::getValue));
    }

    private static <V> Map<String, V> revert(Map<String, V> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(e -> REVERT_STRING.apply(e.getKey()), Map.Entry::getValue));
    }

    private static Document fromUser(User user) {
        Document main = new Document("_id", user.getUuid())
                .append("name", user.getName().orElse("null"))
                .append("primaryGroup", user.getPrimaryGroup().getStoredValue());

        Document perms = new Document();
        for (Map.Entry<String, Boolean> e : convert(exportToLegacy(user.getNodes().values())).entrySet()) {
            perms.append(e.getKey(), e.getValue());
        }

        main.append("perms", perms);
        return main;
    }

    private static Document fromGroup(Group group) {
        Document main = new Document("_id", group.getName());

        Document perms = new Document();
        for (Map.Entry<String, Boolean> e : convert(exportToLegacy(group.getNodes().values())).entrySet()) {
            perms.append(e.getKey(), e.getValue());
        }

        main.append("perms", perms);
        return main;
    }

    private static Document fromTrack(Track track) {
        return new Document("_id", track.getName()).append("groups", track.getGroups());
    }

    private final DatastoreConfiguration configuration;
    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoDBBacking(LuckPermsPlugin plugin, DatastoreConfiguration configuration) {
        super(plugin, "MongoDB");
        this.configuration = configuration;
    }

    @Override
    public void init() {
        MongoCredential credential = null;

        if (configuration.getUsername() != null && !configuration.getUsername().equals("") && configuration.getDatabase() != null && !configuration.getDatabase().equals("")) {
            if (configuration.getPassword() == null || configuration.getPassword().equals("")) {
                credential = MongoCredential.createCredential(
                        configuration.getUsername(),
                        configuration.getDatabase(), null
                );
            } else {
                credential = MongoCredential.createCredential(
                        configuration.getUsername(),
                        configuration.getDatabase(),
                        configuration.getPassword().toCharArray()
                );
            }
        }

        String[] addressSplit = configuration.getAddress().split(":");
        String host = addressSplit[0];
        int port = addressSplit.length > 1 ? Integer.parseInt(addressSplit[1]) : 27017;
        ServerAddress address = new ServerAddress(host, port);

        if (credential == null) {
            mongoClient = new MongoClient(address, Collections.emptyList());
        } else {
            mongoClient = new MongoClient(address, Collections.singletonList(credential));
        }

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
    public boolean applyBulkUpdate(BulkUpdate bulkUpdate) {
        return call(() -> {
            if (bulkUpdate.getDataType().isIncludingUsers()) {
                MongoCollection<Document> c = database.getCollection("users");

                try (MongoCursor<Document> cursor = c.find().iterator()) {
                    while (cursor.hasNext()) {
                        Document d = cursor.next();

                        UUID uuid = UUID.fromString(d.getString("_id"));
                        Map<String, Boolean> perms = revert((Map<String, Boolean>) d.get("perms"));

                        Set<NodeModel> nodes = new HashSet<>();
                        for (Map.Entry<String, Boolean> e : perms.entrySet()) {
                            Node node = NodeFactory.fromSerializedNode(e.getKey(), e.getValue());
                            nodes.add(NodeModel.fromNode(node));
                        }

                        Set<Node> results = nodes.stream()
                                .map(n -> Optional.ofNullable(bulkUpdate.apply(n)))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .map(NodeModel::toNode)
                                .collect(Collectors.toSet());

                        Document permsDoc = new Document();
                        for (Map.Entry<String, Boolean> e : convert(exportToLegacy(results)).entrySet()) {
                            permsDoc.append(e.getKey(), e.getValue());
                        }

                        d.put("perms", perms);
                        c.replaceOne(new Document("_id", uuid), d);
                    }
                }
            }

            if (bulkUpdate.getDataType().isIncludingGroups()) {
                MongoCollection<Document> c = database.getCollection("groups");

                try (MongoCursor<Document> cursor = c.find().iterator()) {
                    while (cursor.hasNext()) {
                        Document d = cursor.next();

                        String holder = d.getString("_id");
                        Map<String, Boolean> perms = revert((Map<String, Boolean>) d.get("perms"));

                        Set<NodeModel> nodes = new HashSet<>();
                        for (Map.Entry<String, Boolean> e : perms.entrySet()) {
                            Node node = NodeFactory.fromSerializedNode(e.getKey(), e.getValue());
                            nodes.add(NodeModel.fromNode(node));
                        }

                        Set<Node> results = nodes.stream()
                                .map(n -> Optional.ofNullable(bulkUpdate.apply(n)))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .map(NodeModel::toNode)
                                .collect(Collectors.toSet());

                        Document permsDoc = new Document();
                        for (Map.Entry<String, Boolean> e : convert(exportToLegacy(results)).entrySet()) {
                            permsDoc.append(e.getKey(), e.getValue());
                        }

                        d.put("perms", perms);
                        c.replaceOne(new Document("_id", holder), d);
                    }
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
            return call(() -> {
                MongoCollection<Document> c = database.getCollection("users");

                try (MongoCursor<Document> cursor = c.find(new Document("_id", user.getUuid())).iterator()) {
                    if (cursor.hasNext()) {
                        // User exists, let's load.
                        Document d = cursor.next();
                        user.setNodes(revert((Map<String, Boolean>) d.get("perms")).entrySet().stream()
                                .map(e -> NodeFactory.fromSerializedNode(e.getKey(), e.getValue()))
                                .collect(Collectors.toSet())
                        );
                        user.getPrimaryGroup().setStoredValue(d.getString("primaryGroup"));

                        boolean save = plugin.getUserManager().giveDefaultIfNeeded(user, false);

                        if (user.setName(name, false)) {
                            save = true;
                        }

                        if (save) {
                            c.replaceOne(new Document("_id", user.getUuid()), fromUser(user));
                        }
                    } else {
                        if (GenericUserManager.shouldSave(user)) {
                            user.clearNodes();
                            user.getPrimaryGroup().setStoredValue(null);
                            plugin.getUserManager().giveDefaultIfNeeded(user, false);
                        }
                    }
                }
                return true;
            }, false);
        } finally {
            user.getIoLock().unlock();
            user.getRefreshBuffer().requestDirectly();
        }
    }

    @Override
    public boolean saveUser(User user) {
        if (!GenericUserManager.shouldSave(user)) {
            user.getIoLock().lock();
            try {
                return call(() -> {
                    MongoCollection<Document> c = database.getCollection("users");
                    return c.deleteOne(new Document("_id", user.getUuid())).wasAcknowledged();
                }, false);
            } finally {
                user.getIoLock().unlock();
            }
        }

        user.getIoLock().lock();
        try {
            return call(() -> {
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
        } finally {
            user.getIoLock().unlock();
        }
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
    public List<HeldPermission<UUID>> getUsersWithPermission(String permission) {
        ImmutableList.Builder<HeldPermission<UUID>> held = ImmutableList.builder();
        boolean success = call(() -> {
            MongoCollection<Document> c = database.getCollection("users");

            try (MongoCursor<Document> cursor = c.find().iterator()) {
                while (cursor.hasNext()) {
                    Document d = cursor.next();

                    UUID holder = d.get("_id", UUID.class);
                    Map<String, Boolean> perms = revert((Map<String, Boolean>) d.get("perms"));

                    for (Map.Entry<String, Boolean> e : perms.entrySet()) {
                        Node node = NodeFactory.fromSerializedNode(e.getKey(), e.getValue());
                        if (!node.getPermission().equalsIgnoreCase(permission)) {
                            continue;
                        }

                        held.add(NodeHeldPermission.of(holder, node));
                    }
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
                MongoCollection<Document> c = database.getCollection("groups");

                try (MongoCursor<Document> cursor = c.find(new Document("_id", group.getName())).iterator()) {
                    if (cursor.hasNext()) {
                        // Group exists, let's load.
                        Document d = cursor.next();
                        group.setNodes(revert((Map<String, Boolean>) d.get("perms")).entrySet().stream()
                                .map(e -> NodeFactory.fromSerializedNode(e.getKey(), e.getValue()))
                                .collect(Collectors.toSet())
                        );
                    } else {
                        c.insertOne(fromGroup(group));
                    }
                }
                return true;
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
                MongoCollection<Document> c = database.getCollection("groups");

                try (MongoCursor<Document> cursor = c.find(new Document("_id", group.getName())).iterator()) {
                    if (cursor.hasNext()) {
                        Document d = cursor.next();

                        group.setNodes(revert((Map<String, Boolean>) d.get("perms")).entrySet().stream()
                                .map(e -> NodeFactory.fromSerializedNode(e.getKey(), e.getValue()))
                                .collect(Collectors.toSet())
                        );
                        return true;
                    }
                    return false;
                }
            }, false);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean loadAllGroups() {
        List<String> groups = new ArrayList<>();
        boolean success = call(() -> {
            MongoCollection<Document> c = database.getCollection("groups");

            boolean b = true;
            try (MongoCursor<Document> cursor = c.find().iterator()) {
                while (cursor.hasNext()) {
                    String name = cursor.next().getString("_id");
                    if (!loadGroup(name)) {
                        b = false;
                    }
                    groups.add(name);
                }
            }
            return b;
        }, false);

        if (success) {
            GroupManager gm = plugin.getGroupManager();
            gm.getAll().values().stream()
                    .filter(g -> !groups.contains(g.getName()))
                    .forEach(gm::unload);
        }
        return success;
    }

    @Override
    public boolean saveGroup(Group group) {
        group.getIoLock().lock();
        try {
            return call(() -> {
                MongoCollection<Document> c = database.getCollection("groups");
                return c.replaceOne(new Document("_id", group.getName()), fromGroup(group)).wasAcknowledged();
            }, false);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean deleteGroup(Group group) {
        group.getIoLock().lock();
        boolean success;
        try {
            success = call(() -> {
                MongoCollection<Document> c = database.getCollection("groups");
                return c.deleteOne(new Document("_id", group.getName())).wasAcknowledged();
            }, false);
        } finally {
            group.getIoLock().unlock();
        }

        if (success) plugin.getGroupManager().unload(group);
        return success;
    }

    @Override
    public List<HeldPermission<String>> getGroupsWithPermission(String permission) {
        ImmutableList.Builder<HeldPermission<String>> held = ImmutableList.builder();
        boolean success = call(() -> {
            MongoCollection<Document> c = database.getCollection("groups");

            try (MongoCursor<Document> cursor = c.find().iterator()) {
                while (cursor.hasNext()) {
                    Document d = cursor.next();

                    String holder = d.getString("_id");
                    Map<String, Boolean> perms = revert((Map<String, Boolean>) d.get("perms"));

                    for (Map.Entry<String, Boolean> e : perms.entrySet()) {
                        Node node = NodeFactory.fromSerializedNode(e.getKey(), e.getValue());
                        if (!node.getPermission().equalsIgnoreCase(permission)) {
                            continue;
                        }

                        held.add(NodeHeldPermission.of(holder, node));
                    }
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
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean loadAllTracks() {
        List<String> tracks = new ArrayList<>();
        boolean success = call(() -> {
            MongoCollection<Document> c = database.getCollection("tracks");

            boolean b = true;
            try (MongoCursor<Document> cursor = c.find().iterator()) {
                while (cursor.hasNext()) {
                    String name = cursor.next().getString("_id");
                    if (!loadTrack(name)) {
                        b = false;
                    }
                    tracks.add(name);
                }
            }
            return b;
        }, false);

        if (success) {
            TrackManager tm = plugin.getTrackManager();
            tm.getAll().values().stream()
                    .filter(t -> !tracks.contains(t.getName()))
                    .forEach(tm::unload);
        }
        return success;
    }

    @Override
    public boolean saveTrack(Track track) {
        track.getIoLock().lock();
        try {
            return call(() -> {
                MongoCollection<Document> c = database.getCollection("tracks");
                return c.replaceOne(new Document("_id", track.getName()), fromTrack(track)).wasAcknowledged();
            }, false);
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean deleteTrack(Track track) {
        track.getIoLock().lock();
        boolean success;
        try {
            success = call(() -> {
                MongoCollection<Document> c = database.getCollection("tracks");
                return c.deleteOne(new Document("_id", track.getName())).wasAcknowledged();
            }, false);
        } finally {
            track.getIoLock().unlock();
        }

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

    public static Map<String, Boolean> exportToLegacy(Iterable<Node> nodes) {
        Map<String, Boolean> m = new HashMap<>();
        for (Node node : nodes) {
            m.put(node.toSerializedNode(), node.getValue());
        }
        return m;
    }
}
