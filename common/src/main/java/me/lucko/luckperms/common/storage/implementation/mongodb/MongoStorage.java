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

package me.lucko.luckperms.common.storage.implementation.mongodb;

import com.google.common.base.Strings;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;

import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.context.contextset.MutableContextSetImpl;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.node.matcher.ConstraintNodeMatcher;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.StorageImplementation;
import me.lucko.luckperms.common.storage.misc.NodeEntry;
import me.lucko.luckperms.common.storage.misc.PlayerSaveResultImpl;
import me.lucko.luckperms.common.storage.misc.StorageCredentials;
import me.lucko.luckperms.common.util.Iterators;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeBuilder;

import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MongoStorage implements StorageImplementation {
    private final LuckPermsPlugin plugin;

    private final StorageCredentials configuration;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private final String prefix;
    private final String connectionUri;

    public MongoStorage(LuckPermsPlugin plugin, StorageCredentials configuration, String prefix, String connectionUri) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.prefix = prefix;
        this.connectionUri = connectionUri;
    }

    @Override
    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public String getImplementationName() {
        return "MongoDB";
    }

    @Override
    public void init() {
        if (!Strings.isNullOrEmpty(this.connectionUri)) {
            this.mongoClient = new MongoClient(new MongoClientURI(this.connectionUri));
        } else {
            MongoCredential credential = null;
            if (!Strings.isNullOrEmpty(this.configuration.getUsername())) {
                credential = MongoCredential.createCredential(
                        this.configuration.getUsername(),
                        this.configuration.getDatabase(),
                        Strings.isNullOrEmpty(this.configuration.getPassword()) ? null : this.configuration.getPassword().toCharArray()
                );
            }

            String[] addressSplit = this.configuration.getAddress().split(":");
            String host = addressSplit[0];
            int port = addressSplit.length > 1 ? Integer.parseInt(addressSplit[1]) : 27017;
            ServerAddress address = new ServerAddress(host, port);

            if (credential == null) {
                this.mongoClient = new MongoClient(address);
            } else {
                this.mongoClient = new MongoClient(address, credential, MongoClientOptions.builder().build());
            }
        }
        
        this.database = this.mongoClient.getDatabase(this.configuration.getDatabase());
    }

    @Override
    public void shutdown() {
        if (this.mongoClient != null) {
            this.mongoClient.close();
        }
    }

    @Override
    public Map<Component, Component> getMeta() {
        Map<Component, Component> meta = new LinkedHashMap<>();
        boolean success = true;

        long start = System.currentTimeMillis();
        try {
            this.database.runCommand(new Document("ping", 1));
        } catch (Exception e) {
            success = false;
        }
        long duration = System.currentTimeMillis() - start;

        if (success) {
            meta.put(
                    Component.translatable("luckperms.command.info.storage.meta.ping-key"),
                    Component.text(duration + "ms", NamedTextColor.GREEN)
            );
        }
        meta.put(
                Component.translatable("luckperms.command.info.storage.meta.connected-key"),
                Message.formatBoolean(success)
        );

        return meta;
    }

    @Override
    public void logAction(Action entry) {
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "action");

        Document doc = new Document()
                .append("timestamp", entry.getTimestamp().getEpochSecond())
                .append("source", new Document()
                        .append("uniqueId", entry.getSource().getUniqueId())
                        .append("name", entry.getSource().getName())
                );

        Document target = new Document()
                .append("type", entry.getTarget().getType().name())
                .append("name", entry.getTarget().getName());

        if (entry.getTarget().getUniqueId().isPresent()) {
            target.append("uniqueId", entry.getTarget().getUniqueId().get());
        }

        doc.append("target", target);
        doc.append("description", entry.getDescription());

        c.insertOne(doc);
    }

    @Override
    public Log getLog() {
        Log.Builder log = Log.builder();
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "action");
        try (MongoCursor<Document> cursor = c.find().iterator()) {
            while (cursor.hasNext()) {
                Document d = cursor.next();

                if (d.containsKey("source")) {
                    // new format
                    Document source = d.get("source", Document.class);
                    Document target = d.get("target", Document.class);

                    UUID targetUniqueId = null;
                    if (target.containsKey("uniqueId")) {
                        targetUniqueId = target.get("uniqueId", UUID.class);
                    }

                    LoggedAction e = LoggedAction.build()
                            .timestamp(Instant.ofEpochSecond(d.getLong("timestamp")))
                            .source(source.get("uniqueId", UUID.class))
                            .sourceName(source.getString("name"))
                            .targetType(LoggedAction.parseType(target.getString("type")))
                            .target(targetUniqueId)
                            .targetName(target.getString("name"))
                            .description(d.getString("description"))
                            .build();

                    log.add(e);
                } else {
                    // old format
                    UUID actedUuid = null;
                    if (d.containsKey("acted")) {
                        actedUuid = d.get("acted", UUID.class);
                    }

                    LoggedAction e = LoggedAction.build()
                            .timestamp(Instant.ofEpochSecond(d.getLong("timestamp")))
                            .source(d.get("actor", UUID.class))
                            .sourceName(d.getString("actorName"))
                            .targetType(LoggedAction.parseTypeCharacter(d.getString("type").charAt(0)))
                            .target(actedUuid)
                            .targetName(d.getString("actedName"))
                            .description(d.getString("action"))
                            .build();

                    log.add(e);
                }
            }
        }
        return log.build();
    }

    @Override
    public void applyBulkUpdate(BulkUpdate bulkUpdate) {
        if (bulkUpdate.getDataType().isIncludingUsers()) {
            MongoCollection<Document> c = this.database.getCollection(this.prefix + "users");
            try (MongoCursor<Document> cursor = c.find().iterator()) {
                while (cursor.hasNext()) {
                    Document d = cursor.next();
                    UUID uuid = getDocumentId(d);
                    Document results = processBulkUpdate(d, bulkUpdate, HolderType.USER);
                    if (results != null) {
                        c.replaceOne(new Document("_id", uuid), results);
                    }
                }
            }
        }

        if (bulkUpdate.getDataType().isIncludingGroups()) {
            MongoCollection<Document> c = this.database.getCollection(this.prefix + "groups");
            try (MongoCursor<Document> cursor = c.find().iterator()) {
                while (cursor.hasNext()) {
                    Document d = cursor.next();
                    String holder = d.getString("_id");
                    Document results = processBulkUpdate(d, bulkUpdate, HolderType.GROUP);
                    if (results != null) {
                        c.replaceOne(new Document("_id", holder), results);
                    }
                }
            }
        }
    }

    private Document processBulkUpdate(Document document, BulkUpdate bulkUpdate, HolderType holderType) {
        Set<Node> nodes = new HashSet<>(nodesFromDoc(document));
        Set<Node> results = bulkUpdate.apply(nodes, holderType);

        if (results == null) {
            return null;
        }

        List<Document> newNodes = results.stream()
                .map(MongoStorage::nodeToDoc)
                .collect(Collectors.toList());

        document.append("permissions", newNodes).remove("perms");
        return document;
    }

    @Override
    public User loadUser(UUID uniqueId, String username) {
        User user = this.plugin.getUserManager().getOrMake(uniqueId, username);
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "users");
        try (MongoCursor<Document> cursor = c.find(new Document("_id", user.getUniqueId())).iterator()) {
            if (cursor.hasNext()) {
                // User exists, let's load.
                Document d = cursor.next();
                String name = d.getString("name");

                user.getPrimaryGroup().setStoredValue(d.getString("primaryGroup"));
                user.setUsername(name, true);

                user.loadNodesFromStorage(nodesFromDoc(d));
                this.plugin.getUserManager().giveDefaultIfNeeded(user);


                boolean updatedUsername = user.getUsername().isPresent() && (name == null || !user.getUsername().get().equalsIgnoreCase(name));
                if (updatedUsername | user.auditTemporaryNodes()) {
                    c.replaceOne(new Document("_id", user.getUniqueId()), userToDoc(user));
                }
            } else {
                if (this.plugin.getUserManager().isNonDefaultUser(user)) {
                    user.loadNodesFromStorage(Collections.emptyList());
                    user.getPrimaryGroup().setStoredValue(null);
                    this.plugin.getUserManager().giveDefaultIfNeeded(user);
                }
            }
        }
        return user;
    }

    @Override
    public void saveUser(User user) {
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "users");
        user.normalData().discardChanges();
        if (!this.plugin.getUserManager().isNonDefaultUser(user)) {
            c.deleteOne(new Document("_id", user.getUniqueId()));
        } else {
            c.replaceOne(new Document("_id", user.getUniqueId()), userToDoc(user), new ReplaceOptions().upsert(true));
        }
    }

    @Override
    public Set<UUID> getUniqueUsers() {
        Set<UUID> uuids = new HashSet<>();
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "users");
        try (MongoCursor<Document> cursor = c.find().iterator()) {
            while (cursor.hasNext()) {
                try {
                    uuids.add(getDocumentId(cursor.next()));
                } catch (IllegalArgumentException e) {
                    // ignore
                }
            }
        }
        return uuids;
    }

    @Override
    public <N extends Node> List<NodeEntry<UUID, N>> searchUserNodes(ConstraintNodeMatcher<N> constraint) throws Exception {
        List<NodeEntry<UUID, N>> held = new ArrayList<>();
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "users");
        try (MongoCursor<Document> cursor = c.find().iterator()) {
            while (cursor.hasNext()) {
                Document d = cursor.next();
                UUID holder = getDocumentId(d);

                Set<Node> nodes = new HashSet<>(nodesFromDoc(d));
                for (Node e : nodes) {
                    N match = constraint.match(e);
                    if (match != null) {
                        held.add(NodeEntry.of(holder, match));
                    }
                }
            }
        }
        return held;
    }

    @Override
    public Group createAndLoadGroup(String name) {
        Group group = this.plugin.getGroupManager().getOrMake(name);
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "groups");
        try (MongoCursor<Document> cursor = c.find(new Document("_id", group.getName())).iterator()) {
            if (cursor.hasNext()) {
                Document d = cursor.next();
                group.loadNodesFromStorage(nodesFromDoc(d));
            } else {
                c.insertOne(groupToDoc(group));
            }
        }
        return group;
    }

    @Override
    public Optional<Group> loadGroup(String name) {
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "groups");
        try (MongoCursor<Document> cursor = c.find(new Document("_id", name)).iterator()) {
            if (!cursor.hasNext()) {
                return Optional.empty();
            }

            Group group = this.plugin.getGroupManager().getOrMake(name);
            Document d = cursor.next();
            group.loadNodesFromStorage(nodesFromDoc(d));
            return Optional.of(group);
        }
    }

    @Override
    public void loadAllGroups() {
        List<String> groups = new ArrayList<>();
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "groups");
        try (MongoCursor<Document> cursor = c.find().iterator()) {
            while (cursor.hasNext()) {
                String name = cursor.next().getString("_id");
                groups.add(name);
            }
        }

        if (!Iterators.tryIterate(groups, this::loadGroup)) {
            throw new RuntimeException("Exception occurred whilst loading a group");
        }

        this.plugin.getGroupManager().retainAll(groups);
    }

    @Override
    public void saveGroup(Group group) {
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "groups");
        group.normalData().discardChanges();
        c.replaceOne(new Document("_id", group.getName()), groupToDoc(group), new ReplaceOptions().upsert(true));
    }

    @Override
    public void deleteGroup(Group group) {
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "groups");
        c.deleteOne(new Document("_id", group.getName()));
    }

    @Override
    public <N extends Node> List<NodeEntry<String, N>> searchGroupNodes(ConstraintNodeMatcher<N> constraint) throws Exception {
        List<NodeEntry<String, N>> held = new ArrayList<>();
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "groups");
        try (MongoCursor<Document> cursor = c.find().iterator()) {
            while (cursor.hasNext()) {
                Document d = cursor.next();
                String holder = d.getString("_id");

                Set<Node> nodes = new HashSet<>(nodesFromDoc(d));
                for (Node e : nodes) {
                    N match = constraint.match(e);
                    if (match != null) {
                        held.add(NodeEntry.of(holder, match));
                    }
                }
            }
        }
        return held;
    }

    @Override
    public Track createAndLoadTrack(String name) {
        Track track = this.plugin.getTrackManager().getOrMake(name);
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "tracks");
        try (MongoCursor<Document> cursor = c.find(new Document("_id", track.getName())).iterator()) {
            if (!cursor.hasNext()) {
                c.insertOne(trackToDoc(track));
            } else {
                Document d = cursor.next();
                //noinspection unchecked
                track.setGroups((List<String>) d.get("groups"));
            }
        }
        return track;
    }

    @Override
    public Optional<Track> loadTrack(String name) {
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "tracks");
        try (MongoCursor<Document> cursor = c.find(new Document("_id", name)).iterator()) {
            if (!cursor.hasNext()) {
                return Optional.empty();
            }

            Track track = this.plugin.getTrackManager().getOrMake(name);
            Document d = cursor.next();
            //noinspection unchecked
            track.setGroups((List<String>) d.get("groups"));
            return Optional.of(track);
        }
    }

    @Override
    public void loadAllTracks() {
        List<String> tracks = new ArrayList<>();
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "tracks");
        try (MongoCursor<Document> cursor = c.find().iterator()) {
            while (cursor.hasNext()) {
                String name = cursor.next().getString("_id");
                tracks.add(name);
            }
        }

        if (!Iterators.tryIterate(tracks, this::loadTrack)) {
            throw new RuntimeException("Exception occurred whilst loading a track");
        }

        this.plugin.getTrackManager().retainAll(tracks);
    }

    @Override
    public void saveTrack(Track track) {
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "tracks");
        c.replaceOne(new Document("_id", track.getName()), trackToDoc(track));
    }

    @Override
    public void deleteTrack(Track track) {
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "tracks");
        c.deleteOne(new Document("_id", track.getName()));
    }

    @Override
    public PlayerSaveResult savePlayerData(UUID uniqueId, String username) {
        username = username.toLowerCase();
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "uuid");

        // find any existing mapping
        String oldUsername = getPlayerName(uniqueId);

        // do the insert
        if (!username.equalsIgnoreCase(oldUsername)) {
            c.replaceOne(new Document("_id", uniqueId), new Document("_id", uniqueId).append("name", username), new ReplaceOptions().upsert(true));
        }

        PlayerSaveResultImpl result = PlayerSaveResultImpl.determineBaseResult(username, oldUsername);

        Set<UUID> conflicting = new HashSet<>();
        try (MongoCursor<Document> cursor = c.find(new Document("name", username)).iterator()) {
            while (cursor.hasNext()) {
                conflicting.add(getDocumentId(cursor.next()));
            }
        }
        conflicting.remove(uniqueId);

        if (!conflicting.isEmpty()) {
            // remove the mappings for conflicting uuids
            c.deleteMany(Filters.and(conflicting.stream().map(u -> Filters.eq("_id", u)).collect(Collectors.toList())));
            result = result.withOtherUuidsPresent(conflicting);
        }

        return result;
    }

    @Override
    public void deletePlayerData(UUID uniqueId) {
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "uuid");
        c.deleteMany(Filters.eq("_id", uniqueId));
    }

    @Override
    public UUID getPlayerUniqueId(String username) {
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "uuid");
        Document doc = c.find(new Document("name", username.toLowerCase())).first();
        if (doc != null) {
            return getDocumentId(doc);
        }
        return null;
    }

    @Override
    public String getPlayerName(UUID uniqueId) {
        MongoCollection<Document> c = this.database.getCollection(this.prefix + "uuid");
        Document doc = c.find(new Document("_id", uniqueId)).first();
        if (doc != null) {
            return doc.get("name", String.class);
        }
        return null;
    }

    private static UUID getDocumentId(Document document) {
        Object id = document.get("_id");
        if (id instanceof UUID) {
            return (UUID) id;
        } else if (id instanceof String) {
            return UUID.fromString((String) id);
        } else {
            throw new IllegalArgumentException("Unknown id type: " + id.getClass().getName());
        }
    }

    private static Document userToDoc(User user) {
        List<Document> nodes = user.normalData().asList().stream()
                .map(MongoStorage::nodeToDoc)
                .collect(Collectors.toList());

        return new Document("_id", user.getUniqueId())
                .append("name", user.getUsername().orElse("null"))
                .append("primaryGroup", user.getPrimaryGroup().getStoredValue().orElse(GroupManager.DEFAULT_GROUP_NAME))
                .append("permissions", nodes);
    }

    private static List<Node> nodesFromDoc(Document document) {
        List<Node> nodes = new ArrayList<>();
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
        List<Document> nodes = group.normalData().asList().stream()
                .map(MongoStorage::nodeToDoc)
                .collect(Collectors.toList());

        return new Document("_id", group.getName()).append("permissions", nodes);
    }

    private static Document trackToDoc(Track track) {
        return new Document("_id", track.getName()).append("groups", track.getGroups());
    }

    private static Document nodeToDoc(Node node) {
        Document document = new Document();

        document.append("key", node.getKey());
        document.append("value", node.getValue());

        Instant expiry = node.getExpiry();
        if (expiry != null) {
            document.append("expiry", expiry.getEpochSecond());
        }

        if (!node.getContexts().isEmpty()) {
            document.append("context", contextSetToDocs(node.getContexts()));
        }

        return document;
    }

    private static Node nodeFromDoc(Document document) {
        String key = document.containsKey("permission") ? document.getString("permission") : document.getString("key");

        NodeBuilder<?, ?> builder = NodeBuilders.determineMostApplicable(key)
                .value(document.getBoolean("value", true));

        if (document.containsKey("server")) {
            builder.withContext(DefaultContextKeys.SERVER_KEY, document.getString("server"));
        }

        if (document.containsKey("world")) {
            builder.withContext(DefaultContextKeys.WORLD_KEY, document.getString("world"));
        }

        if (document.containsKey("expiry")) {
            builder.expiry(document.getLong("expiry"));
        }

        if (document.containsKey("context") && document.get("context") instanceof List) {
            //noinspection unchecked
            List<Document> contexts = (List<Document>) document.get("context");
            builder.withContext(docsToContextSet(contexts));
        }

        return builder.build();
    }

    private static List<Document> contextSetToDocs(ContextSet contextSet) {
        List<Document> contexts = new ArrayList<>(contextSet.size());
        for (Context e : contextSet) {
            contexts.add(new Document().append("key", e.getKey()).append("value", e.getValue()));
        }
        return contexts;
    }

    private static MutableContextSet docsToContextSet(List<Document> documents) {
        MutableContextSet map = new MutableContextSetImpl();
        for (Document doc : documents) {
            map.add(doc.getString("key"), doc.getString("value"));
        }
        return map;
    }

}
