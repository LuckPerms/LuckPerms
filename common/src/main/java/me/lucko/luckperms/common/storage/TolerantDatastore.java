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
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.common.data.Log;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.tracks.Track;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.common.utils.LPFuture;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Phaser;

/**
 * A Datastore wrapping that ensures all tasks are completed before {@link Datastore#shutdown()} is called.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TolerantDatastore implements Datastore {
    public static TolerantDatastore wrap(Datastore datastore) {
        return new TolerantDatastore(datastore);
    }

    private final Datastore backing;
    private final Phaser phaser = new Phaser();

    @Override
    public String getName() {
        return backing.getName();
    }

    @Override
    public boolean isAcceptingLogins() {
        return backing.isAcceptingLogins();
    }

    @Override
    public void setAcceptingLogins(boolean acceptingLogins) {
        backing.setAcceptingLogins(acceptingLogins);
    }

    @Override
    public void doAsync(Runnable r) {
        backing.doAsync(r);
    }

    @Override
    public void doSync(Runnable r) {
        backing.doSync(r);
    }

    @Override
    public LPFuture<Void> init() {
        phaser.register();
        try {
            return backing.init();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<Void> shutdown() {
        phaser.register(); // Register self
        phaser.arriveAndAwaitAdvance(); // Wait for other threads to finish.

        return backing.shutdown();
    }

    @Override
    public LPFuture<Boolean> logAction(LogEntry entry) {
        phaser.register();
        try {
            return backing.logAction(entry);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<Log> getLog() {
        phaser.register();
        try {
            return backing.getLog();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<Boolean> loadUser(UUID uuid, String username) {
        phaser.register();
        try {
            return backing.loadUser(uuid, username);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<Boolean> saveUser(User user) {
        phaser.register();
        try {
            return backing.saveUser(user);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<Boolean> cleanupUsers() {
        phaser.register();
        try {
            return backing.cleanupUsers();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<Set<UUID>> getUniqueUsers() {
        phaser.register();
        try {
            return backing.getUniqueUsers();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<Boolean> createAndLoadGroup(String name) {
        phaser.register();
        try {
            return backing.createAndLoadGroup(name);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<Boolean> loadGroup(String name) {
        phaser.register();
        try {
            return backing.loadGroup(name);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<Boolean> loadAllGroups() {
        phaser.register();
        try {
            return backing.loadAllGroups();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<Boolean> saveGroup(Group group) {
        phaser.register();
        try {
            return backing.saveGroup(group);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<Boolean> deleteGroup(Group group) {
        phaser.register();
        try {
            return backing.deleteGroup(group);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<Boolean> createAndLoadTrack(String name) {
        phaser.register();
        try {
            return backing.createAndLoadTrack(name);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<Boolean> loadTrack(String name) {
        phaser.register();
        try {
            return backing.loadTrack(name);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<Boolean> loadAllTracks() {
        phaser.register();
        try {
            return backing.loadAllTracks();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<Boolean> saveTrack(Track track) {
        phaser.register();
        try {
            return backing.saveTrack(track);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<Boolean> deleteTrack(Track track) {
        phaser.register();
        try {
            return backing.deleteTrack(track);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<Boolean> saveUUIDData(String username, UUID uuid) {
        phaser.register();
        try {
            return backing.saveUUIDData(username, uuid);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<UUID> getUUID(String username) {
        phaser.register();
        try {
            return backing.getUUID(username);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public LPFuture<String> getName(UUID uuid) {
        phaser.register();
        try {
            return backing.getName(uuid);
        } finally {
            phaser.arriveAndDeregister();
        }
    }
}
