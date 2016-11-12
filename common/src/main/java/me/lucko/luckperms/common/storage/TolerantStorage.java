/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.common.data.Log;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.tracks.Track;
import me.lucko.luckperms.common.users.User;

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
public class TolerantStorage implements Storage {
    public static TolerantStorage wrap(Storage storage) {
        return new TolerantStorage(storage);
    }

    @Delegate(types = Delegated.class)
    private final Storage backing;

    private final Phaser phaser = new Phaser();

    @Override
    public Storage force() {
        return this;
    }

    @Override
    public void shutdown() {
        // Wait for other threads to finish.
        try {
            phaser.awaitAdvanceInterruptibly(phaser.getPhase(), 5, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }

        backing.shutdown();
    }

    @Override
    public CompletableFuture<Boolean> logAction(LogEntry entry) {
        phaser.register();
        try {
            return backing.logAction(entry);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Log> getLog() {
        phaser.register();
        try {
            return backing.getLog();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> loadUser(UUID uuid, String username) {
        phaser.register();
        try {
            return backing.loadUser(uuid, username);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> saveUser(User user) {
        phaser.register();
        try {
            return backing.saveUser(user);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> cleanupUsers() {
        phaser.register();
        try {
            return backing.cleanupUsers();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Set<UUID>> getUniqueUsers() {
        phaser.register();
        try {
            return backing.getUniqueUsers();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> createAndLoadGroup(String name) {
        phaser.register();
        try {
            return backing.createAndLoadGroup(name);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> loadGroup(String name) {
        phaser.register();
        try {
            return backing.loadGroup(name);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> loadAllGroups() {
        phaser.register();
        try {
            return backing.loadAllGroups();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> saveGroup(Group group) {
        phaser.register();
        try {
            return backing.saveGroup(group);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> deleteGroup(Group group) {
        phaser.register();
        try {
            return backing.deleteGroup(group);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> createAndLoadTrack(String name) {
        phaser.register();
        try {
            return backing.createAndLoadTrack(name);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> loadTrack(String name) {
        phaser.register();
        try {
            return backing.loadTrack(name);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> loadAllTracks() {
        phaser.register();
        try {
            return backing.loadAllTracks();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> saveTrack(Track track) {
        phaser.register();
        try {
            return backing.saveTrack(track);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> deleteTrack(Track track) {
        phaser.register();
        try {
            return backing.deleteTrack(track);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<Boolean> saveUUIDData(String username, UUID uuid) {
        phaser.register();
        try {
            return backing.saveUUIDData(username, uuid);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<UUID> getUUID(String username) {
        phaser.register();
        try {
            return backing.getUUID(username);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public CompletableFuture<String> getName(UUID uuid) {
        phaser.register();
        try {
            return backing.getName(uuid);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    private interface Delegated {
        String getName();
        boolean isAcceptingLogins();
        void setAcceptingLogins(boolean b);
        void init();
    }
}
