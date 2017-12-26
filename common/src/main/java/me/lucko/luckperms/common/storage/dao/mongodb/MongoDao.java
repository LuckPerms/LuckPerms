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

package me.lucko.luckperms.common.storage.dao.mongodb;

import lombok.Getter;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.actionlog.ExtendedLogEntry;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.managers.GenericUserManager;
import me.lucko.luckperms.common.managers.GroupManager;
import me.lucko.luckperms.common.managers.TrackManager;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.LegacyNodeFactory;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.node.NodeHeldPermission;
import me.lucko.luckperms.common.node.NodeModel;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.references.UserIdentifier;
import me.lucko.luckperms.common.storage.StorageCredentials;
import me.lucko.luckperms.common.storage.dao.AbstractDao;

import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MongoDao extends AbstractDao {

    private final StorageCredentials configuration;
    private MongoClient mongoClient;
    private MongoDatabase database;

    @Getter
    private final String prefix;

    public MongoDao(LuckPermsPlugin plugin, StorageCredentials configuration, String prefix) {
        super(plugin, "MongoDB");
        this.configuration = configuration;
        this.prefix = prefix;
    }

    @Override
    public void init() {
        MongoCredential credential = null;
        if (!Strings.isNullOrEmpty(configuration.getUsername())) {
            credential = MongoCredential.createCredential(
                    configuration.getUsername(),
                    configuration.getDatabase(),
                    Strings.isNullOrEmpty(configuration.getPassword()) ? null : configuration.getPassword().toCharArray()
            );
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
    }

    @Override
    public void shutdown() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Override
    public Map<String, String> getMeta() {
        Map<String, String> ret = new LinkedHashMap<>();
        boolean success = true;

        long start = System.currentTimeMillis();
        try {
            database.runCommand(new Document("ping", 1));
        } catch (Exception e) {
            success = false;
        }
        long duration = System.currentTimeMillis() - start;

        if (success) {
            ret.put("Ping", "&a" + duration + "ms");
            ret.put("Connected", "true");
        } else {
            ret.put("Connected", "false");
        }

        return ret;
    }

    @Override
    public void logAction(LogEntry entry) {
        MongoCollection<Document> c = database.getCollection(prefix + "action");
        Document doc = new Document()
                .append("timestamp", entry.getTimestamp())
                .append("actor", entry.getActor())
                .append("actorName", entry.getActorName())
                .append("type", Character.toString(entry.getType().getCode()))
                .append("actedName", entry.getActedName())
                .append("action", entry.getAction());

        if (entry.getActed().isPresent()) {
            doc.append("acted", entry.getActed().get());
        }

        c.insertOne(doc);
    }

    @Override
    public Log getLog() {
        Log.Builder log = Log.builder();
        MongoCollection<Document> c = database.getCollection(prefix + "action");
        try (MongoCursor<Document> cursor = c.find().iterator()) {
            while (cursor.hasNext()) {
                Document d = cursor.next();

                UUID actedUuid = null;
                if (d.containsKey("acted")) {
                    actedUuid = d.get("acted", UUID.class);
                }

                ExtendedLogEntry e = ExtendedLogEntry.build()
                        .timestamp(d.getLong("timestamp"))
                        .actor(d.get("actor", UUID.class))
                        .actorName(d.getString("actorName"))
                        .type(LogEntry.Type.valueOf(d.getString("type").toCharArray()[0]))
                        .acted(actedUuid)
                        .actedName(d.getString("actedName"))
                        .action(d.getString("action"))
                        .build();

                log.add(e);
            }
        }
        return log.build();
    }

    @Override
    public void applyBulkUpdate(BulkUpdate bulkUpdate) {
        if (bulkUpdate.getDataType().isIncludingUsers()) {
            MongoCollection<Document> c = database.getCollection(prefix + "users");
            try (MongoCursor<Document> cursor = c.find().iterator()) {
                while (cursor.hasNext()) {
                    Document d = cursor.next();

                    UUID uuid = d.get("_id", UUID.class);
                    Set<NodeModel> nodes = new HashSet<>(nodesFromDoc(d));
                    Set<NodeModel> results = nodes.stream()
                            .map(bulkUpdate::apply)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    if (!nodes.equals(results)) {
                        List<Document> newNodes = results.stream()
                                .map(MongoDao::nodeToDoc)
                                .collect(Collectors.toList());

                        d.append("permissions", newNodes).remove("perms");
                        c.replaceOne(new Document("_id", uuid), d);
                    }
                }
            }
        }

        if (bulkUpdate.getDataType().isIncludingGroups()) {
            MongoCollection<Document> c = database.getCollection(prefix + "groups");
            try (MongoCursor<Document> cursor = c.find().iterator()) {
                while (cursor.hasNext()) {
                    Document d = cursor.next();

                    String holder = d.getString("_id");
                    Set<NodeModel> nodes = new HashSet<>(nodesFromDoc(d));
                    Set<NodeModel> results = nodes.stream()
                            .map(bulkUpdate::apply)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    if (!nodes.equals(results)) {
                        List<Document> newNodes = results.stream()
                                .map(MongoDao::nodeToDoc)
                                .collect(Collectors.toList());

                        d.append("permissions", newNodes).remove("perms");
                        c.replaceOne(new Document("_id", holder), d);
                    }
                }
            }
        }
    }

    @Override
    public User loadUser(UUID uuid, String username) {
        User user = plugin.getUserManager().getOrMake(UserIdentifier.of(uuid, username));
        user.getIoLock().lock();
        try {
            MongoCollection<Document> c = database.getCollection(prefix + "users");
            try (MongoCursor<Document> cursor = c.find(new Document("_id", user.getUuid())).iterator()) {
                if (cursor.hasNext()) {
                    // User exists, let's load.
                    Document d = cursor.next();

                    String name = d.getString("name");
                    user.getPrimaryGroup().setStoredValue(d.getString("primaryGroup"));

                    Set<Node> nodes = nodesFromDoc(d).stream().map(NodeModel::toNode).collect(Collectors.toSet());
                    user.setEnduringNodes(nodes);
                    user.setName(name, true);

                    boolean save = plugin.getUserManager().giveDefaultIfNeeded(user, false);
                    if (user.getName().isPresent() && (name == null || !user.getName().get().equalsIgnoreCase(name))) {
                        save = true;
                    }

                    if (save) {
                        c.replaceOne(new Document("_id", user.getUuid()), userToDoc(user));
                    }
                } else {
                    if (GenericUserManager.shouldSave(user)) {
                        user.clearNodes();
                        user.getPrimaryGroup().setStoredValue(null);
                        plugin.getUserManager().giveDefaultIfNeeded(user, false);
                    }
                }
            }
        } finally {
            user.getIoLock().unlock();
        }
        user.getRefreshBuffer().requestDirectly();
        return user;
    }

    @Override
    public void saveUser(User user) {
        user.getIoLock().lock();
        try {
            MongoCollection<Document> c = database.getCollection(prefix + "users");
            if (!GenericUserManager.shouldSave(user)) {
                c.deleteOne(new Document("_id", user.getUuid()));
            } else {
                c.replaceOne(new Document("_id", user.getUuid()), userToDoc(user), new UpdateOptions().upsert(true));
            }
        } finally {
            user.getIoLock().unlock();
        }
    }

    @Override
    public Set<UUID> getUniqueUsers() {
        Set<UUID> uuids = new HashSet<>();
        MongoCollection<Document> c = database.getCollection(prefix + "users");
        try (MongoCursor<Document> cursor = c.find().iterator()) {
            while (cursor.hasNext()) {
                Document d = cursor.next();
                uuids.add(d.get("_id", UUID.class));
            }
        }
        return uuids;
    }

    @Override
    public List<HeldPermission<UUID>> getUsersWithPermission(String permission) {
        ImmutableList.Builder<HeldPermission<UUID>> held = ImmutableList.builder();
        MongoCollection<Document> c = database.getCollection(prefix + "users");
        try (MongoCursor<Document> cursor = c.find().iterator()) {
            while (cursor.hasNext()) {
                Document d = cursor.next();
                UUID holder = d.get("_id", UUID.class);

                Set<NodeModel> nodes = new HashSet<>(nodesFromDoc(d));
                for (NodeModel e : nodes) {
                    if (!e.getPermission().equalsIgnoreCase(permission)) {
                        continue;
                    }
                    held.add(NodeHeldPermission.of(holder, e));
                }
            }
        }
        return held.build();
    }

    @Override
    public Group createAndLoadGroup(String name) {
        Group group = plugin.getGroupManager().getOrMake(name);
        group.getIoLock().lock();
        try {
            MongoCollection<Document> c = database.getCollection(prefix + "groups");
            try (MongoCursor<Document> cursor = c.find(new Document("_id", group.getName())).iterator()) {
                if (cursor.hasNext()) {
                    Document d = cursor.next();
                    Set<Node> nodes = nodesFromDoc(d).stream().map(NodeModel::toNode).collect(Collectors.toSet());
                    group.setEnduringNodes(nodes);
                } else {
                    c.insertOne(groupToDoc(group));
                }
            }
        } finally {
            group.getIoLock().unlock();
        }
        group.getRefreshBuffer().requestDirectly();
        return group;
    }

    @Override
    public Optional<Group> loadGroup(String name) {
        Group group = plugin.getGroupManager().getIfLoaded(name);
        if (group != null) {
            group.getIoLock().lock();
        }
        try {
            MongoCollection<Document> c = database.getCollection(prefix + "groups");
            try (MongoCursor<Document> cursor = c.find(new Document("_id", name)).iterator()) {
                if (!cursor.hasNext()) {
                    return Optional.empty();
                }

                if (group == null) {
                    group = plugin.getGroupManager().getOrMake(name);
                    group.getIoLock().lock();
                }

                Document d = cursor.next();
                Set<Node> nodes = nodesFromDoc(d).stream().map(NodeModel::toNode).collect(Collectors.toSet());
                group.setEnduringNodes(nodes);
            }
        } finally {
            if (group != null) {
                group.getIoLock().unlock();
            }
        }
        group.getRefreshBuffer().requestDirectly();
        return Optional.of(group);
    }

    @Override
    public void loadAllGroups() {
        List<String> groups = new ArrayList<>();
        MongoCollection<Document> c = database.getCollection(prefix + "groups");
        try (MongoCursor<Document> cursor = c.find().iterator()) {
            while (cursor.hasNext()) {
                String name = cursor.next().getString("_id");
                groups.add(name);
            }
        }

        boolean success = true;
        for (String g : groups) {
            try {
                loadGroup(g);
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
            }
        }

        if (!success) {
            throw new RuntimeException("Exception occurred whilst loading a group");
        }

        GroupManager gm = plugin.getGroupManager();
        gm.getAll().values().stream()
                .filter(g -> !groups.contains(g.getName()))
                .forEach(gm::unload);
    }

    @Override
    public void saveGroup(Group group) {
        group.getIoLock().lock();
        try {
            MongoCollection<Document> c = database.getCollection(prefix + "groups");
            c.replaceOne(new Document("_id", group.getName()), groupToDoc(group), new UpdateOptions().upsert(true));
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public void deleteGroup(Group group) {
        group.getIoLock().lock();
        try {
            MongoCollection<Document> c = database.getCollection(prefix + "groups");
            c.deleteOne(new Document("_id", group.getName()));
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public List<HeldPermission<String>> getGroupsWithPermission(String permission) {
        ImmutableList.Builder<HeldPermission<String>> held = ImmutableList.builder();
        MongoCollection<Document> c = database.getCollection(prefix + "groups");
        try (MongoCursor<Document> cursor = c.find().iterator()) {
            while (cursor.hasNext()) {
                Document d = cursor.next();

                String holder = d.getString("_id");
                Set<NodeModel> nodes = new HashSet<>(nodesFromDoc(d));
                for (NodeModel e : nodes) {
                    if (!e.getPermission().equalsIgnoreCase(permission)) {
                        continue;
                    }
                    held.add(NodeHeldPermission.of(holder, e));
                }
            }
        }
        return held.build();
    }

    @Override
    public Track createAndLoadTrack(String name) {
        Track track = plugin.getTrackManager().getOrMake(name);
        track.getIoLock().lock();
        try {
            MongoCollection<Document> c = database.getCollection(prefix + "tracks");
            try (MongoCursor<Document> cursor = c.find(new Document("_id", track.getName())).iterator()) {
                if (!cursor.hasNext()) {
                    c.insertOne(trackToDoc(track));
                } else {
                    Document d = cursor.next();
                    //noinspection unchecked
                    track.setGroups((List<String>) d.get("groups"));
                }
            }
        } finally {
            track.getIoLock().unlock();
        }
        return track;
    }

    @Override
    public Optional<Track> loadTrack(String name) {
        Track track = plugin.getTrackManager().getIfLoaded(name);
        if (track != null) {
            track.getIoLock().lock();
        }

        try {
            MongoCollection<Document> c = database.getCollection(prefix + "tracks");
            try (MongoCursor<Document> cursor = c.find(new Document("_id", name)).iterator()) {
                if (!cursor.hasNext()) {
                    return Optional.empty();
                }

                if (track == null) {
                    track = plugin.getTrackManager().getOrMake(name);
                    track.getIoLock().lock();
                }

                Document d = cursor.next();
                //noinspection unchecked
                track.setGroups((List<String>) d.get("groups"));
            }
        } finally {
            if (track != null) {
                track.getIoLock().unlock();
            }
        }
        return Optional.of(track);
    }

    @Override
    public void loadAllTracks() {
        List<String> tracks = new ArrayList<>();
        MongoCollection<Document> c = database.getCollection(prefix + "tracks");
        try (MongoCursor<Document> cursor = c.find().iterator()) {
            while (cursor.hasNext()) {
                String name = cursor.next().getString("_id");
                tracks.add(name);
            }
        }

        boolean success = true;
        for (String t : tracks) {
            try {
                loadTrack(t);
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
            }
        }

        if (!success) {
            throw new RuntimeException("Exception occurred whilst loading a track");
        }

        TrackManager tm = plugin.getTrackManager();
        tm.getAll().values().stream()
                .filter(t -> !tracks.contains(t.getName()))
                .forEach(tm::unload);
    }

    @Override
    public void saveTrack(Track track) {
        track.getIoLock().lock();
        try {
            MongoCollection<Document> c = database.getCollection(prefix + "tracks");
            c.replaceOne(new Document("_id", track.getName()), trackToDoc(track));
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public void deleteTrack(Track track) {
        track.getIoLock().lock();
        try {
            MongoCollection<Document> c = database.getCollection(prefix + "tracks");
            c.deleteOne(new Document("_id", track.getName()));
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public void saveUUIDData(UUID uuid, String username) {
        MongoCollection<Document> c = database.getCollection(prefix + "uuid");
        c.replaceOne(new Document("_id", uuid), new Document("_id", uuid).append("name", username.toLowerCase()), new UpdateOptions().upsert(true));
    }

    @Override
    public UUID getUUID(String username) {
        MongoCollection<Document> c = database.getCollection(prefix + "uuid");
        try (MongoCursor<Document> cursor = c.find(new Document("name", username.toLowerCase())).iterator()) {
            if (cursor.hasNext()) {
                return cursor.next().get("_id", UUID.class);
            }
        }
        return null;
    }

    @Override
    public String getName(UUID uuid) {
        MongoCollection<Document> c = database.getCollection(prefix + "uuid");
        try (MongoCursor<Document> cursor = c.find(new Document("_id", uuid)).iterator()) {
            if (cursor.hasNext()) {
                return cursor.next().get("name", String.class);
            }
        }
        return null;
    }

    private static Document userToDoc(User user) {
        List<Document> nodes = user.getEnduringNodes().values().stream()
                .map(NodeModel::fromNode)
                .map(MongoDao::nodeToDoc)
                .collect(Collectors.toList());

        return new Document("_id", user.getUuid())
                .append("name", user.getName().orElse("null"))
                .append("primaryGroup", user.getPrimaryGroup().getStoredValue().orElse(NodeFactory.DEFAULT_GROUP_NAME))
                .append("permissions", nodes);
    }

    private static List<NodeModel> nodesFromDoc(Document document) {
        List<NodeModel> nodes = new ArrayList<>();

        // legacy
        if (document.containsKey("perms") && document.get("perms") instanceof Map) {
            //noinspection unchecked
            Map<String, Boolean> permsMap = (Map<String, Boolean>) document.get("perms");
            for (Map.Entry<String, Boolean> e : permsMap.entrySet()) {
                // legacy permission key deserialisation
                String permission = e.getKey().replace("[**DOT**]", ".").replace("[**DOLLAR**]", "$");
                nodes.add(NodeModel.fromNode(LegacyNodeFactory.fromLegacyString(permission, e.getValue())));
            }
        }

        // new format
        if (document.containsKey("permissions") && document.get("permissions") instanceof List) {
            //noinspection unchecked
            List<Document> permsList = (List<Document>) document.get("permissions");
            for (Document d : permsList) {
                nodes.add(nodeFromDoc(d));
            }
        }

        return nodes;
    }

    private static Document groupToDoc(Group group) {
        List<Document> nodes = group.getEnduringNodes().values().stream()
                .map(NodeModel::fromNode)
                .map(MongoDao::nodeToDoc)
                .collect(Collectors.toList());

        return new Document("_id", group.getName()).append("permissions", nodes);
    }

    private static Document trackToDoc(Track track) {
        return new Document("_id", track.getName()).append("groups", track.getGroups());
    }

    private static Document nodeToDoc(NodeModel node) {
        Document document = new Document();

        document.append("permission", node.getPermission());
        document.append("value", node.getValue());

        if (!node.getServer().equals("global")) {
            document.append("server", node.getServer());
        }

        if (!node.getWorld().equals("global")) {
            document.append("world", node.getWorld());
        }

        if (node.getExpiry() != 0L) {
            document.append("expiry", node.getExpiry());
        }

        if (!node.getContexts().isEmpty()) {
            document.append("context", contextSetToDocs(node.getContexts()));
        }

        return document;
    }

    private static NodeModel nodeFromDoc(Document document) {
        String permission = document.getString("permission");
        boolean value = true;
        String server = "global";
        String world = "global";
        long expiry = 0L;
        ImmutableContextSet context = ImmutableContextSet.empty();

        if (document.containsKey("value")) {
            value = document.getBoolean("value");
        }
        if (document.containsKey("server")) {
            server = document.getString("server");
        }
        if (document.containsKey("world")) {
            world = document.getString("world");
        }
        if (document.containsKey("expiry")) {
            expiry = document.getLong("expiry");
        }

        if (document.containsKey("context") && document.get("context") instanceof List) {
            //noinspection unchecked
            List<Document> contexts = (List<Document>) document.get("context");
            context = docsToContextSet(contexts).makeImmutable();
        }

        return NodeModel.of(permission, value, server, world, expiry, context);
    }

    private static List<Document> contextSetToDocs(ContextSet contextSet) {
        List<Document> contexts = new ArrayList<>();
        for (Map.Entry<String, String> e : contextSet.toSet()) {
            contexts.add(new Document().append("key", e.getKey()).append("value", e.getValue()));
        }
        return contexts;
    }

    private static MutableContextSet docsToContextSet(List<Document> documents) {
        MutableContextSet map = MutableContextSet.create();
        for (Document doc : documents) {
            map.add(doc.getString("key"), doc.getString("value"));
        }
        return map;
    }

}
