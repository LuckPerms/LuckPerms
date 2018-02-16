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

package me.lucko.luckperms.common.storage;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.api.event.cause.DeletionCause;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.api.delegates.model.ApiStorage;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.dao.AbstractDao;
import me.lucko.luckperms.common.storage.wrappings.BufferedOutputStorage;
import me.lucko.luckperms.common.storage.wrappings.PhasedStorage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Implements {@link Storage} using an {@link AbstractDao}.
 */
public class AbstractStorage implements Storage {
    public static Storage create(LuckPermsPlugin plugin, AbstractDao backing) {
        Storage base = new AbstractStorage(plugin, backing);
        Storage phased = PhasedStorage.wrap(base);
        BufferedOutputStorage buffered = BufferedOutputStorage.wrap(phased, 250L);
        plugin.getScheduler().asyncRepeating(buffered, 2L);
        return buffered;
    }

    private final LuckPermsPlugin plugin;
    private final AbstractDao dao;

    private final ApiStorage apiDelegate;

    private AbstractStorage(LuckPermsPlugin plugin, AbstractDao dao) {
        this.plugin = plugin;
        this.dao = dao;
        this.apiDelegate = new ApiStorage(plugin, this);
    }

    @Override
    public AbstractDao getDao() {
        return this.dao;
    }

    private <T> CompletableFuture<T> makeFuture(Callable<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.call();
            } catch (Exception e) {
                Throwables.propagateIfPossible(e);
                throw new CompletionException(e);
            }
        }, this.dao.getPlugin().getScheduler().async());
    }

    private CompletableFuture<Void> makeFuture(ThrowingRunnable runnable) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                Throwables.propagateIfPossible(e);
                throw new CompletionException(e);
            }
        }, this.dao.getPlugin().getScheduler().async());
    }

    @Override
    public ApiStorage getApiDelegate() {
        return this.apiDelegate;
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @Override
    public String getName() {
        return this.dao.getName();
    }

    @Override
    public Storage noBuffer() {
        return this;
    }

    @Override
    public void init() {
        try {
            this.dao.init();
        } catch (Exception e) {
            this.plugin.getLog().severe("Failed to init storage dao");
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        try {
            this.dao.shutdown();
        } catch (Exception e) {
            this.plugin.getLog().severe("Failed to shutdown storage dao");
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, String> getMeta() {
        return this.dao.getMeta();
    }

    @Override
    public CompletableFuture<Void> logAction(LogEntry entry) {
        return makeFuture(() -> this.dao.logAction(entry));
    }

    @Override
    public CompletableFuture<Log> getLog() {
        return makeFuture(this.dao::getLog);
    }

    @Override
    public CompletableFuture<Void> applyBulkUpdate(BulkUpdate bulkUpdate) {
        return makeFuture(() -> this.dao.applyBulkUpdate(bulkUpdate));
    }

    @Override
    public CompletableFuture<User> loadUser(UUID uuid, String username) {
        return makeFuture(() -> {
            User user = this.dao.loadUser(uuid, username);
            if (user != null) {
                this.plugin.getEventFactory().handleUserLoad(user);
            }
            return user;
        });
    }

    @Override
    public CompletableFuture<Void> saveUser(User user) {
        return makeFuture(() -> this.dao.saveUser(user));
    }

    @Override
    public CompletableFuture<Set<UUID>> getUniqueUsers() {
        return makeFuture(this.dao::getUniqueUsers);
    }

    @Override
    public CompletableFuture<List<HeldPermission<UUID>>> getUsersWithPermission(String permission) {
        return makeFuture(() -> {
            List<HeldPermission<UUID>> result = this.dao.getUsersWithPermission(permission);
            result.removeIf(entry -> entry.asNode().hasExpired());
            return ImmutableList.copyOf(result);
        });
    }

    @Override
    public CompletableFuture<Group> createAndLoadGroup(String name, CreationCause cause) {
        return makeFuture(() -> {
            Group group = this.dao.createAndLoadGroup(name);
            if (group != null) {
                this.plugin.getEventFactory().handleGroupCreate(group, cause);
            }
            return group;
        });
    }

    @Override
    public CompletableFuture<Optional<Group>> loadGroup(String name) {
        return makeFuture(() -> {
            Optional<Group> group = this.dao.loadGroup(name);
            if (group.isPresent()) {
                this.plugin.getEventFactory().handleGroupLoad(group.get());
            }
            return group;
        });
    }

    @Override
    public CompletableFuture<Void> loadAllGroups() {
        return makeFuture(() -> {
            this.dao.loadAllGroups();
            this.plugin.getEventFactory().handleGroupLoadAll();
        });
    }

    @Override
    public CompletableFuture<Void> saveGroup(Group group) {
        return makeFuture(() -> this.dao.saveGroup(group));
    }

    @Override
    public CompletableFuture<Void> deleteGroup(Group group, DeletionCause cause) {
        return makeFuture(() -> {
            this.dao.deleteGroup(group);
            this.plugin.getEventFactory().handleGroupDelete(group, cause);
        });
    }

    @Override
    public CompletableFuture<List<HeldPermission<String>>> getGroupsWithPermission(String permission) {
        return makeFuture(() -> {
            List<HeldPermission<String>> result = this.dao.getGroupsWithPermission(permission);
            result.removeIf(entry -> entry.asNode().hasExpired());
            return ImmutableList.copyOf(result);
        });
    }

    @Override
    public CompletableFuture<Track> createAndLoadTrack(String name, CreationCause cause) {
        return makeFuture(() -> {
            Track track = this.dao.createAndLoadTrack(name);
            if (track != null) {
                this.plugin.getEventFactory().handleTrackCreate(track, cause);
            }
            return track;
        });
    }

    @Override
    public CompletableFuture<Optional<Track>> loadTrack(String name) {
        return makeFuture(() -> {
            Optional<Track> track = this.dao.loadTrack(name);
            if (track.isPresent()) {
                this.plugin.getEventFactory().handleTrackLoad(track.get());
            }
            return track;
        });
    }

    @Override
    public CompletableFuture<Void> loadAllTracks() {
        return makeFuture(() -> {
            this.dao.loadAllTracks();
            this.plugin.getEventFactory().handleTrackLoadAll();
        });
    }

    @Override
    public CompletableFuture<Void> saveTrack(Track track) {
        return makeFuture(() -> this.dao.saveTrack(track));
    }

    @Override
    public CompletableFuture<Void> deleteTrack(Track track, DeletionCause cause) {
        return makeFuture(() -> {
            this.dao.deleteTrack(track);
            this.plugin.getEventFactory().handleTrackDelete(track, cause);
         });
    }

    @Override
    public CompletableFuture<Void> saveUUIDData(UUID uuid, String username) {
        return makeFuture(() -> this.dao.saveUUIDData(uuid, username));
    }

    @Override
    public CompletableFuture<UUID> getUUID(String username) {
        return makeFuture(() -> this.dao.getUUID(username));
    }

    @Override
    public CompletableFuture<String> getName(UUID uuid) {
        return makeFuture(() -> this.dao.getName(uuid));
    }
}
