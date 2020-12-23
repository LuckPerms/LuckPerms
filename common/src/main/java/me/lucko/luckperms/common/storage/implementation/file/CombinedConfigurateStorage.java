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
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.node.matcher.ConstraintNodeMatcher;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.file.loader.ConfigurateLoader;
import me.lucko.luckperms.common.storage.implementation.file.watcher.FileWatcher;
import me.lucko.luckperms.common.storage.misc.NodeEntry;
import me.lucko.luckperms.common.util.Iterators;
import me.lucko.luckperms.common.util.Uuids;

import net.luckperms.api.node.Node;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Flat-file storage using Configurate {@link ConfigurationNode}s.
 * The data for users/groups/tracks is stored in a single shared file.
 */
public class CombinedConfigurateStorage extends AbstractConfigurateStorage {
    private final String fileExtension;

    private CachedLoader users;
    private CachedLoader groups;
    private CachedLoader tracks;
    private FileWatcher.WatchedLocation watcher = null;

    public CombinedConfigurateStorage(LuckPermsPlugin plugin, String implementationName, ConfigurateLoader loader, String fileExtension, String dataFolderName) {
        super(plugin, implementationName, loader, dataFolderName);
        this.fileExtension = fileExtension;
    }

    @Override
    protected ConfigurationNode readFile(StorageLocation location, String name) throws IOException {
        ConfigurationNode root = getLoader(location).getNode();
        ConfigurationNode node = root.getNode(name);
        return node.isVirtual() ? null : node;
    }

    @Override
    protected void saveFile(StorageLocation location, String name, ConfigurationNode node) throws IOException {
        getLoader(location).apply(true, false, root -> root.getNode(name).setValue(node));
    }

    private CachedLoader getLoader(StorageLocation location) {
        switch (location) {
            case USERS:
                return this.users;
            case GROUPS:
                return this.groups;
            case TRACKS:
                return this.tracks;
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public void init() throws IOException {
        super.init();

        this.users = new CachedLoader(super.dataDirectory.resolve("users" + this.fileExtension));
        this.groups = new CachedLoader(super.dataDirectory.resolve("groups" + this.fileExtension));
        this.tracks = new CachedLoader(super.dataDirectory.resolve("tracks" + this.fileExtension));

        // Listen for file changes.
        FileWatcher watcher = this.plugin.getFileWatcher().orElse(null);
        if (watcher != null) {
            this.watcher = watcher.getWatcher(super.dataDirectory);
            this.watcher.addListener(path -> {
                if (path.getFileName().equals(this.users.file.getFileName())) {
                    this.plugin.getLogger().info("[FileWatcher] Detected change in users file - reloading...");
                    this.users.reload();
                    this.plugin.getSyncTaskBuffer().request();
                } else if (path.getFileName().equals(this.groups.file.getFileName())) {
                    this.plugin.getLogger().info("[FileWatcher] Detected change in groups file - reloading...");
                    this.groups.reload();
                    this.plugin.getSyncTaskBuffer().request();
                } else if (path.getFileName().equals(this.tracks.file.getFileName())) {
                    this.plugin.getLogger().info("[FileWatcher] Detected change in tracks file - reloading...");
                    this.tracks.reload();
                    this.plugin.getStorage().loadAllTracks();
                }
            });
        }
    }

    @Override
    public void shutdown() {
        try {
            this.users.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            this.groups.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            this.tracks.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.shutdown();
    }

    @Override
    public void applyBulkUpdate(BulkUpdate bulkUpdate) throws Exception {
        if (bulkUpdate.getDataType().isIncludingUsers()) {
            this.users.apply(true, true, root -> {
                for (Map.Entry<Object, ? extends ConfigurationNode> entry : root.getChildrenMap().entrySet()) {
                    processBulkUpdate(bulkUpdate, entry.getValue(), HolderType.USER);
                }
            });
        }

        if (bulkUpdate.getDataType().isIncludingGroups()) {
            this.groups.apply(true, true, root -> {
                for (Map.Entry<Object, ? extends ConfigurationNode> entry : root.getChildrenMap().entrySet()) {
                    processBulkUpdate(bulkUpdate, entry.getValue(), HolderType.GROUP);
                }
            });
        }
    }

    @Override
    public Set<UUID> getUniqueUsers() throws IOException {
        return this.users.getNode().getChildrenMap().keySet().stream()
                .map(Object::toString)
                .map(Uuids::fromString)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public <N extends Node> List<NodeEntry<UUID, N>> searchUserNodes(ConstraintNodeMatcher<N> constraint) throws Exception {
        List<NodeEntry<UUID, N>> held = new ArrayList<>();
        this.users.apply(false, true, root -> {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : root.getChildrenMap().entrySet()) {
                try {
                    UUID holder = UUID.fromString(entry.getKey().toString());
                    ConfigurationNode object = entry.getValue();

                    Set<Node> nodes = readNodes(object);
                    for (Node e : nodes) {
                        N match = constraint.match(e);
                        if (match != null) {
                            held.add(NodeEntry.of(holder, match));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return held;
    }

    @Override
    public void loadAllGroups() throws IOException {
        List<String> groups = new ArrayList<>();
        this.groups.apply(false, true, root -> {
            groups.addAll(root.getChildrenMap().keySet().stream()
                    .map(Object::toString)
                    .collect(Collectors.toList()));
        });

        if (!Iterators.tryIterate(groups, this::loadGroup)) {
            throw new RuntimeException("Exception occurred whilst loading a group");
        }

        this.plugin.getGroupManager().retainAll(groups);
    }

    @Override
    public <N extends Node> List<NodeEntry<String, N>> searchGroupNodes(ConstraintNodeMatcher<N> constraint) throws Exception {
        List<NodeEntry<String, N>> held = new ArrayList<>();
        this.groups.apply(false, true, root -> {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : root.getChildrenMap().entrySet()) {
                try {
                    String holder = entry.getKey().toString();
                    ConfigurationNode object = entry.getValue();

                    Set<Node> nodes = readNodes(object);
                    for (Node e : nodes) {
                        N match = constraint.match(e);
                        if (match != null) {
                            held.add(NodeEntry.of(holder, match));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return held;
    }

    @Override
    public void loadAllTracks() throws IOException {
        List<String> tracks = new ArrayList<>();
        this.tracks.apply(false, true, root -> {
            tracks.addAll(root.getChildrenMap().keySet().stream()
                    .map(Object::toString)
                    .collect(Collectors.toList()));
        });

        if (!Iterators.tryIterate(tracks, this::loadTrack)) {
            throw new RuntimeException("Exception occurred whilst loading a track");
        }

        this.plugin.getTrackManager().retainAll(tracks);
    }

    private final class CachedLoader {
        private final Path file;
        private final ConfigurationLoader<? extends ConfigurationNode> loader;
        private final ReentrantLock lock = new ReentrantLock();
        private ConfigurationNode node = null;

        private CachedLoader(Path file) {
            this.file = file;
            this.loader = CombinedConfigurateStorage.super.loader.loader(file);
            reload();
        }

        private void recordChange() {
            if (CombinedConfigurateStorage.this.watcher != null) {
                CombinedConfigurateStorage.this.watcher.recordChange(this.file.getFileName().toString());
            }
        }

        public ConfigurationNode getNode() throws IOException {
            this.lock.lock();
            try {
                if (this.node == null) {
                    this.node = this.loader.load();
                }

                return this.node;
            } finally {
                this.lock.unlock();
            }
        }

        public void apply(Consumer<ConfigurationNode> action) throws IOException {
            apply(false, false, action);
        }

        public void apply(boolean save, boolean reload, Consumer<ConfigurationNode> action) throws IOException {
            this.lock.lock();
            try {
                if (this.node == null || reload) {
                    reload();
                }

                action.accept(this.node);

                if (save) {
                    save();
                }
            } finally {
                this.lock.unlock();
            }
        }

        public void save() throws IOException {
            this.lock.lock();
            try {
                recordChange();
                this.loader.save(this.node);
            } finally {
                this.lock.unlock();
            }
        }

        public void reload() {
            this.lock.lock();
            try {
                this.node = null;
                try {
                    recordChange();
                    this.node = this.loader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } finally {
                this.lock.unlock();
            }
        }
    }

}
