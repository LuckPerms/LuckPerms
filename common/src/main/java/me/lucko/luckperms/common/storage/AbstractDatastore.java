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
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.data.Log;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.storage.backing.AbstractBacking;
import me.lucko.luckperms.common.tracks.Track;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.common.utils.AbstractFuture;
import me.lucko.luckperms.common.utils.LPFuture;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Converts a {@link AbstractBacking} to use {@link Future}s
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AbstractDatastore implements Datastore {

    public static Datastore wrap(LuckPermsPlugin plugin, AbstractBacking backing) {
        BufferedOutputDatastore bufferedDs = BufferedOutputDatastore.wrap(TolerantDatastore.wrap(new AbstractDatastore(backing)), 1000L);
        plugin.doAsyncRepeating(bufferedDs, 10L);
        return bufferedDs;
    }

    private final AbstractBacking backing;

    private <T> LPFuture<T> makeFuture(Supplier<T> supplier) {
        AbstractFuture<T> future = new AbstractFuture<>();
        backing.doAsync(() -> {
            T result = supplier.get();
            future.complete(result);
        });
        return future;
    }

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
    public Datastore force() {
        return this;
    }

    @Override
    public void init() {
        backing.init();
    }

    @Override
    public void shutdown() {
        backing.shutdown();
    }

    @Override
    public LPFuture<Boolean> logAction(LogEntry entry) {
        return makeFuture(() -> backing.logAction(entry));
    }

    @Override
    public LPFuture<Log> getLog() {
        return makeFuture(backing::getLog);
    }

    @Override
    public LPFuture<Boolean> loadUser(UUID uuid, String username) {
        return makeFuture(() -> backing.loadUser(uuid, username));
    }

    @Override
    public LPFuture<Boolean> saveUser(User user) {
        return makeFuture(() -> backing.saveUser(user));
    }

    @Override
    public LPFuture<Boolean> cleanupUsers() {
        return makeFuture(backing::cleanupUsers);
    }

    @Override
    public LPFuture<Set<UUID>> getUniqueUsers() {
        return makeFuture(backing::getUniqueUsers);
    }

    @Override
    public LPFuture<Boolean> createAndLoadGroup(String name) {
        return makeFuture(() -> backing.createAndLoadGroup(name));
    }

    @Override
    public LPFuture<Boolean> loadGroup(String name) {
        return makeFuture(() -> backing.loadGroup(name));
    }

    @Override
    public LPFuture<Boolean> loadAllGroups() {
        return makeFuture(backing::loadAllGroups);
    }

    @Override
    public LPFuture<Boolean> saveGroup(Group group) {
        return makeFuture(() -> backing.saveGroup(group));
    }

    @Override
    public LPFuture<Boolean> deleteGroup(Group group) {
        return makeFuture(() -> backing.deleteGroup(group));
    }

    @Override
    public LPFuture<Boolean> createAndLoadTrack(String name) {
        return makeFuture(() -> backing.createAndLoadTrack(name));
    }

    @Override
    public LPFuture<Boolean> loadTrack(String name) {
        return makeFuture(() -> backing.loadTrack(name));
    }

    @Override
    public LPFuture<Boolean> loadAllTracks() {
        return makeFuture(backing::loadAllTracks);
    }

    @Override
    public LPFuture<Boolean> saveTrack(Track track) {
        return makeFuture(() -> backing.saveTrack(track));
    }

    @Override
    public LPFuture<Boolean> deleteTrack(Track track) {
        return makeFuture(() -> backing.deleteTrack(track));
    }

    @Override
    public LPFuture<Boolean> saveUUIDData(String username, UUID uuid) {
        return makeFuture(() -> backing.saveUUIDData(username, uuid));
    }

    @Override
    public LPFuture<UUID> getUUID(String username) {
        return makeFuture(() -> backing.getUUID(username));
    }

    @Override
    public LPFuture<String> getName(UUID uuid) {
        return makeFuture(() -> backing.getName(uuid));
    }
}
