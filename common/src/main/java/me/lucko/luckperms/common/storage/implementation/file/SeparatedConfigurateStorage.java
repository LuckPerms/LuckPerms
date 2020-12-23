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

package me.lucko.luckperms.common.storage.implementation.file;

import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.matcher.ConstraintNodeMatcher;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.file.loader.ConfigurateLoader;
import me.lucko.luckperms.common.storage.implementation.file.watcher.FileWatcher;
import me.lucko.luckperms.common.storage.misc.NodeEntry;
import me.lucko.luckperms.common.util.Iterators;
import me.lucko.luckperms.common.util.MoreFiles;
import me.lucko.luckperms.common.util.Uuids;

import net.luckperms.api.node.Node;

import ninja.leaping.configurate.ConfigurationNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Flat-file storage using Configurate {@link ConfigurationNode}s.
 * The data for each user/group/track is stored in a separate file.
 */
public class SeparatedConfigurateStorage extends AbstractConfigurateStorage {
    private final String fileExtension;
    private final Predicate<Path> fileExtensionFilter;

    private final Map<StorageLocation, FileGroup> fileGroups;
    private final FileGroup users;
    private final FileGroup groups;
    private final FileGroup tracks;

    private static final class FileGroup {
        private Path directory;
        private FileWatcher.WatchedLocation watcher;
    }

    public SeparatedConfigurateStorage(LuckPermsPlugin plugin, String implementationName, ConfigurateLoader loader, String fileExtension, String dataFolderName) {
        super(plugin, implementationName, loader, dataFolderName);
        this.fileExtension = fileExtension;
        this.fileExtensionFilter = path -> path.getFileName().toString().endsWith(this.fileExtension);

        this.users = new FileGroup();
        this.groups = new FileGroup();
        this.tracks = new FileGroup();

        EnumMap<StorageLocation, FileGroup> fileGroups = new EnumMap<>(StorageLocation.class);
        fileGroups.put(StorageLocation.USERS, this.users);
        fileGroups.put(StorageLocation.GROUPS, this.groups);
        fileGroups.put(StorageLocation.TRACKS, this.tracks);
        this.fileGroups = ImmutableMap.copyOf(fileGroups);
    }

    @Override
    protected ConfigurationNode readFile(StorageLocation location, String name) throws IOException {
        Path file = getDirectory(location).resolve(name + this.fileExtension);
        registerFileAction(location, file);
        return readFile(file);
    }

    private ConfigurationNode readFile(Path file) throws IOException {
        if (!Files.exists(file)) {
            return null;
        }

        return this.loader.loader(file).load();
    }

    @Override
    protected void saveFile(StorageLocation location, String name, ConfigurationNode node) throws IOException {
        Path file = getDirectory(location).resolve(name + this.fileExtension);
        registerFileAction(location, file);
        saveFile(file, node);
    }

    private void saveFile(Path file, ConfigurationNode node) throws IOException {
        if (node == null) {
            Files.deleteIfExists(file);
            return;
        }

        this.loader.loader(file).save(node);
    }

    private Path getDirectory(StorageLocation location) {
        return this.fileGroups.get(location).directory;
    }

    private void registerFileAction(StorageLocation type, Path file) {
        FileWatcher.WatchedLocation watcher = this.fileGroups.get(type).watcher;
        if (watcher != null) {
            watcher.recordChange(file.getFileName().toString());
        }
    }

    @Override
    public void init() throws IOException {
        super.init();

        this.users.directory = MoreFiles.createDirectoryIfNotExists(super.dataDirectory.resolve("users"));
        this.groups.directory = MoreFiles.createDirectoryIfNotExists(super.dataDirectory.resolve("groups"));
        this.tracks.directory = MoreFiles.createDirectoryIfNotExists(super.dataDirectory.resolve("tracks"));

        // Listen for file changes.
        FileWatcher watcher = this.plugin.getFileWatcher().orElse(null);
        if (watcher != null) {
            this.users.watcher = watcher.getWatcher(this.users.directory);
            this.users.watcher.addListener(path -> {
                String fileName = path.getFileName().toString();
                if (!fileName.endsWith(this.fileExtension)) {
                    return;
                }

                String user = fileName.substring(0, fileName.length() - this.fileExtension.length());
                UUID uuid = Uuids.parse(user);
                if (uuid == null) {
                    return;
                }

                User u = this.plugin.getUserManager().getIfLoaded(uuid);
                if (u != null) {
                    this.plugin.getLogger().info("[FileWatcher] Detected change in user file for " + u.getPlainDisplayName() + " - reloading...");
                    this.plugin.getStorage().loadUser(uuid, null);
                }
            });

            this.groups.watcher = watcher.getWatcher(this.groups.directory);
            this.groups.watcher.addListener(path -> {
                String fileName = path.getFileName().toString();
                if (!fileName.endsWith(this.fileExtension)) {
                    return;
                }

                String groupName = fileName.substring(0, fileName.length() - this.fileExtension.length());
                this.plugin.getLogger().info("[FileWatcher] Detected change in group file for " + groupName + " - reloading...");
                this.plugin.getSyncTaskBuffer().request();
            });

            this.tracks.watcher = watcher.getWatcher(this.tracks.directory);
            this.tracks.watcher.addListener(path -> {
                String fileName = path.getFileName().toString();
                if (!fileName.endsWith(this.fileExtension)) {
                    return;
                }

                String trackName = fileName.substring(0, fileName.length() - this.fileExtension.length());
                this.plugin.getLogger().info("[FileWatcher] Detected change in track file for " + trackName + " - reloading...");
                this.plugin.getStorage().loadAllTracks();
            });
        }
    }

    @Override
    public void applyBulkUpdate(BulkUpdate bulkUpdate) throws Exception {
        if (bulkUpdate.getDataType().isIncludingUsers()) {
            try (Stream<Path> s = Files.list(getDirectory(StorageLocation.USERS))) {
                s.filter(this.fileExtensionFilter).forEach(file -> {
                    try {
                        registerFileAction(StorageLocation.USERS, file);
                        ConfigurationNode object = readFile(file);
                        if (processBulkUpdate(bulkUpdate, object, HolderType.USER)) {
                            saveFile(file, object);
                        }
                    } catch (Exception e) {
                        this.plugin.getLogger().severe(
                                "Exception whilst performing bulkupdate",
                                new FileIOException(file.getFileName().toString(), e)
                        );
                    }
                });
            }
        }

        if (bulkUpdate.getDataType().isIncludingGroups()) {
            try (Stream<Path> s = Files.list(getDirectory(StorageLocation.GROUPS))) {
                s.filter(this.fileExtensionFilter).forEach(file -> {
                    try {
                        registerFileAction(StorageLocation.GROUPS, file);
                        ConfigurationNode object = readFile(file);
                        if (processBulkUpdate(bulkUpdate, object, HolderType.GROUP)) {
                            saveFile(file, object);
                        }
                    } catch (Exception e) {
                        this.plugin.getLogger().severe(
                                "Exception whilst performing bulkupdate",
                                new FileIOException(file.getFileName().toString(), e)
                        );
                    }
                });
            }
        }
    }

    @Override
    public Set<UUID> getUniqueUsers() throws IOException {
        try (Stream<Path> stream = Files.list(this.users.directory)) {
            return stream.filter(this.fileExtensionFilter)
                    .map(p -> p.getFileName().toString())
                    .map(s -> s.substring(0, s.length() - this.fileExtension.length()))
                    .map(Uuids::fromString)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public <N extends Node> List<NodeEntry<UUID, N>> searchUserNodes(ConstraintNodeMatcher<N> constraint) throws IOException {
        List<NodeEntry<UUID, N>> held = new ArrayList<>();
        try (Stream<Path> stream = Files.list(getDirectory(StorageLocation.USERS))) {
            stream.filter(this.fileExtensionFilter)
                    .forEach(file -> {
                        String fileName = file.getFileName().toString();
                        try {
                            registerFileAction(StorageLocation.USERS, file);
                            ConfigurationNode object = readFile(file);
                            UUID holder = UUID.fromString(fileName.substring(0, fileName.length() - this.fileExtension.length()));
                            Set<Node> nodes = readNodes(object);
                            for (Node e : nodes) {
                                N match = constraint.match(e);
                                if (match != null) {
                                    held.add(NodeEntry.of(holder, match));
                                }
                            }
                        } catch (Exception e) {
                            this.plugin.getLogger().severe(
                                    "Exception whilst searching user nodes",
                                    new FileIOException(file.getFileName().toString(), e)
                            );
                        }
                    });
        }
        return held;
    }

    @Override
    public void loadAllGroups() throws IOException {
        List<String> groups;
        try (Stream<Path> stream = Files.list(this.groups.directory)) {
            groups = stream.filter(this.fileExtensionFilter)
                    .map(p -> p.getFileName().toString())
                    .map(s -> s.substring(0, s.length() - this.fileExtension.length()))
                    .collect(Collectors.toList());
        }

        if (!Iterators.tryIterate(groups, this::loadGroup)) {
            throw new RuntimeException("Exception occurred whilst loading a group");
        }

        this.plugin.getGroupManager().retainAll(groups);
    }

    @Override
    public <N extends Node> List<NodeEntry<String, N>> searchGroupNodes(ConstraintNodeMatcher<N> constraint) throws IOException {
        List<NodeEntry<String, N>> held = new ArrayList<>();
        try (Stream<Path> stream = Files.list(getDirectory(StorageLocation.GROUPS))) {
            stream.filter(this.fileExtensionFilter)
                    .forEach(file -> {
                        String fileName = file.getFileName().toString();
                        try {
                            registerFileAction(StorageLocation.GROUPS, file);
                            ConfigurationNode object = readFile(file);
                            String holder = fileName.substring(0, fileName.length() - this.fileExtension.length());
                            Set<Node> nodes = readNodes(object);
                            for (Node e : nodes) {
                                N match = constraint.match(e);
                                if (match != null) {
                                    held.add(NodeEntry.of(holder, match));
                                }
                            }
                        } catch (Exception e) {
                            this.plugin.getLogger().severe(
                                    "Exception whilst searching group nodes",
                                    new FileIOException(file.getFileName().toString(), e)
                            );
                        }
                    });
        }
        return held;
    }

    @Override
    public void loadAllTracks() throws IOException {
        List<String> tracks;
        try (Stream<Path> stream = Files.list(this.tracks.directory)) {
            tracks = stream.filter(this.fileExtensionFilter)
                    .map(p -> p.getFileName().toString())
                    .map(s -> s.substring(0, s.length() - this.fileExtension.length()))
                    .collect(Collectors.toList());
        }

        if (!Iterators.tryIterate(tracks, this::loadTrack)) {
            throw new RuntimeException("Exception occurred whilst loading a track");
        }

        this.plugin.getTrackManager().retainAll(tracks);
    }

}
