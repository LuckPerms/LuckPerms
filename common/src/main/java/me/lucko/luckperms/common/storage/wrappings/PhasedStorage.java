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

package me.lucko.luckperms.common.storage.wrappings;

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
import me.lucko.luckperms.common.storage.Storage;
import me.lucko.luckperms.common.storage.dao.AbstractDao;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A storage wrapping that ensures all tasks are completed before
 * {@link Storage#shutdown()} is called.
 */
public class PhasedStorage implements Storage {
    public static PhasedStorage wrap(Storage storage) {
        return new PhasedStorage(storage);
    }

    private final Storage delegate;
    private final Phaser phaser = new Phaser();

    private PhasedStorage(Storage delegate) {
        this.delegate = delegate;
    }

    @Override
    public AbstractDao getDao() {
        return this.delegate.getDao();
    }

    @Override
    public ApiStorage getApiDelegate() {
        return this.delegate.getApiDelegate();
    }

    @Override
    public String getName() {
        return this.delegate.getName();
    }

    @Override
    public Storage noBuffer() {
        return this;
    }

    @Override
    public void init() {
        this.delegate.init();
    }

    @Override
    public void shutdown() {
        // Wait for other threads to finish.
        try {
            this.phaser.awaitAdvanceInterruptibly(this.phaser.getPhase(), 10, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }

        this.delegate.shutdown();
    }

    @Override
    public Map<String, String> getMeta() {
        return this.delegate.getMeta();
    }

    @Override
    public CompletableFuture<Void> logAction(LogEntry entry) {
        this.phaser.register();
        try {
            return this.delegate.logAction(entry);
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Log> getLog() {
        this.phaser.register();
        try {
            return this.delegate.getLog();
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> applyBulkUpdate(BulkUpdate bulkUpdate) {
        this.phaser.register();
        try {
            return this.delegate.applyBulkUpdate(bulkUpdate);
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<User> loadUser(UUID uuid, String username) {
        this.phaser.register();
        try {
            return this.delegate.loadUser(uuid, username);
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> saveUser(User user) {
        this.phaser.register();
        try {
            return this.delegate.saveUser(user);
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Set<UUID>> getUniqueUsers() {
        this.phaser.register();
        try {
            return this.delegate.getUniqueUsers();
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<List<HeldPermission<UUID>>> getUsersWithPermission(String permission) {
        this.phaser.register();
        try {
            return this.delegate.getUsersWithPermission(permission);
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Group> createAndLoadGroup(String name, CreationCause cause) {
        this.phaser.register();
        try {
            return this.delegate.createAndLoadGroup(name, cause);
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Optional<Group>> loadGroup(String name) {
        this.phaser.register();
        try {
            return this.delegate.loadGroup(name);
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> loadAllGroups() {
        this.phaser.register();
        try {
            return this.delegate.loadAllGroups();
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> saveGroup(Group group) {
        this.phaser.register();
        try {
            return this.delegate.saveGroup(group);
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> deleteGroup(Group group, DeletionCause cause) {
        this.phaser.register();
        try {
            return this.delegate.deleteGroup(group, cause);
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<List<HeldPermission<String>>> getGroupsWithPermission(String permission) {
        this.phaser.register();
        try {
            return this.delegate.getGroupsWithPermission(permission);
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Track> createAndLoadTrack(String name, CreationCause cause) {
        this.phaser.register();
        try {
            return this.delegate.createAndLoadTrack(name, cause);
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Optional<Track>> loadTrack(String name) {
        this.phaser.register();
        try {
            return this.delegate.loadTrack(name);
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> loadAllTracks() {
        this.phaser.register();
        try {
            return this.delegate.loadAllTracks();
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> saveTrack(Track track) {
        this.phaser.register();
        try {
            return this.delegate.saveTrack(track);
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> deleteTrack(Track track, DeletionCause cause) {
        this.phaser.register();
        try {
            return this.delegate.deleteTrack(track, cause);
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> saveUUIDData(UUID uuid, String username) {
        this.phaser.register();
        try {
            return this.delegate.saveUUIDData(uuid, username);
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<UUID> getUUID(String username) {
        this.phaser.register();
        try {
            return this.delegate.getUUID(username);
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<String> getName(UUID uuid) {
        this.phaser.register();
        try {
            return this.delegate.getName(uuid);
        } finally {
            this.phaser.arriveAndDeregister();
        }
    }
}
