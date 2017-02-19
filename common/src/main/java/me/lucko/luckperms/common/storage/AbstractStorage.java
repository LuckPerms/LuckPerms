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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.api.event.cause.DeletionCause;
import me.lucko.luckperms.common.api.delegates.StorageDelegate;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.Log;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.backing.AbstractBacking;
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
        BufferedOutputStorage bufferedDs = BufferedOutputStorage.wrap(TolerantStorage.wrap(new AbstractStorage(plugin, backing)), 1000L);
        plugin.getScheduler().doAsyncRepeating(bufferedDs, 10L);
        return bufferedDs;
    }

    private final LuckPermsPlugin plugin;

    @Delegate(types = Delegated.class)
    private final AbstractBacking backing;

    @Getter
    private final StorageDelegate delegate;

    private AbstractStorage(LuckPermsPlugin plugin, AbstractBacking backing) {
        this.plugin = plugin;
        this.backing = backing;
        this.delegate = new StorageDelegate(plugin, this);
    }

    private <T> CompletableFuture<T> makeFuture(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, backing.getPlugin().getScheduler().getAsyncExecutor());
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
        return makeFuture(() -> {
            if (backing.loadUser(uuid, username)) {
                plugin.getApiProvider().getEventFactory().handleUserLoad(plugin.getUserManager().get(uuid));
                return true;
            }
            return false;
        });
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
    public CompletableFuture<Boolean> createAndLoadGroup(String name, CreationCause cause) {
        return makeFuture(() -> {
            if (backing.createAndLoadGroup(name)) {
                plugin.getApiProvider().getEventFactory().handleGroupCreate(plugin.getGroupManager().getIfLoaded(name), cause);
                return true;
            }
            return false;
        });
    }

    @Override
    public CompletableFuture<Boolean> loadGroup(String name) {
        return makeFuture(() -> {
            if (backing.loadGroup(name)) {
                plugin.getApiProvider().getEventFactory().handleGroupLoad(plugin.getGroupManager().getIfLoaded(name));
                return true;
            }
            return false;
        });
    }

    @Override
    public CompletableFuture<Boolean> loadAllGroups() {
        return makeFuture(() -> {
            if (backing.loadAllGroups()) {
                plugin.getApiProvider().getEventFactory().handleGroupLoadAll();
                return true;
            }
            return false;
        });
    }

    @Override
    public CompletableFuture<Boolean> saveGroup(Group group) {
        return makeFuture(() -> backing.saveGroup(group));
    }

    @Override
    public CompletableFuture<Boolean> deleteGroup(Group group, DeletionCause cause) {
        return makeFuture(() -> {
            if (backing.deleteGroup(group)) {
                plugin.getApiProvider().getEventFactory().handleGroupDelete(group, cause);
                return true;
            }
            return false;
        });
    }

    @Override
    public CompletableFuture<List<HeldPermission<String>>> getGroupsWithPermission(String permission) {
        return makeFuture(() -> backing.getGroupsWithPermission(permission));
    }

    @Override
    public CompletableFuture<Boolean> createAndLoadTrack(String name, CreationCause cause) {
        return makeFuture(() -> {
            if (backing.createAndLoadTrack(name)) {
                plugin.getApiProvider().getEventFactory().handleTrackCreate(plugin.getTrackManager().getIfLoaded(name), cause);
                return true;
            }
            return false;
        });
    }

    @Override
    public CompletableFuture<Boolean> loadTrack(String name) {
        return makeFuture(() -> {
            if (backing.loadTrack(name)) {
                plugin.getApiProvider().getEventFactory().handleTrackLoad(plugin.getTrackManager().getIfLoaded(name));
                return true;
            }
            return false;
        });
    }

    @Override
    public CompletableFuture<Boolean> loadAllTracks() {
        return makeFuture(() -> {
            if (backing.loadAllTracks()) {
                plugin.getApiProvider().getEventFactory().handleTrackLoadAll();
                return true;
            }
            return false;
        });
    }

    @Override
    public CompletableFuture<Boolean> saveTrack(Track track) {
        return makeFuture(() -> backing.saveTrack(track));
    }

    @Override
    public CompletableFuture<Boolean> deleteTrack(Track track, DeletionCause cause) {
        return makeFuture(() -> {
            if (backing.deleteTrack(track)) {
                plugin.getApiProvider().getEventFactory().handleTrackDelete(track, cause);
                return true;
            }
            return false;
         });
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
