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

package me.lucko.luckperms.common.api.delegate;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.Log;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.Storage;
import me.lucko.luckperms.api.Track;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.common.LuckPermsPlugin;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static me.lucko.luckperms.common.api.ApiUtils.checkGroup;
import static me.lucko.luckperms.common.api.ApiUtils.checkName;
import static me.lucko.luckperms.common.api.ApiUtils.checkTrack;
import static me.lucko.luckperms.common.api.ApiUtils.checkUser;
import static me.lucko.luckperms.common.api.ApiUtils.checkUsername;

/**
 * Provides a link between {@link Storage} and {@link me.lucko.luckperms.common.storage.Storage}
 */
@AllArgsConstructor
public class StorageDelegate implements Storage {
    private final LuckPermsPlugin plugin;
    private final me.lucko.luckperms.common.storage.Storage master;

    @Override
    public String getName() {
        return master.getName();
    }

    @Override
    public boolean isAcceptingLogins() {
        return master.isAcceptingLogins();
    }

    @Override
    public Executor getSyncExecutor() {
        return plugin.getSyncExecutor();
    }

    @Override
    public Executor getAsyncExecutor() {
        return plugin.getAsyncExecutor();
    }

    @Override
    public CompletableFuture<Boolean> logAction(@NonNull LogEntry entry) {
        return master.force().logAction(entry);
    }

    @Override
    public CompletableFuture<Log> getLog() {
        return master.force().getLog().thenApply(log -> log == null ? null : new LogDelegate(log));
    }

    @Override
    public CompletableFuture<Boolean> loadUser(UUID uuid, String username) {
        return master.force().loadUser(uuid, checkUsername(username));
    }

    @Override
    public CompletableFuture<Boolean> saveUser(User user) {
        checkUser(user);
        return master.force().saveUser(((UserDelegate) user).getMaster());
    }

    @Override
    public CompletableFuture<Boolean> cleanupUsers() {
        return master.force().cleanupUsers();
    }

    @Override
    public CompletableFuture<Set<UUID>> getUniqueUsers() {
        return master.force().getUniqueUsers();
    }

    @Override
    public CompletableFuture<List<HeldPermission<UUID>>> getUsersWithPermission(@NonNull String permission) {
        return master.force().getUsersWithPermission(permission);
    }

    @Override
    public CompletableFuture<Boolean> createAndLoadGroup(String name) {
        return master.force().createAndLoadGroup(checkName(name));
    }

    @Override
    public CompletableFuture<Boolean> loadGroup(String name) {
        return master.force().loadGroup(checkName(name));
    }

    @Override
    public CompletableFuture<Boolean> loadAllGroups() {
        return master.force().loadAllGroups();
    }

    @Override
    public CompletableFuture<Boolean> saveGroup(Group group) {
        checkGroup(group);
        return master.force().saveGroup(((GroupDelegate) group).getMaster());
    }

    @Override
    public CompletableFuture<Boolean> deleteGroup(Group group) {
        checkGroup(group);
        if (group.getName().equalsIgnoreCase(plugin.getConfiguration().getDefaultGroupName())) {
            throw new IllegalArgumentException("Cannot delete the default group.");
        }
        return master.force().deleteGroup(((GroupDelegate) group).getMaster());
    }

    @Override
    public CompletableFuture<List<HeldPermission<String>>> getGroupsWithPermission(@NonNull String permission) {
        return master.force().getGroupsWithPermission(permission);
    }

    @Override
    public CompletableFuture<Boolean> createAndLoadTrack(String name) {
        return master.force().createAndLoadTrack(checkName(name));
    }

    @Override
    public CompletableFuture<Boolean> loadTrack(String name) {
        return master.force().loadTrack(checkName(name));
    }

    @Override
    public CompletableFuture<Boolean> loadAllTracks() {
        return master.force().loadAllTracks();
    }

    @Override
    public CompletableFuture<Boolean> saveTrack(Track track) {
        checkTrack(track);
        return master.force().saveTrack(((TrackDelegate) track).getMaster());
    }

    @Override
    public CompletableFuture<Boolean> deleteTrack(Track track) {
        checkTrack(track);
        return master.force().deleteTrack(((TrackDelegate) track).getMaster());
    }

    @Override
    public CompletableFuture<Boolean> saveUUIDData(String username, UUID uuid) {
        return master.force().saveUUIDData(checkUsername(username), uuid);
    }

    @Override
    public CompletableFuture<UUID> getUUID(String username) {
        return master.force().getUUID(checkUsername(username));
    }

    @Override
    public CompletableFuture<String> getName(@NonNull UUID uuid) {
        return master.force().getName(uuid);
    }
}
