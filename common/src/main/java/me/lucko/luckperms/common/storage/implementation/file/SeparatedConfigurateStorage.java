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

import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.bulkupdate.comparison.Constraint;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.model.manager.track.TrackManager;
import me.lucko.luckperms.common.node.model.HeldNodeImpl;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.file.loader.ConfigurateLoader;
import me.lucko.luckperms.common.util.MoreFiles;
import me.lucko.luckperms.common.util.Uuids;

import net.luckperms.api.node.HeldNode;
import net.luckperms.api.node.Node;

import ninja.leaping.configurate.ConfigurationNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SeparatedConfigurateStorage extends AbstractConfigurateStorage {
    private final String fileExtension;

    private Path usersDirectory;
    private Path groupsDirectory;
    private Path tracksDirectory;

    private FileWatcher.WatchedLocation userWatcher = null;
    private FileWatcher.WatchedLocation groupWatcher = null;
    private FileWatcher.WatchedLocation trackWatcher = null;

    /**
     * Creates a new configurate storage implementation
     *
     * @param plugin the plugin instance
     * @param implementationName the name of this implementation
     * @param fileExtension the file extension used by this instance, including a "." at the start
     * @param dataFolderName the name of the folder used to store data
     */
    public SeparatedConfigurateStorage(LuckPermsPlugin plugin, String implementationName, ConfigurateLoader loader, String fileExtension, String dataFolderName) {
        super(plugin, implementationName, loader, dataFolderName);
        this.fileExtension = fileExtension;
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

    private Predicate<Path> getFileTypeFilter() {
        return path -> path.getFileName().toString().endsWith(this.fileExtension);
    }

    private void registerFileAction(StorageLocation type, Path file) {
        switch (type) {
            case USER:
                if (this.userWatcher != null) {
                    this.userWatcher.recordChange(file.getFileName().toString());
                }
                break;
            case GROUP:
                if (this.groupWatcher != null) {
                    this.groupWatcher.recordChange(file.getFileName().toString());
                }
                break;
            case TRACK:
                if (this.trackWatcher != null) {
                    this.trackWatcher.recordChange(file.getFileName().toString());
                }
                break;
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public void init() throws IOException {
        super.init();

        this.usersDirectory = MoreFiles.createDirectoryIfNotExists(super.dataDirectory.resolve("users"));
        this.groupsDirectory = MoreFiles.createDirectoryIfNotExists(super.dataDirectory.resolve("groups"));
        this.tracksDirectory = MoreFiles.createDirectoryIfNotExists(super.dataDirectory.resolve("tracks"));

        // Listen for file changes.
        FileWatcher watcher = this.plugin.getFileWatcher().orElse(null);
        if (watcher != null) {
            this.userWatcher = watcher.getWatcher(this.usersDirectory);
            this.userWatcher.addListener(path -> {
                String s = path.getFileName().toString();

                if (!s.endsWith(this.fileExtension)) {
                    return;
                }

                String user = s.substring(0, s.length() - this.fileExtension.length());
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

            this.groupWatcher = watcher.getWatcher(this.groupsDirectory);
            this.groupWatcher.addListener(path -> {
                String s = path.getFileName().toString();

                if (!s.endsWith(this.fileExtension)) {
                    return;
                }

                String groupName = s.substring(0, s.length() - this.fileExtension.length());
                this.plugin.getLogger().info("[FileWatcher] Detected change in group file for " + groupName + " - reloading...");
                this.plugin.getSyncTaskBuffer().request();
            });

            this.trackWatcher = watcher.getWatcher(this.tracksDirectory);
            this.trackWatcher.addListener(path -> {
                String s = path.getFileName().toString();

                if (!s.endsWith(this.fileExtension)) {
                    return;
                }

                String trackName = s.substring(0, s.length() - this.fileExtension.length());
                this.plugin.getLogger().info("[FileWatcher] Detected change in track file for " + trackName + " - reloading...");
                this.plugin.getStorage().loadAllTracks();
            });
        }
    }

    @Override
    public void applyBulkUpdate(BulkUpdate bulkUpdate) throws Exception {
        if (bulkUpdate.getDataType().isIncludingUsers()) {
            try (Stream<Path> s = Files.list(getDirectory(StorageLocation.USER))) {
                s.filter(getFileTypeFilter()).forEach(file -> {
                    try {
                        registerFileAction(StorageLocation.USER, file);
                        ConfigurationNode object = readFile(file);
                        ConfigurationNode results = processBulkUpdate(bulkUpdate, object);
                        if (results != null) {
                            saveFile(file, object);
                        }
                    } catch (Exception e) {
                        throw reportException(file.getFileName().toString(), e);
                    }
                });
            }
        }

        if (bulkUpdate.getDataType().isIncludingGroups()) {
            try (Stream<Path> s = Files.list(getDirectory(StorageLocation.GROUP))) {
                s.filter(getFileTypeFilter()).forEach(file -> {
                    try {
                        registerFileAction(StorageLocation.GROUP, file);
                        ConfigurationNode object = readFile(file);
                        ConfigurationNode results = processBulkUpdate(bulkUpdate, object);
                        if (results != null) {
                            saveFile(file, object);
                        }
                    } catch (Exception e) {
                        throw reportException(file.getFileName().toString(), e);
                    }
                });
            }
        }
    }

    @Override
    public Set<UUID> getUniqueUsers() throws IOException {
        try (Stream<Path> stream = Files.list(this.usersDirectory)) {
            return stream.filter(getFileTypeFilter())
                    .map(p -> p.getFileName().toString())
                    .map(s -> s.substring(0, s.length() - this.fileExtension.length()))
                    .map(UUID::fromString)
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public List<HeldNode<UUID>> getUsersWithPermission(Constraint constraint) throws Exception {
        List<HeldNode<UUID>> held = new ArrayList<>();
        try (Stream<Path> stream = Files.list(getDirectory(StorageLocation.USER))) {
            stream.filter(getFileTypeFilter())
                    .forEach(file -> {
                        String fileName = file.getFileName().toString();
                        try {
                            registerFileAction(StorageLocation.USER, file);
                            ConfigurationNode object = readFile(file);
                            UUID holder = UUID.fromString(fileName.substring(0, fileName.length() - this.fileExtension.length()));
                            Set<Node> nodes = readNodes(object);
                            for (Node e : nodes) {
                                if (!constraint.eval(e.getKey())) {
                                    continue;
                                }
                                held.add(HeldNodeImpl.of(holder, e));
                            }
                        } catch (Exception e) {
                            throw reportException(file.getFileName().toString(), e);
                        }
                    });
        }
        return held;
    }

    @Override
    public void loadAllGroups() throws IOException {
        List<String> groups;
        try (Stream<Path> stream = Files.list(this.groupsDirectory)) {
            groups = stream.filter(getFileTypeFilter())
                    .map(p -> p.getFileName().toString())
                    .map(s -> s.substring(0, s.length() - this.fileExtension.length()))
                    .collect(Collectors.toList());
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

        GroupManager<?> gm = this.plugin.getGroupManager();
        gm.getAll().values().stream()
                .map(Group::getName)
                .filter(g -> !groups.contains(g))
                .forEach(gm::unload);
    }

    @Override
    public List<HeldNode<String>> getGroupsWithPermission(Constraint constraint) throws Exception {
        List<HeldNode<String>> held = new ArrayList<>();
        try (Stream<Path> stream = Files.list(getDirectory(StorageLocation.GROUP))) {
            stream.filter(getFileTypeFilter())
                    .forEach(file -> {
                        String fileName = file.getFileName().toString();
                        try {
                            registerFileAction(StorageLocation.GROUP, file);
                            ConfigurationNode object = readFile(file);
                            String holder = fileName.substring(0, fileName.length() - this.fileExtension.length());
                            Set<Node> nodes = readNodes(object);
                            for (Node e : nodes) {
                                if (!constraint.eval(e.getKey())) {
                                    continue;
                                }
                                held.add(HeldNodeImpl.of(holder, e));
                            }
                        } catch (Exception e) {
                            throw reportException(file.getFileName().toString(), e);
                        }
                    });
        }
        return held;
    }

    @Override
    public void loadAllTracks() throws IOException {
        List<String> tracks;
        try (Stream<Path> stream = Files.list(this.tracksDirectory)) {
            tracks = stream.filter(getFileTypeFilter())
                    .map(p -> p.getFileName().toString())
                    .map(s -> s.substring(0, s.length() - this.fileExtension.length()))
                    .collect(Collectors.toList());
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

        TrackManager<?> tm = this.plugin.getTrackManager();
        tm.getAll().values().stream()
                .map(Track::getName)
                .filter(t -> !tracks.contains(t))
                .forEach(tm::unload);
    }

}
