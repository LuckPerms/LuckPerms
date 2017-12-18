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

package me.lucko.luckperms.common.api.delegates.model;

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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static me.lucko.luckperms.common.api.ApiUtils.checkName;
import static me.lucko.luckperms.common.api.ApiUtils.checkUsername;

@AllArgsConstructor
public class ApiStorage implements Storage {
    private final LuckPermsPlugin plugin;
    private final me.lucko.luckperms.common.storage.Storage handle;

    @Override
    public String getName() {
        return handle.getName();
    }

    @Override
    public boolean isAcceptingLogins() {
        return true;
    }

    @Override
    public Executor getSyncExecutor() {
        return plugin.getScheduler().sync();
    }

    @Override
    public Executor getAsyncExecutor() {
        return plugin.getScheduler().async();
    }

    @Override
    public CompletableFuture<Boolean> logAction(@NonNull LogEntry entry) {
        return handle.noBuffer().logAction(entry).thenApply(x -> true);
    }

    @Override
    public CompletableFuture<Log> getLog() {
        return handle.noBuffer().getLog().thenApply(log -> log == null ? null : new ApiLog(log));
    }

    @Override
    public CompletableFuture<Boolean> loadUser(@NonNull UUID uuid, String username) {
        return handle.noBuffer().loadUser(uuid, username == null ? null : checkUsername(username)).thenApply(Objects::nonNull);
    }

    @Override
    public CompletableFuture<Boolean> saveUser(@NonNull User user) {
        return handle.noBuffer().saveUser(ApiUser.cast(user)).thenApply(x -> true);
    }

    @Override
    public CompletableFuture<Set<UUID>> getUniqueUsers() {
        return handle.noBuffer().getUniqueUsers();
    }

    @Override
    public CompletableFuture<List<HeldPermission<UUID>>> getUsersWithPermission(@NonNull String permission) {
        return handle.noBuffer().getUsersWithPermission(permission);
    }

    @Override
    public CompletableFuture<Boolean> createAndLoadGroup(@NonNull String name) {
        return handle.noBuffer().createAndLoadGroup(checkName(name), CreationCause.API).thenApply(Objects::nonNull);
    }

    @Override
    public CompletableFuture<Boolean> loadGroup(@NonNull String name) {
        return handle.noBuffer().loadGroup(checkName(name)).thenApply(Optional::isPresent);
    }

    @Override
    public CompletableFuture<Boolean> loadAllGroups() {
        return handle.noBuffer().loadAllGroups().thenApply(x -> true);
    }

    @Override
    public CompletableFuture<Boolean> saveGroup(@NonNull Group group) {
        return handle.noBuffer().saveGroup(ApiGroup.cast(group)).thenApply(x -> true);
    }

    @Override
    public CompletableFuture<Boolean> deleteGroup(@NonNull Group group) {
        if (group.getName().equalsIgnoreCase(plugin.getConfiguration().get(ConfigKeys.DEFAULT_GROUP_NAME))) {
            throw new IllegalArgumentException("Cannot delete the default group.");
        }
        return handle.noBuffer().deleteGroup(ApiGroup.cast(group), DeletionCause.API).thenApply(x -> true);
    }

    @Override
    public CompletableFuture<List<HeldPermission<String>>> getGroupsWithPermission(@NonNull String permission) {
        return handle.noBuffer().getGroupsWithPermission(permission);
    }

    @Override
    public CompletableFuture<Boolean> createAndLoadTrack(@NonNull String name) {
        return handle.noBuffer().createAndLoadTrack(checkName(name), CreationCause.API).thenApply(Objects::nonNull);
    }

    @Override
    public CompletableFuture<Boolean> loadTrack(@NonNull String name) {
        return handle.noBuffer().loadTrack(checkName(name)).thenApply(Optional::isPresent);
    }

    @Override
    public CompletableFuture<Boolean> loadAllTracks() {
        return handle.noBuffer().loadAllTracks().thenApply(x -> true);
    }

    @Override
    public CompletableFuture<Boolean> saveTrack(@NonNull Track track) {
        return handle.noBuffer().saveTrack(ApiTrack.cast(track)).thenApply(x -> true);
    }

    @Override
    public CompletableFuture<Boolean> deleteTrack(@NonNull Track track) {
        return handle.noBuffer().deleteTrack(ApiTrack.cast(track), DeletionCause.API).thenApply(x -> true);
    }

    @Override
    public CompletableFuture<Boolean> saveUUIDData(@NonNull String username, @NonNull UUID uuid) {
        return handle.noBuffer().saveUUIDData(uuid, checkUsername(username)).thenApply(x -> true);
    }

    @Override
    public CompletableFuture<UUID> getUUID(@NonNull String username) {
        return handle.noBuffer().getUUID(checkUsername(username));
    }

    @Override
    public CompletableFuture<String> getName(@NonNull UUID uuid) {
        return handle.noBuffer().getName(uuid);
    }
}
