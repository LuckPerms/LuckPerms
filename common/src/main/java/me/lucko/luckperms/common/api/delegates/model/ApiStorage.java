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
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.Storage;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import javax.annotation.Nonnull;

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

    @Nonnull
    @Override
    public String getName() {
        return this.handle.getName();
    }

    @Override
    public boolean isAcceptingLogins() {
        return true;
    }

    @Nonnull
    @Override
    public Executor getSyncExecutor() {
        return this.plugin.getScheduler().sync();
    }

    @Nonnull
    @Override
    public Executor getAsyncExecutor() {
        return this.plugin.getScheduler().async();
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> logAction(@Nonnull LogEntry entry) {
        Objects.requireNonNull(entry, "entry");
        return this.handle.noBuffer().logAction(entry)
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Nonnull
    @Override
    public CompletableFuture<Log> getLog() {
        return this.handle.noBuffer().getLog().<Log>thenApply(ApiLog::new).exceptionally(consumeExceptionToNull());
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> loadUser(@Nonnull UUID uuid, String username) {
        Objects.requireNonNull(uuid, "uuid");
        username = checkUsername(username);

        if (this.plugin.getUserManager().getIfLoaded(uuid) == null) {
            this.plugin.getUserManager().getHouseKeeper().registerApiUsage(uuid);
        }

        return this.handle.noBuffer().loadUser(uuid, username)
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> saveUser(@Nonnull User user) {
        Objects.requireNonNull(user, "user");
        return this.handle.noBuffer().saveUser(ApiUser.cast(user))
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Nonnull
    @Override
    public CompletableFuture<Set<UUID>> getUniqueUsers() {
        return this.handle.noBuffer().getUniqueUsers().exceptionally(consumeExceptionToNull());
    }

    @Nonnull
    @Override
    public CompletableFuture<List<HeldPermission<UUID>>> getUsersWithPermission(@Nonnull String permission) {
        Objects.requireNonNull(permission, "permission");
        return this.handle.noBuffer().getUsersWithPermission(permission).exceptionally(consumeExceptionToNull());
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> createAndLoadGroup(@Nonnull String name) {
        Objects.requireNonNull(name, "name");
        return this.handle.noBuffer().createAndLoadGroup(checkName(name), CreationCause.API)
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> loadGroup(@Nonnull String name) {
        Objects.requireNonNull(name, "name");
        return this.handle.noBuffer().loadGroup(checkName(name))
                .thenApply(Optional::isPresent)
                .exceptionally(consumeExceptionToFalse());
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> loadAllGroups() {
        return this.handle.noBuffer().loadAllGroups()
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> saveGroup(@Nonnull Group group) {
        Objects.requireNonNull(group, "group");
        return this.handle.noBuffer().saveGroup(ApiGroup.cast(group))
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> deleteGroup(@Nonnull Group group) {
        Objects.requireNonNull(group, "group");
        if (group.getName().equalsIgnoreCase(NodeFactory.DEFAULT_GROUP_NAME)) {
            throw new IllegalArgumentException("Cannot delete the default group.");
        }
        return this.handle.noBuffer().deleteGroup(ApiGroup.cast(group), DeletionCause.API)
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Nonnull
    @Override
    public CompletableFuture<List<HeldPermission<String>>> getGroupsWithPermission(@Nonnull String permission) {
        Objects.requireNonNull(permission, "permission");
        return this.handle.noBuffer().getGroupsWithPermission(permission).exceptionally(consumeExceptionToNull());
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> createAndLoadTrack(@Nonnull String name) {
        Objects.requireNonNull(name, "name");
        return this.handle.noBuffer().createAndLoadTrack(checkName(name), CreationCause.API)
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> loadTrack(@Nonnull String name) {
        Objects.requireNonNull(name, "name");
        return this.handle.noBuffer().loadTrack(checkName(name))
                .thenApply(Optional::isPresent)
                .exceptionally(consumeExceptionToFalse());
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> loadAllTracks() {
        return this.handle.noBuffer().loadAllTracks()
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> saveTrack(@Nonnull Track track) {
        Objects.requireNonNull(track, "track");
        return this.handle.noBuffer().saveTrack(ApiTrack.cast(track))
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> deleteTrack(@Nonnull Track track) {
        Objects.requireNonNull(track, "track");
        return this.handle.noBuffer().deleteTrack(ApiTrack.cast(track), DeletionCause.API)
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> saveUUIDData(@Nonnull String username, @Nonnull UUID uuid) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(uuid, "uuid");
        return this.handle.noBuffer().saveUUIDData(uuid, checkUsername(username))
                .thenApply(r -> true)
                .exceptionally(consumeExceptionToFalse());
    }

    @Nonnull
    @Override
    public CompletableFuture<UUID> getUUID(@Nonnull String username) {
        Objects.requireNonNull(username, "username");
        return this.handle.noBuffer().getUUID(checkUsername(username));
    }

    @Nonnull
    @Override
    public CompletableFuture<String> getName(@Nonnull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return this.handle.noBuffer().getName(uuid);
    }
}
