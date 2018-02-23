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

package me.lucko.luckperms.common.storage.dao.file;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.contexts.ContextSetConfigurateSerializer;
import me.lucko.luckperms.common.managers.group.GroupManager;
import me.lucko.luckperms.common.managers.track.TrackManager;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.MetaType;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.node.NodeHeldPermission;
import me.lucko.luckperms.common.node.NodeModel;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.references.UserIdentifier;
import me.lucko.luckperms.common.storage.dao.AbstractDao;
import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.common.utils.Uuids;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.SimpleConfigurationNode;
import ninja.leaping.configurate.Types;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ConfigurateDao extends AbstractDao {
    private final FileUuidCache uuidCache = new FileUuidCache();
    private final FileActionLogger actionLogger = new FileActionLogger();

    private final String fileExtension;
    private final String dataFolderName;

    private File uuidDataFile;
    private File actionLogFile;

    private File usersDirectory;
    private File groupsDirectory;
    private File tracksDirectory;

    protected ConfigurateDao(LuckPermsPlugin plugin, String name, String fileExtension, String dataFolderName) {
        super(plugin, name);
        this.fileExtension = fileExtension;
        this.dataFolderName = dataFolderName;
    }

    public String getFileExtension() {
        return this.fileExtension;
    }

    protected abstract ConfigurationLoader<? extends ConfigurationNode> loader(Path path);

    private ConfigurationNode readFile(StorageLocation location, String name) throws IOException {
        File file = new File(getDirectory(location), name + this.fileExtension);
        registerFileAction(location, file);
        return readFile(file);
    }

    private ConfigurationNode readFile(File file) throws IOException {
        if (!file.exists()) {
            return null;
        }

        return loader(file.toPath()).load();
    }

    private void saveFile(StorageLocation location, String name, ConfigurationNode node) throws IOException {
        File file = new File(getDirectory(location), name + this.fileExtension);
        registerFileAction(location, file);
        saveFile(file, node);
    }

    private void saveFile(File file, ConfigurationNode node) throws IOException {
        if (node == null) {
            if (file.exists()) {
                file.delete();
            }
            return;
        }

        loader(file.toPath()).save(node);
    }

    private File getDirectory(StorageLocation location) {
        switch (location) {
            case USER:
                return this.usersDirectory;
            case GROUP:
                return this.groupsDirectory;
            case TRACK:
                return this.tracksDirectory;
            default:
                throw new RuntimeException();
        }
    }

    private FilenameFilter getFileTypeFilter() {
        return (dir, name) -> name.endsWith(this.fileExtension);
    }

    private Exception reportException(String file, Exception ex) throws Exception {
        this.plugin.getLog().warn("Exception thrown whilst performing i/o: " + file);
        ex.printStackTrace();
        throw ex;
    }

    private void registerFileAction(StorageLocation type, File file) {
        this.plugin.getFileWatcher().ifPresent(fileWatcher -> fileWatcher.registerChange(type, file.getName()));
    }

    @Override
    public void init() {
        try {
            File data = FileUtils.mkdirs(new File(this.plugin.getDataDirectory(), this.dataFolderName));

            this.usersDirectory = FileUtils.mkdir(new File(data, "users"));
            this.groupsDirectory = FileUtils.mkdir(new File(data, "groups"));
            this.tracksDirectory = FileUtils.mkdir(new File(data, "tracks"));
            this.uuidDataFile = FileUtils.createNewFile(new File(data, "uuidcache.txt"));
            this.actionLogFile = FileUtils.createNewFile(new File(data, "actions.log"));

            // Listen for file changes.
            this.plugin.getFileWatcher().ifPresent(watcher -> {
                watcher.subscribe("user", this.usersDirectory.toPath(), s -> {
                    if (!s.endsWith(this.fileExtension)) {
                        return;
                    }

                    String user = s.substring(0, s.length() - this.fileExtension.length());
                    UUID uuid = Uuids.parseNullable(user);
                    if (uuid == null) {
                        return;
                    }

                    User u = this.plugin.getUserManager().getIfLoaded(uuid);
                    if (u != null) {
                        this.plugin.getLog().info("[FileWatcher] Refreshing user " + u.getFriendlyName());
                        this.plugin.getStorage().loadUser(uuid, null);
                    }
                });
                watcher.subscribe("group", this.groupsDirectory.toPath(), s -> {
                    if (!s.endsWith(this.fileExtension)) {
                        return;
                    }

                    String groupName = s.substring(0, s.length() - this.fileExtension.length());
                    this.plugin.getLog().info("[FileWatcher] Refreshing group " + groupName);
                    this.plugin.getUpdateTaskBuffer().request();
                });
                watcher.subscribe("track", this.tracksDirectory.toPath(), s -> {
                    if (!s.endsWith(this.fileExtension)) {
                        return;
                    }

                    String trackName = s.substring(0, s.length() - this.fileExtension.length());
                    this.plugin.getLog().info("[FileWatcher] Refreshing track " + trackName);
                    this.plugin.getStorage().loadAllTracks();
                });
            });
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        this.uuidCache.load(this.uuidDataFile);
        this.actionLogger.init(this.actionLogFile);
    }

    @Override
    public void shutdown() {
        this.uuidCache.save(this.uuidDataFile);
    }

    @Override
    public void logAction(LogEntry entry) {
        this.actionLogger.logAction(entry);
    }

    @Override
    public Log getLog() {
        // File based daos don't support viewing log data from in-game.
        // You can just read the file in a text editor.
        return Log.empty();
    }

    @Override
    public void applyBulkUpdate(BulkUpdate bulkUpdate) throws Exception {
        if (bulkUpdate.getDataType().isIncludingUsers()) {
            File[] files = getDirectory(StorageLocation.USER).listFiles(getFileTypeFilter());
            if (files == null) {
                throw new IllegalStateException("Users directory matched no files.");
            }

            for (File file : files) {
                try {
                    registerFileAction(StorageLocation.USER, file);
                    ConfigurationNode object = readFile(file);
                    Set<NodeModel> nodes = readNodes(object);
                    Set<NodeModel> results = nodes.stream()
                            .map(bulkUpdate::apply)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    if (!nodes.equals(results)) {
                        writeNodes(object, results);
                        saveFile(file, object);
                    }
                } catch (Exception e) {
                    throw reportException(file.getName(), e);
                }
            }
        }

        if (bulkUpdate.getDataType().isIncludingGroups()) {
            File[] files = getDirectory(StorageLocation.GROUP).listFiles(getFileTypeFilter());
            if (files == null) {
                throw new IllegalStateException("Groups directory matched no files.");
            }

            for (File file : files) {
                try {
                    registerFileAction(StorageLocation.GROUP, file);
                    ConfigurationNode object = readFile(file);
                    Set<NodeModel> nodes = readNodes(object);
                    Set<NodeModel> results = nodes.stream()
                            .map(bulkUpdate::apply)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    if (!nodes.equals(results)) {
                        writeNodes(object, results);
                        saveFile(file, object);
                    }
                } catch (Exception e) {
                    throw reportException(file.getName(), e);
                }
            }
        }
    }

    @Override
    public User loadUser(UUID uuid, String username) throws Exception {
        User user = this.plugin.getUserManager().getOrMake(UserIdentifier.of(uuid, username));
        user.getIoLock().lock();
        try {
            ConfigurationNode object = readFile(StorageLocation.USER, uuid.toString());
            if (object != null) {
                String name = object.getNode("name").getString();
                user.getPrimaryGroup().setStoredValue(object.getNode(this instanceof JsonDao ? "primaryGroup" : "primary-group").getString());

                Set<Node> nodes = readNodes(object).stream().map(NodeModel::toNode).collect(Collectors.toSet());
                user.setEnduringNodes(nodes);
                user.setName(name, true);

                boolean save = this.plugin.getUserManager().giveDefaultIfNeeded(user, false);
                if (user.getName().isPresent() && (name == null || !user.getName().get().equalsIgnoreCase(name))) {
                    save = true;
                }

                if (save | user.auditTemporaryPermissions()) {
                    saveUser(user);
                }
            } else {
                if (this.plugin.getUserManager().shouldSave(user)) {
                    user.clearNodes();
                    user.getPrimaryGroup().setStoredValue(null);
                    this.plugin.getUserManager().giveDefaultIfNeeded(user, false);
                }
            }
        } catch (Exception e) {
            throw reportException(uuid.toString(), e);
        } finally {
            user.getIoLock().unlock();
        }
        user.getRefreshBuffer().requestDirectly();
        return user;
    }

    @Override
    public void saveUser(User user) throws Exception {
        user.getIoLock().lock();
        try {
            if (!this.plugin.getUserManager().shouldSave(user)) {
                saveFile(StorageLocation.USER, user.getUuid().toString(), null);
            } else {
                ConfigurationNode data = SimpleConfigurationNode.root();
                data.getNode("uuid").setValue(user.getUuid().toString());
                data.getNode("name").setValue(user.getName().orElse("null"));
                data.getNode(this instanceof JsonDao ? "primaryGroup" : "primary-group").setValue(user.getPrimaryGroup().getStoredValue().orElse(NodeFactory.DEFAULT_GROUP_NAME));

                Set<NodeModel> nodes = user.getEnduringNodes().values().stream().map(NodeModel::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
                writeNodes(data, nodes);

                saveFile(StorageLocation.USER, user.getUuid().toString(), data);
            }
        } catch (Exception e) {
            throw reportException(user.getUuid().toString(), e);
        } finally {
            user.getIoLock().unlock();
        }
    }

    @Override
    public Set<UUID> getUniqueUsers() {
        String[] fileNames = this.usersDirectory.list(getFileTypeFilter());
        if (fileNames == null) return null;
        return Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - this.fileExtension.length()))
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }

    @Override
    public List<HeldPermission<UUID>> getUsersWithPermission(String permission) throws Exception {
        List<HeldPermission<UUID>> held = new ArrayList<>();
        File[] files = getDirectory(StorageLocation.USER).listFiles(getFileTypeFilter());
        if (files == null) {
            throw new IllegalStateException("Users directory matched no files.");
        }

        for (File file : files) {
            try {
                registerFileAction(StorageLocation.USER, file);
                ConfigurationNode object = readFile(file);
                UUID holder = UUID.fromString(file.getName().substring(0, file.getName().length() - this.fileExtension.length()));
                Set<NodeModel> nodes = readNodes(object);
                for (NodeModel e : nodes) {
                    if (!e.getPermission().equalsIgnoreCase(permission)) {
                        continue;
                    }
                    held.add(NodeHeldPermission.of(holder, e));
                }
            } catch (Exception e) {
                throw reportException(file.getName(), e);
            }
        }
        return held;
    }

    @Override
    public Group createAndLoadGroup(String name) throws Exception {
        Group group = this.plugin.getGroupManager().getOrMake(name);
        group.getIoLock().lock();
        try {
            ConfigurationNode object = readFile(StorageLocation.GROUP, name);

            if (object != null) {
                Set<Node> nodes = readNodes(object).stream().map(NodeModel::toNode).collect(Collectors.toSet());
                group.setEnduringNodes(nodes);
            } else {
                ConfigurationNode data = SimpleConfigurationNode.root();
                data.getNode("name").setValue(group.getName());

                Set<NodeModel> nodes = group.getEnduringNodes().values().stream().map(NodeModel::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
                writeNodes(data, nodes);

                saveFile(StorageLocation.GROUP, name, data);
            }
        } catch (Exception e) {
            throw reportException(name, e);
        } finally {
            group.getIoLock().unlock();
        }
        group.getRefreshBuffer().requestDirectly();
        return group;
    }

    @Override
    public Optional<Group> loadGroup(String name) throws Exception {
        Group group = this.plugin.getGroupManager().getIfLoaded(name);
        if (group != null) {
            group.getIoLock().lock();
        }

        try {
            ConfigurationNode object = readFile(StorageLocation.GROUP, name);

            if (object == null) {
                return Optional.empty();
            }

            if (group == null) {
                group = this.plugin.getGroupManager().getOrMake(name);
                group.getIoLock().lock();
            }

            Set<NodeModel> data = readNodes(object);
            Set<Node> nodes = data.stream().map(NodeModel::toNode).collect(Collectors.toSet());
            group.setEnduringNodes(nodes);

        } catch (Exception e) {
            throw reportException(name, e);
        } finally {
            if (group != null) {
                group.getIoLock().unlock();
            }
        }
        group.getRefreshBuffer().requestDirectly();
        return Optional.of(group);
    }

    @Override
    public void loadAllGroups() throws IOException {
        String[] fileNames = this.groupsDirectory.list(getFileTypeFilter());
        if (fileNames == null) {
            throw new IOException("Not a directory");
        }
        List<String> groups = Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - this.fileExtension.length()))
                .collect(Collectors.toList());

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

        GroupManager<?> gm = this.plugin.getGroupManager();
        gm.getAll().values().stream()
                .filter(g -> !groups.contains(g.getName()))
                .forEach(gm::unload);
    }

    @Override
    public void saveGroup(Group group) throws Exception {
        group.getIoLock().lock();
        try {
            ConfigurationNode data = SimpleConfigurationNode.root();
            data.getNode("name").setValue(group.getName());

            Set<NodeModel> nodes = group.getEnduringNodes().values().stream().map(NodeModel::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
            writeNodes(data, nodes);

            saveFile(StorageLocation.GROUP, group.getName(), data);
        } catch (Exception e) {
            throw reportException(group.getName(), e);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public void deleteGroup(Group group) throws Exception {
        group.getIoLock().lock();
        try {
            File groupFile = new File(this.groupsDirectory, group.getName() + this.fileExtension);
            registerFileAction(StorageLocation.GROUP, groupFile);

            if (groupFile.exists()) {
                groupFile.delete();
            }
        } catch (Exception e) {
            throw reportException(group.getName(), e);
        } finally {
            group.getIoLock().unlock();
        }
        this.plugin.getGroupManager().unload(group);
    }

    @Override
    public List<HeldPermission<String>> getGroupsWithPermission(String permission) throws Exception {
        List<HeldPermission<String>> held = new ArrayList<>();
        File[] files = getDirectory(StorageLocation.GROUP).listFiles(getFileTypeFilter());
        if (files == null) {
            throw new IllegalStateException("Groups directory matched no files.");
        }

        for (File file : files) {
            try {
                registerFileAction(StorageLocation.GROUP, file);
                ConfigurationNode object = readFile(file);
                String holder = file.getName().substring(0, file.getName().length() - this.fileExtension.length());
                Set<NodeModel> nodes = readNodes(object);
                for (NodeModel e : nodes) {
                    if (!e.getPermission().equalsIgnoreCase(permission)) {
                        continue;
                    }
                    held.add(NodeHeldPermission.of(holder, e));
                }
            } catch (Exception e) {
                throw reportException(file.getName(), e);
            }
        }
        return held;
    }

    @Override
    public Track createAndLoadTrack(String name) throws Exception {
        Track track = this.plugin.getTrackManager().getOrMake(name);
        track.getIoLock().lock();
        try {
            ConfigurationNode object = readFile(StorageLocation.TRACK, name);

            if (object != null) {
                List<String> groups = object.getNode("groups").getChildrenList().stream()
                        .map(ConfigurationNode::getString)
                        .collect(ImmutableCollectors.toList());

                track.setGroups(groups);
            } else {
                ConfigurationNode data = SimpleConfigurationNode.root();
                data.getNode("name").setValue(name);
                data.getNode("groups").setValue(track.getGroups());
                saveFile(StorageLocation.TRACK, name, data);
            }

        } catch (Exception e) {
            throw reportException(name, e);
        } finally {
            track.getIoLock().unlock();
        }
        return track;
    }

    @Override
    public Optional<Track> loadTrack(String name) throws Exception {
        Track track = this.plugin.getTrackManager().getIfLoaded(name);
        if (track != null) {
            track.getIoLock().lock();
        }

        try {
            ConfigurationNode object = readFile(StorageLocation.TRACK, name);

            if (object == null) {
                return Optional.empty();
            }

            if (track == null) {
                track = this.plugin.getTrackManager().getOrMake(name);
                track.getIoLock().lock();
            }

            List<String> groups = object.getNode("groups").getChildrenList().stream()
                    .map(ConfigurationNode::getString)
                    .collect(ImmutableCollectors.toList());

            track.setGroups(groups);

        } catch (Exception e) {
            throw reportException(name, e);
        } finally {
            if (track != null) {
                track.getIoLock().unlock();
            }
        }
        return Optional.of(track);
    }

    @Override
    public void loadAllTracks() throws IOException {
        String[] fileNames = this.tracksDirectory.list(getFileTypeFilter());
        if (fileNames == null) {
            throw new IOException("Not a directory");
        }
        List<String> tracks = Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - this.fileExtension.length()))
                .collect(Collectors.toList());

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

        TrackManager<?> tm = this.plugin.getTrackManager();
        tm.getAll().values().stream()
                .filter(t -> !tracks.contains(t.getName()))
                .forEach(tm::unload);
    }

    @Override
    public void saveTrack(Track track) throws Exception {
        track.getIoLock().lock();
        try {
            ConfigurationNode data = SimpleConfigurationNode.root();
            data.getNode("name").setValue(track.getName());
            data.getNode("groups").setValue(track.getGroups());
            saveFile(StorageLocation.TRACK, track.getName(), data);
        } catch (Exception e) {
            throw reportException(track.getName(), e);
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public void deleteTrack(Track track) throws Exception {
        track.getIoLock().lock();
        try {
            File trackFile = new File(this.tracksDirectory, track.getName() + this.fileExtension);
            registerFileAction(StorageLocation.TRACK, trackFile);

            if (trackFile.exists()) {
                trackFile.delete();
            }
        } catch (Exception e) {
            throw reportException(track.getName(), e);
        } finally {
            track.getIoLock().unlock();
        }
        this.plugin.getTrackManager().unload(track);
    }

    @Override
    public void saveUUIDData(UUID uuid, String username) {
        this.uuidCache.addMapping(uuid, username);
    }

    @Override
    public UUID getUUID(String username) {
        return this.uuidCache.lookup(username);
    }

    @Override
    public String getName(UUID uuid) {
        return this.uuidCache.lookupUsername(uuid);
    }

    private static NodeModel readAttributes(ConfigurationNode attributes, Function<ConfigurationNode, String> permissionFunction) {
        boolean value = attributes.getNode("value").getBoolean(true);
        String server = attributes.getNode("server").getString("global");
        String world = attributes.getNode("world").getString("global");
        long expiry = attributes.getNode("expiry").getLong(0L);

        ImmutableContextSet context = ImmutableContextSet.empty();
        ConfigurationNode contextMap = attributes.getNode("context");
        if (!contextMap.isVirtual() && contextMap.hasMapChildren()) {
            context = ContextSetConfigurateSerializer.deserializeContextSet(contextMap).makeImmutable();
        }

        return NodeModel.of(permissionFunction.apply(attributes), value, server, world, expiry, context);
    }

    private static Collection<NodeModel> readAttributes(ConfigurationNode attributes, String permission) {
        boolean value = attributes.getNode("value").getBoolean(true);
        String server = attributes.getNode("server").getString("global");
        String world = attributes.getNode("world").getString("global");
        long expiry = attributes.getNode("expiry").getLong(0L);

        ImmutableContextSet context = ImmutableContextSet.empty();
        ConfigurationNode contextMap = attributes.getNode("context");
        if (!contextMap.isVirtual() && contextMap.hasMapChildren()) {
            context = ContextSetConfigurateSerializer.deserializeContextSet(contextMap).makeImmutable();
        }

        ConfigurationNode batchAttribute = attributes.getNode("permissions");
        if (permission.startsWith("luckperms.batch") && !batchAttribute.isVirtual() && batchAttribute.hasListChildren()) {
            List<NodeModel> nodes = new ArrayList<>();
            for (ConfigurationNode element : batchAttribute.getChildrenList()) {
                nodes.add(NodeModel.of(element.getString(), value, server, world, expiry, context));
            }
            return nodes;
        } else {
            return Collections.singleton(NodeModel.of(permission, value, server, world, expiry, context));
        }
    }

    private static Map.Entry<String, ConfigurationNode> parseEntry(ConfigurationNode appended) {
        if (!appended.hasMapChildren()) {
            return null;
        }
        Map.Entry<Object, ? extends ConfigurationNode> entry = Iterables.getFirst(appended.getChildrenMap().entrySet(), null);
        if (entry == null || !entry.getValue().hasMapChildren()) {
            return null;
        }

        return Maps.immutableEntry(entry.getKey().toString(), entry.getValue());
    }

    private static Set<NodeModel> readNodes(ConfigurationNode data) {
        Set<NodeModel> nodes = new HashSet<>();

        if (data.getNode("permissions").hasListChildren()) {
            List<? extends ConfigurationNode> children = data.getNode("permissions").getChildrenList();
            for (ConfigurationNode appended : children) {
                String plainValue = appended.getValue(Types::strictAsString);
                if (plainValue != null) {
                    nodes.add(NodeModel.of(plainValue));
                    continue;
                }

                Map.Entry<String, ConfigurationNode> entry = parseEntry(appended);
                if (entry == null) {
                    continue;
                }
                nodes.addAll(readAttributes(entry.getValue(), entry.getKey()));
            }
        }

        if (data.getNode("parents").hasListChildren()) {
            List<? extends ConfigurationNode> children = data.getNode("parents").getChildrenList();
            for (ConfigurationNode appended : children) {
                String stringValue = appended.getValue(Types::strictAsString);
                if (stringValue != null) {
                    nodes.add(NodeModel.of(NodeFactory.groupNode(stringValue)));
                    continue;
                }

                Map.Entry<String, ConfigurationNode> entry = parseEntry(appended);
                if (entry == null) {
                    continue;
                }
                nodes.add(readAttributes(entry.getValue(), c -> NodeFactory.groupNode(entry.getKey())));
            }
        }

        for (ChatMetaType chatMetaType : ChatMetaType.values()) {
            String keyName = chatMetaType.toString() + "es";
            if (data.getNode(keyName).hasListChildren()) {
                List<? extends ConfigurationNode> children = data.getNode(keyName).getChildrenList();
                for (ConfigurationNode appended : children) {
                    Map.Entry<String, ConfigurationNode> entry = parseEntry(appended);
                    if (entry == null) {
                        continue;
                    }
                    nodes.add(readAttributes(entry.getValue(), c -> NodeFactory.chatMetaNode(chatMetaType, c.getNode("priority").getInt(0), entry.getKey())));
                }
            }
        }

        if (data.getNode("meta").hasListChildren()) {
            List<? extends ConfigurationNode> children = data.getNode("meta").getChildrenList();
            for (ConfigurationNode appended : children) {
                Map.Entry<String, ConfigurationNode> entry = parseEntry(appended);
                if (entry == null) {
                    continue;
                }
                nodes.add(readAttributes(entry.getValue(), c -> NodeFactory.metaNode(entry.getKey(), entry.getValue().getNode("value").getString("null"))));
            }
        }

        return nodes;
    }

    private static void writeAttributesTo(ConfigurationNode attributes, NodeModel node, boolean writeValue) {
        if (writeValue) {
            attributes.getNode("value").setValue(node.getValue());
        }

        if (!node.getServer().equals("global")) {
            attributes.getNode("server").setValue(node.getServer());
        }

        if (!node.getWorld().equals("global")) {
            attributes.getNode("world").setValue(node.getWorld());
        }

        if (node.getExpiry() != 0L) {
            attributes.getNode("expiry").setValue(node.getExpiry());
        }

        if (!node.getContexts().isEmpty()) {
            attributes.getNode("context").setValue(ContextSetConfigurateSerializer.serializeContextSet(node.getContexts()));
        }
    }

    private static boolean isPlain(NodeModel node) {
        return node.getValue() &&
                node.getServer().equalsIgnoreCase("global") &&
                node.getWorld().equalsIgnoreCase("global") &&
                node.getExpiry() == 0L &&
                node.getContexts().isEmpty();
    }

    private static void writeNodes(ConfigurationNode to, Set<NodeModel> nodes) {
        ConfigurationNode permissionsSection = SimpleConfigurationNode.root();
        ConfigurationNode parentsSection = SimpleConfigurationNode.root();
        ConfigurationNode prefixesSection = SimpleConfigurationNode.root();
        ConfigurationNode suffixesSection = SimpleConfigurationNode.root();
        ConfigurationNode metaSection = SimpleConfigurationNode.root();

        for (NodeModel node : nodes) {
            Node n = node.toNode();

            // just add a string to the list.
            if (isPlain(node)) {
                if (n.isGroupNode()) {
                    parentsSection.getAppendedNode().setValue(n.getGroupName());
                    continue;
                }
                if (!MetaType.ANY.matches(n)) {
                    permissionsSection.getAppendedNode().setValue(node.getPermission());
                    continue;
                }
            }

            ChatMetaType chatMetaType = ChatMetaType.ofNode(n).orElse(null);
            if (chatMetaType != null && n.getValuePrimitive()) {
                // handle prefixes / suffixes
                Map.Entry<Integer, String> entry = chatMetaType.getEntry(n);

                ConfigurationNode attributes = SimpleConfigurationNode.root();
                attributes.getNode("priority").setValue(entry.getKey());
                writeAttributesTo(attributes, node, false);

                ConfigurationNode appended = SimpleConfigurationNode.root();
                appended.getNode(entry.getValue()).setValue(attributes);

                switch (chatMetaType) {
                    case PREFIX:
                        prefixesSection.getAppendedNode().setValue(appended);
                        break;
                    case SUFFIX:
                        suffixesSection.getAppendedNode().setValue(appended);
                        break;
                    default:
                        throw new AssertionError();
                }
            } else if (n.isMeta() && n.getValuePrimitive()) {
                // handle meta nodes
                Map.Entry<String, String> meta = n.getMeta();

                ConfigurationNode attributes = SimpleConfigurationNode.root();
                attributes.getNode("value").setValue(meta.getValue());
                writeAttributesTo(attributes, node, false);

                ConfigurationNode appended = SimpleConfigurationNode.root();
                appended.getNode(meta.getKey()).setValue(attributes);

                metaSection.getAppendedNode().setValue(appended);
            } else if (n.isGroupNode() && n.getValuePrimitive()) {
                // handle group nodes
                ConfigurationNode attributes = SimpleConfigurationNode.root();
                writeAttributesTo(attributes, node, false);

                ConfigurationNode appended = SimpleConfigurationNode.root();
                appended.getNode(n.getGroupName()).setValue(attributes);

                parentsSection.getAppendedNode().setValue(appended);
            } else {
                // handle regular permissions and negated meta+prefixes+suffixes
                ConfigurationNode attributes = SimpleConfigurationNode.root();
                writeAttributesTo(attributes, node, true);

                ConfigurationNode appended = SimpleConfigurationNode.root();
                appended.getNode(n.getPermission()).setValue(attributes);

                permissionsSection.getAppendedNode().setValue(appended);
            }
        }

        if (permissionsSection.hasListChildren()) {
            to.getNode("permissions").setValue(permissionsSection);
        }
        if (parentsSection.hasListChildren()) {
            to.getNode("parents").setValue(parentsSection);
        }
        if (prefixesSection.hasListChildren()) {
            to.getNode("prefixes").setValue(prefixesSection);
        }
        if (suffixesSection.hasListChildren()) {
            to.getNode("suffixes").setValue(suffixesSection);
        }
        if (metaSection.hasListChildren()) {
            to.getNode("meta").setValue(metaSection);
        }
    }

}
