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

import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.Log;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.Track;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.api.event.cause.DeletionCause;
import me.lucko.luckperms.common.bulkupdate.comparisons.Constraint;
import me.lucko.luckperms.common.bulkupdate.comparisons.StandardComparison;
import me.lucko.luckperms.common.node.factory.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.Storage;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static me.lucko.luckperms.common.api.ApiUtils.checkName;
import static me.lucko.luckperms.common.api.ApiUtils.checkUsername;

public class ApiStorage implements me.lucko.luckperms.api.Storage {
    private static final Function<Throwable, Boolean> CONSUME_EXCEPTION = throwable -> {
        throwable.printStackTrace();
        return false;
    };

    private static Function<Throwable, Boolean> consumeExceptionToFalse() {
        return CONSUME_EXCEPTION;
    }

    private static <T> Function<Throwable, T> consumeExceptionToNull() {
        return throwable -> {
            throwable.printStackTrace();
            return null;
        };
    }
    
    private final LuckPermsPlugin plugin;
    private final Storage handle;
    
    public ApiStorage(LuckPermsPlugin plugin, Storage handle) {
        this.plugin = plugin;
        this.handle = handle;
    }

    @Override
    public @NonNull String getName() {
        return this.handle.getName();
    }

    @Override
    public boolean isAcceptingLogins() {
        return true;
    }

    @Override
    public @NonNull Executor getSyncExecutor() {
        return this.plugin.getBootstrap().getScheduler().sync();
    }

    @Override
    public @NonNull Executor getAsyncExecutor() {
        return this.plugin.getBootstrap().getScheduler().async();
    }

    @Override
    public @NonNull CompletableFuture<Boolean> logAction(@NonNull LogEntry entry) {
        Objects.requireNonNull(entry, "entry");
        return this.handle.logAction(entry)
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Override
    public @NonNull CompletableFuture<Log> getLog() {
        return this.handle.getLog().<Log>thenApply(ApiLog::new).exceptionally(consumeExceptionToNull());
    }

    @Override
    public @NonNull CompletableFuture<Boolean> loadUser(@NonNull UUID uuid, String username) {
        Objects.requireNonNull(uuid, "uuid");
        username = checkUsername(username);

        if (this.plugin.getUserManager().getIfLoaded(uuid) == null) {
            this.plugin.getUserManager().getHouseKeeper().registerApiUsage(uuid);
        }

        return this.handle.loadUser(uuid, username)
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Override
    public @NonNull CompletableFuture<Boolean> saveUser(@NonNull User user) {
        Objects.requireNonNull(user, "user");
        return this.handle.saveUser(ApiUser.cast(user))
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Override
    public @NonNull CompletableFuture<Set<UUID>> getUniqueUsers() {
        return this.handle.getUniqueUsers().exceptionally(consumeExceptionToNull());
    }

    @Override
    public @NonNull CompletableFuture<List<HeldPermission<UUID>>> getUsersWithPermission(@NonNull String permission) {
        Objects.requireNonNull(permission, "permission");
        return this.handle.getUsersWithPermission(Constraint.of(StandardComparison.EQUAL, permission)).exceptionally(consumeExceptionToNull());
    }

    @Override
    public @NonNull CompletableFuture<Boolean> createAndLoadGroup(@NonNull String name) {
        Objects.requireNonNull(name, "name");
        return this.handle.createAndLoadGroup(checkName(name), CreationCause.API)
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Override
    public @NonNull CompletableFuture<Boolean> loadGroup(@NonNull String name) {
        Objects.requireNonNull(name, "name");
        return this.handle.loadGroup(checkName(name))
                .thenApply(Optional::isPresent)
                .exceptionally(consumeExceptionToFalse());
    }

    @Override
    public @NonNull CompletableFuture<Boolean> loadAllGroups() {
        return this.handle.loadAllGroups()
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Override
    public @NonNull CompletableFuture<Boolean> saveGroup(@NonNull Group group) {
        Objects.requireNonNull(group, "group");
        return this.handle.saveGroup(ApiGroup.cast(group))
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Override
    public @NonNull CompletableFuture<Boolean> deleteGroup(@NonNull Group group) {
        Objects.requireNonNull(group, "group");
        if (group.getName().equalsIgnoreCase(NodeFactory.DEFAULT_GROUP_NAME)) {
            throw new IllegalArgumentException("Cannot delete the default group.");
        }
        return this.handle.deleteGroup(ApiGroup.cast(group), DeletionCause.API)
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Override
    public @NonNull CompletableFuture<List<HeldPermission<String>>> getGroupsWithPermission(@NonNull String permission) {
        Objects.requireNonNull(permission, "permission");
        return this.handle.getGroupsWithPermission(Constraint.of(StandardComparison.EQUAL, permission)).exceptionally(consumeExceptionToNull());
    }

    @Override
    public @NonNull CompletableFuture<Boolean> createAndLoadTrack(@NonNull String name) {
        Objects.requireNonNull(name, "name");
        return this.handle.createAndLoadTrack(checkName(name), CreationCause.API)
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Override
    public @NonNull CompletableFuture<Boolean> loadTrack(@NonNull String name) {
        Objects.requireNonNull(name, "name");
        return this.handle.loadTrack(checkName(name))
                .thenApply(Optional::isPresent)
                .exceptionally(consumeExceptionToFalse());
    }

    @Override
    public @NonNull CompletableFuture<Boolean> loadAllTracks() {
        return this.handle.loadAllTracks()
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Override
    public @NonNull CompletableFuture<Boolean> saveTrack(@NonNull Track track) {
        Objects.requireNonNull(track, "track");
        return this.handle.saveTrack(ApiTrack.cast(track))
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Override
    public @NonNull CompletableFuture<Boolean> deleteTrack(@NonNull Track track) {
        Objects.requireNonNull(track, "track");
        return this.handle.deleteTrack(ApiTrack.cast(track), DeletionCause.API)
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Override
    public @NonNull CompletableFuture<Boolean> saveUUIDData(@NonNull String username, @NonNull UUID uuid) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(uuid, "uuid");
        return this.handle.savePlayerData(uuid, checkUsername(username))
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Override
    public @NonNull CompletableFuture<UUID> getUUID(@NonNull String username) {
        Objects.requireNonNull(username, "username");
        return this.handle.getPlayerUuid(checkUsername(username));
    }

    @Override
    public @NonNull CompletableFuture<String> getName(@NonNull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return this.handle.getPlayerName(uuid);
    }
}
