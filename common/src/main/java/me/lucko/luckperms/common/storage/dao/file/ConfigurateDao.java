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

import lombok.Getter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.contexts.ContextSetConfigurateSerializer;
import me.lucko.luckperms.common.managers.GenericUserManager;
import me.lucko.luckperms.common.managers.GroupManager;
import me.lucko.luckperms.common.managers.TrackManager;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.node.NodeHeldPermission;
import me.lucko.luckperms.common.node.NodeModel;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.references.UserIdentifier;
import me.lucko.luckperms.common.storage.dao.AbstractDao;
import me.lucko.luckperms.common.storage.dao.legacy.LegacyJsonMigration;
import me.lucko.luckperms.common.storage.dao.legacy.LegacyYamlMigration;
import me.lucko.luckperms.common.utils.ImmutableCollectors;

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
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class ConfigurateDao extends AbstractDao {
    private static final String LOG_FORMAT = "%s(%s): [%s] %s(%s) --> %s";

    private final Logger actionLogger = Logger.getLogger("luckperms_actions");
    private final FileUuidCache uuidCache = new FileUuidCache();

    @Getter
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

    protected abstract ConfigurationLoader<? extends ConfigurationNode> loader(Path path);

    private ConfigurationNode readFile(StorageLocation location, String name) throws IOException {
        File file = new File(getDirectory(location), name + fileExtension);
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
        File file = new File(getDirectory(location), name + fileExtension);
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
                return usersDirectory;
            case GROUP:
                return groupsDirectory;
            case TRACK:
                return tracksDirectory;
            default:
                throw new RuntimeException();
        }
    }

    private FilenameFilter getFileTypeFilter() {
        return (dir, name) -> name.endsWith(fileExtension);
    }

    private Exception reportException(String file, Exception ex) throws Exception {
        plugin.getLog().warn("Exception thrown whilst performing i/o: " + file);
        ex.printStackTrace();
        throw ex;
    }

    private void registerFileAction(StorageLocation type, File file) {
        plugin.getFileWatcher().ifPresent(fileWatcher -> fileWatcher.registerChange(type, file.getName()));
    }

    @Override
    public void init() {
        try {
            setupFiles();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        uuidCache.load(uuidDataFile);

        try {
            FileHandler fh = new FileHandler(actionLogFile.getAbsolutePath(), 0, 1, true);
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
    }

    private static void mkdir(File file) throws IOException {
        if (file.exists()) {
            return;
        }
        if (!file.mkdir()) {
            throw new IOException("Unable to create directory - " + file.getPath());
        }
    }

    private static void mkdirs(File file) throws IOException {
        if (file.exists()) {
            return;
        }
        if (!file.mkdirs()) {
            throw new IOException("Unable to create directory - " + file.getPath());
        }
    }

    private void setupFiles() throws IOException {
        File data = new File(plugin.getDataDirectory(), dataFolderName);

        // Try to perform schema migration
        File oldData = new File(plugin.getDataDirectory(), "data");

        if (!data.exists() && oldData.exists()) {
            mkdirs(data);

            plugin.getLog().severe("===== Legacy Schema Migration =====");
            plugin.getLog().severe("Starting migration from legacy schema. This could take a while....");
            plugin.getLog().severe("Please do not stop your server while the migration takes place.");

            if (this instanceof YamlDao) {
                try {
                    new LegacyYamlMigration(plugin, (YamlDao) this, oldData, data).run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (this instanceof JsonDao) {
                try {
                    new LegacyJsonMigration(plugin, (JsonDao) this, oldData, data).run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            mkdirs(data);
        }

        usersDirectory = new File(data, "users");
        mkdir(usersDirectory);

        groupsDirectory = new File(data, "groups");
        mkdir(groupsDirectory);

        tracksDirectory = new File(data, "tracks");
        mkdir(tracksDirectory);

        uuidDataFile = new File(data, "uuidcache.txt");
        uuidDataFile.createNewFile();

        actionLogFile = new File(data, "actions.log");
        actionLogFile.createNewFile();

        // Listen for file changes.
        plugin.getFileWatcher().ifPresent(watcher -> {
            watcher.subscribe("user", usersDirectory.toPath(), s -> {
                if (!s.endsWith(fileExtension)) {
                    return;
                }

                String user = s.substring(0, s.length() - fileExtension.length());
                UUID uuid = CommandUtils.parseUuid(user);
                if (uuid == null) {
                    return;
                }

                User u = plugin.getUserManager().getIfLoaded(uuid);
                if (u != null) {
                    plugin.getLog().info("[FileWatcher] Refreshing user " + u.getFriendlyName());
                    plugin.getStorage().loadUser(uuid, null);
                }
            });
            watcher.subscribe("group", groupsDirectory.toPath(), s -> {
                if (!s.endsWith(fileExtension)) {
                    return;
                }

                String groupName = s.substring(0, s.length() - fileExtension.length());
                plugin.getLog().info("[FileWatcher] Refreshing group " + groupName);
                plugin.getUpdateTaskBuffer().request();
            });
            watcher.subscribe("track", tracksDirectory.toPath(), s -> {
                if (!s.endsWith(fileExtension)) {
                    return;
                }

                String trackName = s.substring(0, s.length() - fileExtension.length());
                plugin.getLog().info("[FileWatcher] Refreshing track " + trackName);
                plugin.getStorage().loadAllTracks();
            });
        });
    }

    @Override
    public void shutdown() {
        uuidCache.save(uuidDataFile);
    }

    @Override
    public void logAction(LogEntry entry) {
        actionLogger.info(String.format(LOG_FORMAT,
                (entry.getActor().equals(CommandManager.CONSOLE_UUID) ? "" : entry.getActor() + " "),
                entry.getActorName(),
                Character.toString(entry.getType().getCode()),
                entry.getActed().map(e -> e.toString() + " ").orElse(""),
                entry.getActedName(),
                entry.getAction())
        );
    }

    @Override
    public Log getLog() {
        // Flatfile doesn't support viewing log data from in-game. You can just read the file in a text editor.
        return Log.builder().build();
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
        User user = plugin.getUserManager().getOrMake(UserIdentifier.of(uuid, username));
        user.getIoLock().lock();
        try {
            ConfigurationNode object = readFile(StorageLocation.USER, uuid.toString());
            if (object != null) {
                String name = object.getNode("name").getString();
                user.getPrimaryGroup().setStoredValue(object.getNode(this instanceof JsonDao ? "primaryGroup" : "primary-group").getString());

                Set<Node> nodes = readNodes(object).stream().map(NodeModel::toNode).collect(Collectors.toSet());
                user.setEnduringNodes(nodes);
                user.setName(name, true);

                boolean save = plugin.getUserManager().giveDefaultIfNeeded(user, false);
                if (user.getName().isPresent() && (name == null || !user.getName().get().equalsIgnoreCase(name))) {
                    save = true;
                }

                if (save) {
                    saveUser(user);
                }
            } else {
                if (GenericUserManager.shouldSave(user)) {
                    user.clearNodes();
                    user.getPrimaryGroup().setStoredValue(null);
                    plugin.getUserManager().giveDefaultIfNeeded(user, false);
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
            if (!GenericUserManager.shouldSave(user)) {
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
        String[] fileNames = usersDirectory.list(getFileTypeFilter());
        if (fileNames == null) return null;
        return Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - fileExtension.length()))
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }

    @Override
    public List<HeldPermission<UUID>> getUsersWithPermission(String permission) throws Exception {
        ImmutableList.Builder<HeldPermission<UUID>> held = ImmutableList.builder();
        File[] files = getDirectory(StorageLocation.USER).listFiles(getFileTypeFilter());
        if (files == null) {
            throw new IllegalStateException("Users directory matched no files.");
        }

        for (File file : files) {
            try {
                registerFileAction(StorageLocation.USER, file);
                ConfigurationNode object = readFile(file);
                UUID holder = UUID.fromString(file.getName().substring(0, file.getName().length() - fileExtension.length()));
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
        return held.build();
    }

    @Override
    public Group createAndLoadGroup(String name) throws Exception {
        Group group = plugin.getGroupManager().getOrMake(name);
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
        Group group = plugin.getGroupManager().getIfLoaded(name);
        if (group != null) {
            group.getIoLock().lock();
        }

        try {
            ConfigurationNode object = readFile(StorageLocation.GROUP, name);

            if (object == null) {
                return Optional.empty();
            }

            if (group == null) {
                group = plugin.getGroupManager().getOrMake(name);
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
        String[] fileNames = groupsDirectory.list(getFileTypeFilter());
        if (fileNames == null) {
            throw new IOException("Not a directory");
        }
        List<String> groups = Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - fileExtension.length()))
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

        GroupManager gm = plugin.getGroupManager();
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
            File groupFile = new File(groupsDirectory, group.getName() + fileExtension);
            registerFileAction(StorageLocation.GROUP, groupFile);

            if (groupFile.exists()) {
                groupFile.delete();
            }
        } catch (Exception e) {
            throw reportException(group.getName(), e);
        } finally {
            group.getIoLock().unlock();
        }
        plugin.getGroupManager().unload(group);
    }

    @Override
    public List<HeldPermission<String>> getGroupsWithPermission(String permission) throws Exception {
        ImmutableList.Builder<HeldPermission<String>> held = ImmutableList.builder();
        File[] files = getDirectory(StorageLocation.GROUP).listFiles(getFileTypeFilter());
        if (files == null) {
            throw new IllegalStateException("Groups directory matched no files.");
        }

        for (File file : files) {
            try {
                registerFileAction(StorageLocation.GROUP, file);
                ConfigurationNode object = readFile(file);
                String holder = file.getName().substring(0, file.getName().length() - fileExtension.length());
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
        return held.build();
    }

    @Override
    public Track createAndLoadTrack(String name) throws Exception {
        Track track = plugin.getTrackManager().getOrMake(name);
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
        Track track = plugin.getTrackManager().getIfLoaded(name);
        if (track != null) {
            track.getIoLock().lock();
        }

        try {
            ConfigurationNode object = readFile(StorageLocation.TRACK, name);

            if (object == null) {
                return Optional.empty();
            }

            if (track == null) {
                track = plugin.getTrackManager().getOrMake(name);
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
        String[] fileNames = tracksDirectory.list(getFileTypeFilter());
        if (fileNames == null) {
            throw new IOException("Not a directory");
        }
        List<String> tracks = Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - fileExtension.length()))
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

        TrackManager tm = plugin.getTrackManager();
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
            File trackFile = new File(tracksDirectory, track.getName() + fileExtension);
            registerFileAction(StorageLocation.TRACK, trackFile);

            if (trackFile.exists()) {
                trackFile.delete();
            }
        } catch (Exception e) {
            throw reportException(track.getName(), e);
        } finally {
            track.getIoLock().unlock();
        }
        plugin.getTrackManager().unload(track);
    }

    @Override
    public void saveUUIDData(UUID uuid, String username) {
        uuidCache.addMapping(uuid, username);
    }

    @Override
    public UUID getUUID(String username) {
        return uuidCache.lookupUUID(username);
    }

    @Override
    public String getName(UUID uuid) {
        return uuidCache.lookupUsername(uuid);
    }

    private static Collection<NodeModel> readAttributes(ConfigurationNode entry, String permission) {
        Map<Object, ? extends ConfigurationNode> attributes = entry.getChildrenMap();

        boolean value = true;
        String server = "global";
        String world = "global";
        long expiry = 0L;
        ImmutableContextSet context = ImmutableContextSet.empty();

        if (attributes.containsKey("value")) {
            value = attributes.get("value").getBoolean();
        }
        if (attributes.containsKey("server")) {
            server = attributes.get("server").getString();
        }
        if (attributes.containsKey("world")) {
            world = attributes.get("world").getString();
        }
        if (attributes.containsKey("expiry")) {
            expiry = attributes.get("expiry").getLong();
        }

        if (attributes.containsKey("context") && attributes.get("context").hasMapChildren()) {
            ConfigurationNode contexts = attributes.get("context");
            context = ContextSetConfigurateSerializer.deserializeContextSet(contexts).makeImmutable();
        }

        ConfigurationNode batchAttribute = attributes.get("permissions");
        if (permission.startsWith("luckperms.batch") && batchAttribute != null && batchAttribute.hasListChildren()) {
            List<NodeModel> nodes = new ArrayList<>();
            for (ConfigurationNode element : batchAttribute.getChildrenList()) {
                nodes.add(NodeModel.of(element.getString(), value, server, world, expiry, context));
            }
            return nodes;
        } else {
            return Collections.singleton(NodeModel.of(permission, value, server, world, expiry, context));
        }
    }

    private static Set<NodeModel> readNodes(ConfigurationNode data) {
        Set<NodeModel> nodes = new HashSet<>();

        if (data.getNode("permissions").hasListChildren()) {
            List<? extends ConfigurationNode> parts = data.getNode("permissions").getChildrenList();

            for (ConfigurationNode ent : parts) {
                String stringValue = ent.getValue(Types::strictAsString);
                if (stringValue != null) {
                    nodes.add(NodeModel.of(stringValue, true, "global", "global", 0L, ImmutableContextSet.empty()));
                    continue;
                }

                if (!ent.hasMapChildren()) {
                    continue;
                }

                Map.Entry<Object, ? extends ConfigurationNode> entry = Iterables.getFirst(ent.getChildrenMap().entrySet(), null);
                if (entry == null || !entry.getValue().hasMapChildren()) {
                    continue;
                }

                String permission = entry.getKey().toString();
                nodes.addAll(readAttributes(entry.getValue(), permission));
            }
        }

        if (data.getNode("parents").hasListChildren()) {
            List<? extends ConfigurationNode> parts = data.getNode("parents").getChildrenList();

            for (ConfigurationNode ent : parts) {
                String stringValue = ent.getValue(Types::strictAsString);
                if (stringValue != null) {
                    nodes.add(NodeModel.of(NodeFactory.groupNode(stringValue), true, "global", "global", 0L, ImmutableContextSet.empty()));
                    continue;
                }

                if (!ent.hasMapChildren()) {
                    continue;
                }

                Map.Entry<Object, ? extends ConfigurationNode> entry = Iterables.getFirst(ent.getChildrenMap().entrySet(), null);
                if (entry == null || !entry.getValue().hasMapChildren()) {
                    continue;
                }

                String permission = NodeFactory.groupNode(entry.getKey().toString());
                nodes.addAll(readAttributes(entry.getValue(), permission));
            }
        }

        return nodes;
    }

    private static ConfigurationNode writeAttributes(NodeModel node) {
        ConfigurationNode attributes = SimpleConfigurationNode.root();
        attributes.getNode("value").setValue(node.getValue());

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

        return attributes;
    }

    private static void writeNodes(ConfigurationNode to, Set<NodeModel> nodes) {
        ConfigurationNode permsSection = SimpleConfigurationNode.root();
        ConfigurationNode parentsSection = SimpleConfigurationNode.root();

        for (NodeModel node : nodes) {

            // just a raw, default node.
            boolean single = node.getValue() &&
                    node.getServer().equalsIgnoreCase("global") &&
                    node.getWorld().equalsIgnoreCase("global") &&
                    node.getExpiry() == 0L &&
                    node.getContexts().isEmpty();

            // try to parse out the group
            String group = node.toNode().isGroupNode() ? node.toNode().getGroupName() : null;

            // just add a string to the list.
            if (single) {

                if (group != null) {
                    parentsSection.getAppendedNode().setValue(group);
                    continue;
                }

                permsSection.getAppendedNode().setValue(node.getPermission());
                continue;
            }

            if (group != null) {
                ConfigurationNode ent = SimpleConfigurationNode.root();
                ent.getNode(group).setValue(writeAttributes(node));
                parentsSection.getAppendedNode().setValue(ent);
                continue;
            }

            ConfigurationNode ent = SimpleConfigurationNode.root();
            ent.getNode(node.getPermission()).setValue(writeAttributes(node));
            permsSection.getAppendedNode().setValue(ent);
        }

        to.getNode("permissions").setValue(permsSection);
        to.getNode("parents").setValue(parentsSection);
    }

}
