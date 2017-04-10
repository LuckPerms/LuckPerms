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

package me.lucko.luckperms.common.api.delegates;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.Log;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.Storage;
import me.lucko.luckperms.api.Track;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.api.event.cause.DeletionCause;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static me.lucko.luckperms.common.api.ApiUtils.checkName;
import static me.lucko.luckperms.common.api.ApiUtils.checkUsername;

/**
 * Provides a link between {@link Storage} and {@link me.lucko.luckperms.common.storage.Storage}
 */
@AllArgsConstructor
public class StorageDelegate implements Storage {
    private final LuckPermsPlugin plugin;
    private final me.lucko.luckperms.common.storage.Storage handle;

    @Override
    public String getName() {
        return handle.getName();
    }

    @Override
    public boolean isAcceptingLogins() {
        return handle.isAcceptingLogins();
    }

    @Override
    public Executor getSyncExecutor() {
        return plugin.getScheduler().getSyncExecutor();
    }

    @Override
    public Executor getAsyncExecutor() {
        return plugin.getScheduler().getAsyncExecutor();
    }

    @Override
    public CompletableFuture<Boolean> logAction(@NonNull LogEntry entry) {
        return handle.force().logAction(entry);
    }

    @Override
    public CompletableFuture<Log> getLog() {
        return handle.force().getLog().thenApply(log -> log == null ? null : new LogDelegate(log));
    }

    @Override
    public CompletableFuture<Boolean> loadUser(UUID uuid, String username) {
        return handle.force().loadUser(uuid, checkUsername(username));
    }

    @Override
    public CompletableFuture<Boolean> saveUser(User user) {
        return handle.force().saveUser(UserDelegate.cast(user));
    }

    @Override
    public CompletableFuture<Boolean> cleanupUsers() {
        return handle.force().cleanupUsers();
    }

    @Override
    public CompletableFuture<Set<UUID>> getUniqueUsers() {
        return handle.force().getUniqueUsers();
    }

    @Override
    public CompletableFuture<List<HeldPermission<UUID>>> getUsersWithPermission(@NonNull String permission) {
        return handle.force().getUsersWithPermission(permission);
    }

    @Override
    public CompletableFuture<Boolean> createAndLoadGroup(String name) {
        return handle.force().createAndLoadGroup(checkName(name), CreationCause.API);
    }

    @Override
    public CompletableFuture<Boolean> loadGroup(String name) {
        return handle.force().loadGroup(checkName(name));
    }

    @Override
    public CompletableFuture<Boolean> loadAllGroups() {
        return handle.force().loadAllGroups();
    }

    @Override
    public CompletableFuture<Boolean> saveGroup(Group group) {
        return handle.force().saveGroup(GroupDelegate.cast(group));
    }

    @Override
    public CompletableFuture<Boolean> deleteGroup(Group group) {
        if (group.getName().equalsIgnoreCase(plugin.getConfiguration().get(ConfigKeys.DEFAULT_GROUP_NAME))) {
            throw new IllegalArgumentException("Cannot delete the default group.");
        }
        return handle.force().deleteGroup(GroupDelegate.cast(group), DeletionCause.API);
    }

    @Override
    public CompletableFuture<List<HeldPermission<String>>> getGroupsWithPermission(@NonNull String permission) {
        return handle.force().getGroupsWithPermission(permission);
    }

    @Override
    public CompletableFuture<Boolean> createAndLoadTrack(String name) {
        return handle.force().createAndLoadTrack(checkName(name), CreationCause.API);
    }

    @Override
    public CompletableFuture<Boolean> loadTrack(String name) {
        return handle.force().loadTrack(checkName(name));
    }

    @Override
    public CompletableFuture<Boolean> loadAllTracks() {
        return handle.force().loadAllTracks();
    }

    @Override
    public CompletableFuture<Boolean> saveTrack(Track track) {
        return handle.force().saveTrack(TrackDelegate.cast(track));
    }

    @Override
    public CompletableFuture<Boolean> deleteTrack(Track track) {
        return handle.force().deleteTrack(TrackDelegate.cast(track), DeletionCause.API);
    }

    @Override
    public CompletableFuture<Boolean> saveUUIDData(String username, UUID uuid) {
        return handle.force().saveUUIDData(checkUsername(username), uuid);
    }

    @Override
    public CompletableFuture<UUID> getUUID(String username) {
        return handle.force().getUUID(checkUsername(username));
    }

    @Override
    public CompletableFuture<String> getName(@NonNull UUID uuid) {
        return handle.force().getName(uuid);
    }
}
