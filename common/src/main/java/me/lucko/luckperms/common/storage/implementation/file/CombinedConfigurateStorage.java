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
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.model.manager.track.TrackManager;
import me.lucko.luckperms.common.node.model.HeldNodeImpl;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.file.loader.ConfigurateLoader;

import net.luckperms.api.node.HeldNode;
import net.luckperms.api.node.Node;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CombinedConfigurateStorage extends AbstractConfigurateStorage {
    private final String fileExtension;

    private Path usersFile;
    private Path groupsFile;
    private Path tracksFile;

    private CachedLoader usersLoader;
    private CachedLoader groupsLoader;
    private CachedLoader tracksLoader;

    private final class CachedLoader {
        private final Path path;

        private final ConfigurationLoader<? extends ConfigurationNode> loader;
        private ConfigurationNode node = null;
        private final ReentrantLock lock = new ReentrantLock();

        private CachedLoader(Path path) {
            this.path = path;
            this.loader = CombinedConfigurateStorage.super.loader.loader(path);
            reload();
        }

        private void recordChange() {
            if (CombinedConfigurateStorage.this.watcher != null) {
                CombinedConfigurateStorage.this.watcher.recordChange(this.path.getFileName().toString());
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

    private FileWatcher.WatchedLocation watcher = null;

    /**
     * Creates a new configurate dao
     * @param plugin the plugin instance
     * @param implementationName the name of this dao
     * @param fileExtension the file extension used by this instance, including a "." at the start
     * @param dataFolderName the name of the folder used to store data
     */
    public CombinedConfigurateStorage(LuckPermsPlugin plugin, String implementationName, ConfigurateLoader loader, String fileExtension, String dataFolderName) {
        super(plugin, implementationName, loader, dataFolderName);
        this.fileExtension = fileExtension;
    }

    @Override
    protected ConfigurationNode readFile(StorageLocation location, String name) throws IOException {
        ConfigurationNode root = getStorageLoader(location).getNode();
        ConfigurationNode ret = root.getNode(name);
        return ret.isVirtual() ? null : ret;
    }

    @Override
    protected void saveFile(StorageLocation location, String name, ConfigurationNode node) throws IOException {
        getStorageLoader(location).apply(true, false, root -> root.getNode(name).setValue(node));
    }

    private CachedLoader getStorageLoader(StorageLocation location) {
        switch (location) {
            case USER:
                return this.usersLoader;
            case GROUP:
                return this.groupsLoader;
            case TRACK:
                return this.tracksLoader;
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public void init() throws IOException {
        super.init();

        this.usersFile = super.dataDirectory.resolve("users" + this.fileExtension);
        this.groupsFile = super.dataDirectory.resolve("groups" + this.fileExtension);
        this.tracksFile = super.dataDirectory.resolve("tracks" + this.fileExtension);

        this.usersLoader = new CachedLoader(this.usersFile);
        this.groupsLoader = new CachedLoader(this.groupsFile);
        this.tracksLoader = new CachedLoader(this.tracksFile);

        // Listen for file changes.
        FileWatcher watcher = this.plugin.getFileWatcher().orElse(null);
        if (watcher != null) {
            this.watcher = watcher.getWatcher(super.dataDirectory);
            this.watcher.addListener(path -> {
                if (path.getFileName().equals(this.usersFile.getFileName())) {
                    this.plugin.getLogger().info("[FileWatcher] Detected change in users file - reloading...");
                    this.usersLoader.reload();
                    this.plugin.getSyncTaskBuffer().request();
                } else if (path.getFileName().equals(this.groupsFile.getFileName())) {
                    this.plugin.getLogger().info("[FileWatcher] Detected change in groups file - reloading...");
                    this.groupsLoader.reload();
                    this.plugin.getSyncTaskBuffer().request();
                } else if (path.getFileName().equals(this.tracksFile.getFileName())) {
                    this.plugin.getLogger().info("[FileWatcher] Detected change in tracks file - reloading...");
                    this.tracksLoader.reload();
                    this.plugin.getStorage().loadAllTracks();
                }
            });
        }
    }

    @Override
    public void shutdown() {
        try {
            this.usersLoader.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            this.groupsLoader.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            this.tracksLoader.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.shutdown();
    }

    @Override
    public void applyBulkUpdate(BulkUpdate bulkUpdate) throws Exception {
        if (bulkUpdate.getDataType().isIncludingUsers()) {
            this.usersLoader.apply(true, true, root -> {
                for (Map.Entry<Object, ? extends ConfigurationNode> entry : root.getChildrenMap().entrySet()) {
                    processBulkUpdate(bulkUpdate, entry.getValue());
                }
            });
        }

        if (bulkUpdate.getDataType().isIncludingGroups()) {
            this.groupsLoader.apply(true, true, root -> {
                for (Map.Entry<Object, ? extends ConfigurationNode> entry : root.getChildrenMap().entrySet()) {
                    processBulkUpdate(bulkUpdate, entry.getValue());
                }
            });
        }
    }

    @Override
    public Set<UUID> getUniqueUsers() throws IOException {
        return this.usersLoader.getNode().getChildrenMap().keySet().stream()
                .map(Object::toString)
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }

    @Override
    public List<HeldNode<UUID>> getUsersWithPermission(Constraint constraint) throws Exception {
        List<HeldNode<UUID>> held = new ArrayList<>();
        this.usersLoader.apply(false, true, root -> {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : root.getChildrenMap().entrySet()) {
                try {
                    UUID holder = UUID.fromString(entry.getKey().toString());
                    ConfigurationNode object = entry.getValue();

                    Set<Node> nodes = readNodes(object);
                    for (Node e : nodes) {
                        if (!constraint.eval(e.getKey())) {
                            continue;
                        }
                        held.add(HeldNodeImpl.of(holder, e));
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

        this.groupsLoader.apply(false, true, root -> {
            groups.addAll(root.getChildrenMap().keySet().stream()
                    .map(Object::toString)
                    .collect(Collectors.toList()));
        });

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
        this.groupsLoader.apply(false, true, root -> {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : root.getChildrenMap().entrySet()) {
                try {
                    String holder = entry.getKey().toString();
                    ConfigurationNode object = entry.getValue();

                    Set<Node> nodes = readNodes(object);
                    for (Node e : nodes) {
                        if (!constraint.eval(e.getKey())) {
                            continue;
                        }
                        held.add(HeldNodeImpl.of(holder, e));
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

        this.tracksLoader.apply(false, true, root -> {
            tracks.addAll(root.getChildrenMap().keySet().stream()
                    .map(Object::toString)
                    .collect(Collectors.toList()));
        });

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
