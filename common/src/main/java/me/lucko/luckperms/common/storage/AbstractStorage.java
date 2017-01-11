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
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.Log;
import me.lucko.luckperms.common.storage.backing.AbstractBacking;
import me.lucko.luckperms.common.storage.holder.HeldPermission;
import me.lucko.luckperms.common.storage.wrappings.BufferedOutputStorage;
import me.lucko.luckperms.common.storage.wrappings.TolerantStorage;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Converts a {@link AbstractBacking} to use {@link CompletableFuture}s
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AbstractStorage implements Storage {
    public static Storage wrap(LuckPermsPlugin plugin, AbstractBacking backing) {
        BufferedOutputStorage bufferedDs = BufferedOutputStorage.wrap(TolerantStorage.wrap(new AbstractStorage(backing)), 1000L);
        plugin.doAsyncRepeating(bufferedDs, 10L);
        return bufferedDs;
    }

    @Delegate(types = Delegated.class)
    private final AbstractBacking backing;

    private <T> CompletableFuture<T> makeFuture(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, backing.getPlugin().getAsyncExecutor());
    }

    @Override
    public Storage force() {
        return this;
    }

    @Override
    public CompletableFuture<Boolean> logAction(LogEntry entry) {
        return makeFuture(() -> backing.logAction(entry));
    }

    @Override
    public CompletableFuture<Log> getLog() {
        return makeFuture(backing::getLog);
    }

    @Override
    public CompletableFuture<Boolean> loadUser(UUID uuid, String username) {
        return makeFuture(() -> backing.loadUser(uuid, username));
    }

    @Override
    public CompletableFuture<Boolean> saveUser(User user) {
        return makeFuture(() -> backing.saveUser(user));
    }

    @Override
    public CompletableFuture<Boolean> cleanupUsers() {
        return makeFuture(backing::cleanupUsers);
    }

    @Override
    public CompletableFuture<Set<UUID>> getUniqueUsers() {
        return makeFuture(backing::getUniqueUsers);
    }

    @Override
    public CompletableFuture<List<HeldPermission<UUID>>> getUsersWithPermission(String permission) {
        return makeFuture(() -> backing.getUsersWithPermission(permission));
    }

    @Override
    public CompletableFuture<Boolean> createAndLoadGroup(String name) {
        return makeFuture(() -> backing.createAndLoadGroup(name));
    }

    @Override
    public CompletableFuture<Boolean> loadGroup(String name) {
        return makeFuture(() -> backing.loadGroup(name));
    }

    @Override
    public CompletableFuture<Boolean> loadAllGroups() {
        return makeFuture(backing::loadAllGroups);
    }

    @Override
    public CompletableFuture<Boolean> saveGroup(Group group) {
        return makeFuture(() -> backing.saveGroup(group));
    }

    @Override
    public CompletableFuture<Boolean> deleteGroup(Group group) {
        return makeFuture(() -> backing.deleteGroup(group));
    }

    @Override
    public CompletableFuture<List<HeldPermission<String>>> getGroupsWithPermission(String permission) {
        return makeFuture(() -> backing.getGroupsWithPermission(permission));
    }

    @Override
    public CompletableFuture<Boolean> createAndLoadTrack(String name) {
        return makeFuture(() -> backing.createAndLoadTrack(name));
    }

    @Override
    public CompletableFuture<Boolean> loadTrack(String name) {
        return makeFuture(() -> backing.loadTrack(name));
    }

    @Override
    public CompletableFuture<Boolean> loadAllTracks() {
        return makeFuture(backing::loadAllTracks);
    }

    @Override
    public CompletableFuture<Boolean> saveTrack(Track track) {
        return makeFuture(() -> backing.saveTrack(track));
    }

    @Override
    public CompletableFuture<Boolean> deleteTrack(Track track) {
        return makeFuture(() -> backing.deleteTrack(track));
    }

    @Override
    public CompletableFuture<Boolean> saveUUIDData(String username, UUID uuid) {
        return makeFuture(() -> backing.saveUUIDData(username, uuid));
    }

    @Override
    public CompletableFuture<UUID> getUUID(String username) {
        return makeFuture(() -> backing.getUUID(username));
    }

    @Override
    public CompletableFuture<String> getName(UUID uuid) {
        return makeFuture(() -> backing.getName(uuid));
    }

    private interface Delegated {
        String getName();
        boolean isAcceptingLogins();
        void setAcceptingLogins(boolean b);
        void init();
        void shutdown();
    }
}
