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
import me.lucko.luckperms.common.buffers.Buffer;
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

/**
 * A storage wrapping that passes save tasks through a buffer
 */
public class BufferedOutputStorage implements Storage, Runnable {
    public static BufferedOutputStorage wrap(Storage storage, long flushTime) {
        return new BufferedOutputStorage(storage, flushTime);
    }

    private final Storage delegate;

    private final long flushTime;

    private final Buffer<User, Void> userOutputBuffer = Buffer.of(user -> BufferedOutputStorage.this.delegate.saveUser(user).join());
    private final Buffer<Group, Void> groupOutputBuffer = Buffer.of(group -> BufferedOutputStorage.this.delegate.saveGroup(group).join());
    private final Buffer<Track, Void> trackOutputBuffer = Buffer.of(track -> BufferedOutputStorage.this.delegate.saveTrack(track).join());

    private BufferedOutputStorage(Storage delegate, long flushTime) {
        this.delegate = delegate;
        this.flushTime = flushTime;
    }

    @Override
    public void run() {
        flush(this.flushTime);
    }

    public void forceFlush() {
        flush(-1);
    }

    public void flush(long flushTime) {
        this.userOutputBuffer.flush(flushTime);
        this.groupOutputBuffer.flush(flushTime);
        this.trackOutputBuffer.flush(flushTime);
    }

    @Override
    public Storage noBuffer() {
        return this.delegate;
    }

    @Override
    public void shutdown() {
        forceFlush();
        this.delegate.shutdown();
    }

    @Override
    public CompletableFuture<Void> saveUser(User user) {
        return this.userOutputBuffer.enqueue(user);
    }

    @Override
    public CompletableFuture<Void> saveGroup(Group group) {
        return this.groupOutputBuffer.enqueue(group);
    }

    @Override
    public CompletableFuture<Void> saveTrack(Track track) {
        return this.trackOutputBuffer.enqueue(track);
    }

    // delegate

    @Override
    public AbstractDao getDao() {
        return this.delegate.getDao();
    }

    @Override
    public CompletableFuture<Set<UUID>> getUniqueUsers() {
        return this.delegate.getUniqueUsers();
    }

    @Override
    public CompletableFuture<Optional<Track>> loadTrack(String name) {
        return this.delegate.loadTrack(name);
    }

    @Override
    public CompletableFuture<List<HeldPermission<UUID>>> getUsersWithPermission(String permission) {
        return this.delegate.getUsersWithPermission(permission);
    }

    @Override
    public CompletableFuture<List<HeldPermission<String>>> getGroupsWithPermission(String permission) {
        return this.delegate.getGroupsWithPermission(permission);
    }

    @Override
    public CompletableFuture<Void> applyBulkUpdate(BulkUpdate bulkUpdate) {
        return this.delegate.applyBulkUpdate(bulkUpdate);
    }

    @Override
    public CompletableFuture<Void> saveUUIDData(UUID uuid, String username) {
        return this.delegate.saveUUIDData(uuid, username);
    }

    @Override
    public CompletableFuture<Group> createAndLoadGroup(String name, CreationCause cause) {
        return this.delegate.createAndLoadGroup(name, cause);
    }

    @Override
    public Map<String, String> getMeta() {
        return this.delegate.getMeta();
    }

    @Override
    public CompletableFuture<Void> deleteGroup(Group group, DeletionCause cause) {
        return this.delegate.deleteGroup(group, cause);
    }

    @Override
    public CompletableFuture<UUID> getUUID(String username) {
        return this.delegate.getUUID(username);
    }

    @Override
    public CompletableFuture<User> loadUser(UUID uuid, String username) {
        return this.delegate.loadUser(uuid, username);
    }

    @Override
    public CompletableFuture<Track> createAndLoadTrack(String name, CreationCause cause) {
        return this.delegate.createAndLoadTrack(name, cause);
    }

    @Override
    public CompletableFuture<Log> getLog() {
        return this.delegate.getLog();
    }

    @Override
    public ApiStorage getApiDelegate() {
        return this.delegate.getApiDelegate();
    }

    @Override
    public CompletableFuture<String> getName(UUID uuid) {
        return this.delegate.getName(uuid);
    }

    @Override
    public String getName() {
        return this.delegate.getName();
    }

    @Override
    public CompletableFuture<Void> loadAllTracks() {
        return this.delegate.loadAllTracks();
    }

    @Override
    public void init() {
        this.delegate.init();
    }

    @Override
    public CompletableFuture<Void> deleteTrack(Track track, DeletionCause cause) {
        return this.delegate.deleteTrack(track, cause);
    }

    @Override
    public CompletableFuture<Void> logAction(LogEntry entry) {
        return this.delegate.logAction(entry);
    }

    @Override
    public CompletableFuture<Void> loadAllGroups() {
        return this.delegate.loadAllGroups();
    }

    @Override
    public CompletableFuture<Optional<Group>> loadGroup(String name) {
        return this.delegate.loadGroup(name);
    }

}
