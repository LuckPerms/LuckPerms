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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

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
 * A Datastore wrapping that ensures all tasks are completed before {@link Storage#shutdown()} is called.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PhasedStorage implements Storage {
    public static PhasedStorage wrap(Storage storage) {
        return new PhasedStorage(storage);
    }

    private final Storage delegate;

    private final Phaser phaser = new Phaser();

    @Override
    public ApiStorage getDelegate() {
        return delegate.getDelegate();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Storage noBuffer() {
        return this;
    }

    @Override
    public void init() {
        delegate.init();
    }

    @Override
    public void shutdown() {
        // Wait for other threads to finish.
        try {
            phaser.awaitAdvanceInterruptibly(phaser.getPhase(), 10, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }

        delegate.shutdown();
    }

    @Override
    public Map<String, String> getMeta() {
        return delegate.getMeta();
    }

    @Override
    public CompletableFuture<Void> logAction(LogEntry entry) {
        phaser.register();
        try {
            return delegate.logAction(entry);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Log> getLog() {
        phaser.register();
        try {
            return delegate.getLog();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> applyBulkUpdate(BulkUpdate bulkUpdate) {
        phaser.register();
        try {
            return delegate.applyBulkUpdate(bulkUpdate);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<User> loadUser(UUID uuid, String username) {
        phaser.register();
        try {
            return delegate.loadUser(uuid, username);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> saveUser(User user) {
        phaser.register();
        try {
            return delegate.saveUser(user);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Set<UUID>> getUniqueUsers() {
        phaser.register();
        try {
            return delegate.getUniqueUsers();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<List<HeldPermission<UUID>>> getUsersWithPermission(String permission) {
        phaser.register();
        try {
            return delegate.getUsersWithPermission(permission);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Group> createAndLoadGroup(String name, CreationCause cause) {
        phaser.register();
        try {
            return delegate.createAndLoadGroup(name, cause);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Optional<Group>> loadGroup(String name) {
        phaser.register();
        try {
            return delegate.loadGroup(name);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> loadAllGroups() {
        phaser.register();
        try {
            return delegate.loadAllGroups();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> saveGroup(Group group) {
        phaser.register();
        try {
            return delegate.saveGroup(group);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> deleteGroup(Group group, DeletionCause cause) {
        phaser.register();
        try {
            return delegate.deleteGroup(group, cause);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<List<HeldPermission<String>>> getGroupsWithPermission(String permission) {
        phaser.register();
        try {
            return delegate.getGroupsWithPermission(permission);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Track> createAndLoadTrack(String name, CreationCause cause) {
        phaser.register();
        try {
            return delegate.createAndLoadTrack(name, cause);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Optional<Track>> loadTrack(String name) {
        phaser.register();
        try {
            return delegate.loadTrack(name);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> loadAllTracks() {
        phaser.register();
        try {
            return delegate.loadAllTracks();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> saveTrack(Track track) {
        phaser.register();
        try {
            return delegate.saveTrack(track);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> deleteTrack(Track track, DeletionCause cause) {
        phaser.register();
        try {
            return delegate.deleteTrack(track, cause);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Void> saveUUIDData(UUID uuid, String username) {
        phaser.register();
        try {
            return delegate.saveUUIDData(uuid, username);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<UUID> getUUID(String username) {
        phaser.register();
        try {
            return delegate.getUUID(username);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<String> getName(UUID uuid) {
        phaser.register();
        try {
            return delegate.getName(uuid);
        } finally {
            phaser.arriveAndDeregister();
        }
    }
}
