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

package me.lucko.luckperms.common.storage;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.api.event.cause.DeletionCause;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.api.delegates.StorageDelegate;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Main interface for all Storage providers.
 */
public interface Storage {

    StorageDelegate getDelegate();

    String getName();

    boolean isAcceptingLogins();

    void setAcceptingLogins(boolean acceptingLogins);

    Storage noBuffer();

    void init();

    void shutdown();

    Map<String, String> getMeta();

    CompletableFuture<Boolean> logAction(LogEntry entry);

    CompletableFuture<Log> getLog();

    CompletableFuture<Boolean> applyBulkUpdate(BulkUpdate bulkUpdate);

    CompletableFuture<Boolean> loadUser(UUID uuid, String username);

    CompletableFuture<Boolean> saveUser(User user);

    CompletableFuture<Boolean> cleanupUsers();

    CompletableFuture<Set<UUID>> getUniqueUsers();

    CompletableFuture<List<HeldPermission<UUID>>> getUsersWithPermission(String permission);

    CompletableFuture<Boolean> createAndLoadGroup(String name, CreationCause cause);

    CompletableFuture<Boolean> loadGroup(String name);

    CompletableFuture<Boolean> loadAllGroups();

    CompletableFuture<Boolean> saveGroup(Group group);

    CompletableFuture<Boolean> deleteGroup(Group group, DeletionCause cause);

    CompletableFuture<List<HeldPermission<String>>> getGroupsWithPermission(String permission);

    CompletableFuture<Boolean> createAndLoadTrack(String name, CreationCause cause);

    CompletableFuture<Boolean> loadTrack(String name);

    CompletableFuture<Boolean> loadAllTracks();

    CompletableFuture<Boolean> saveTrack(Track track);

    CompletableFuture<Boolean> deleteTrack(Track track, DeletionCause cause);

    CompletableFuture<Boolean> saveUUIDData(String username, UUID uuid);

    CompletableFuture<UUID> getUUID(String username);

    CompletableFuture<String> getName(UUID uuid);
}
