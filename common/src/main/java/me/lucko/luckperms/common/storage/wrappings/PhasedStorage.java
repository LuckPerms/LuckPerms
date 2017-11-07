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
import lombok.experimental.Delegate;

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

    @Delegate(types = Delegated.class)
    private final Storage delegate;

    private final Phaser phaser = new Phaser();

    @Override
    public Storage noBuffer() {
        return this;
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
    public CompletableFuture<Boolean> logAction(LogEntry entry) {
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
    public CompletableFuture<Boolean> applyBulkUpdate(BulkUpdate bulkUpdate) {
        phaser.register();
        try {
            return delegate.applyBulkUpdate(bulkUpdate);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> loadUser(UUID uuid, String username) {
        phaser.register();
        try {
            return delegate.loadUser(uuid, username);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> saveUser(User user) {
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
    public CompletableFuture<Boolean> createAndLoadGroup(String name, CreationCause cause) {
        phaser.register();
        try {
            return delegate.createAndLoadGroup(name, cause);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> loadGroup(String name) {
        phaser.register();
        try {
            return delegate.loadGroup(name);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> loadAllGroups() {
        phaser.register();
        try {
            return delegate.loadAllGroups();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> saveGroup(Group group) {
        phaser.register();
        try {
            return delegate.saveGroup(group);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> deleteGroup(Group group, DeletionCause cause) {
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
    public CompletableFuture<Boolean> createAndLoadTrack(String name, CreationCause cause) {
        phaser.register();
        try {
            return delegate.createAndLoadTrack(name, cause);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> loadTrack(String name) {
        phaser.register();
        try {
            return delegate.loadTrack(name);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> loadAllTracks() {
        phaser.register();
        try {
            return delegate.loadAllTracks();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> saveTrack(Track track) {
        phaser.register();
        try {
            return delegate.saveTrack(track);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> deleteTrack(Track track, DeletionCause cause) {
        phaser.register();
        try {
            return delegate.deleteTrack(track, cause);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> saveUUIDData(UUID uuid, String username) {
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

    private interface Delegated {
        ApiStorage getDelegate();
        String getName();
        boolean isAcceptingLogins();
        void setAcceptingLogins(boolean b);
        void init();
        Map<String, String> getMeta();
    }
}
