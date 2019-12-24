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

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.bulkupdate.comparison.Constraint;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.StorageImplementation;
import me.lucko.luckperms.common.storage.implementation.split.SplitStorage;
import me.lucko.luckperms.common.util.ThrowingRunnable;

import net.luckperms.api.actionlog.Action;
import net.luckperms.api.event.cause.CreationCause;
import net.luckperms.api.event.cause.DeletionCause;
import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.node.HeldNode;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Provides a {@link CompletableFuture} based API for interacting with a {@link StorageImplementation}.
 */
public class Storage {
    private final LuckPermsPlugin plugin;
    private final StorageImplementation implementation;

    public Storage(LuckPermsPlugin plugin, StorageImplementation implementation) {
        this.plugin = plugin;
        this.implementation = implementation;
    }

    public StorageImplementation getImplementation() {
        return this.implementation;
    }

    public Collection<StorageImplementation> getImplementations() {
        if (this.implementation instanceof SplitStorage) {
            return ((SplitStorage) this.implementation).getImplementations().values();
        } else {
            return Collections.singleton(this.implementation);
        }
    }

    private <T> CompletableFuture<T> makeFuture(Callable<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.call();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new CompletionException(e);
            }
        }, this.plugin.getBootstrap().getScheduler().async());
    }

    private CompletableFuture<Void> makeFuture(ThrowingRunnable runnable) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new CompletionException(e);
            }
        }, this.plugin.getBootstrap().getScheduler().async());
    }

    public String getName() {
        return this.implementation.getImplementationName();
    }

    public void init() {
        try {
            this.implementation.init();
        } catch (Exception e) {
            this.plugin.getLogger().severe("Failed to init storage implementation");
            e.printStackTrace();
        }
    }

    public void shutdown() {
        try {
            this.implementation.shutdown();
        } catch (Exception e) {
            this.plugin.getLogger().severe("Failed to shutdown storage implementation");
            e.printStackTrace();
        }
    }

    public Map<String, String> getMeta() {
        return this.implementation.getMeta();
    }

    public CompletableFuture<Void> logAction(Action entry) {
        return makeFuture(() -> this.implementation.logAction(entry));
    }

    public CompletableFuture<Log> getLog() {
        return makeFuture(this.implementation::getLog);
    }

    public CompletableFuture<Void> applyBulkUpdate(BulkUpdate bulkUpdate) {
        return makeFuture(() -> this.implementation.applyBulkUpdate(bulkUpdate));
    }

    public CompletableFuture<User> loadUser(UUID uniqueId, String username) {
        return makeFuture(() -> {
            User user = this.implementation.loadUser(uniqueId, username);
            if (user != null) {
                this.plugin.getEventDispatcher().dispatchUserLoad(user);
            }
            return user;
        });
    }

    public CompletableFuture<Void> saveUser(User user) {
        return makeFuture(() -> this.implementation.saveUser(user));
    }

    public CompletableFuture<Set<UUID>> getUniqueUsers() {
        return makeFuture(this.implementation::getUniqueUsers);
    }

    public CompletableFuture<List<HeldNode<UUID>>> getUsersWithPermission(Constraint constraint) {
        return makeFuture(() -> {
            List<HeldNode<UUID>> result = this.implementation.getUsersWithPermission(constraint);
            result.removeIf(entry -> entry.getNode().hasExpired());
            return ImmutableList.copyOf(result);
        });
    }

    public CompletableFuture<Group> createAndLoadGroup(String name, CreationCause cause) {
        return makeFuture(() -> {
            Group group = this.implementation.createAndLoadGroup(name);
            if (group != null) {
                this.plugin.getEventDispatcher().dispatchGroupCreate(group, cause);
            }
            return group;
        });
    }

    public CompletableFuture<Optional<Group>> loadGroup(String name) {
        return makeFuture(() -> {
            Optional<Group> group = this.implementation.loadGroup(name);
            if (group.isPresent()) {
                this.plugin.getEventDispatcher().dispatchGroupLoad(group.get());
            }
            return group;
        });
    }

    public CompletableFuture<Void> loadAllGroups() {
        return makeFuture(() -> {
            this.implementation.loadAllGroups();
            this.plugin.getEventDispatcher().dispatchGroupLoadAll();
        });
    }

    public CompletableFuture<Void> saveGroup(Group group) {
        return makeFuture(() -> this.implementation.saveGroup(group));
    }

    public CompletableFuture<Void> deleteGroup(Group group, DeletionCause cause) {
        return makeFuture(() -> {
            this.implementation.deleteGroup(group);
            this.plugin.getEventDispatcher().dispatchGroupDelete(group, cause);
        });
    }

    public CompletableFuture<List<HeldNode<String>>> getGroupsWithPermission(Constraint constraint) {
        return makeFuture(() -> {
            List<HeldNode<String>> result = this.implementation.getGroupsWithPermission(constraint);
            result.removeIf(entry -> entry.getNode().hasExpired());
            return ImmutableList.copyOf(result);
        });
    }

    public CompletableFuture<Track> createAndLoadTrack(String name, CreationCause cause) {
        return makeFuture(() -> {
            Track track = this.implementation.createAndLoadTrack(name);
            if (track != null) {
                this.plugin.getEventDispatcher().dispatchTrackCreate(track, cause);
            }
            return track;
        });
    }

    public CompletableFuture<Optional<Track>> loadTrack(String name) {
        return makeFuture(() -> {
            Optional<Track> track = this.implementation.loadTrack(name);
            if (track.isPresent()) {
                this.plugin.getEventDispatcher().dispatchTrackLoad(track.get());
            }
            return track;
        });
    }

    public CompletableFuture<Void> loadAllTracks() {
        return makeFuture(() -> {
            this.implementation.loadAllTracks();
            this.plugin.getEventDispatcher().dispatchTrackLoadAll();
        });
    }

    public CompletableFuture<Void> saveTrack(Track track) {
        return makeFuture(() -> this.implementation.saveTrack(track));
    }

    public CompletableFuture<Void> deleteTrack(Track track, DeletionCause cause) {
        return makeFuture(() -> {
            this.implementation.deleteTrack(track);
            this.plugin.getEventDispatcher().dispatchTrackDelete(track, cause);
         });
    }

    public CompletableFuture<PlayerSaveResult> savePlayerData(UUID uniqueId, String username) {
        return makeFuture(() -> {
            PlayerSaveResult result = this.implementation.savePlayerData(uniqueId, username);
            if (result != null) {
                this.plugin.getEventDispatcher().dispatchPlayerDataSave(uniqueId, username, result);
            }
            return result;
        });
    }

    public CompletableFuture<UUID> getPlayerUniqueId(String username) {
        return makeFuture(() -> this.implementation.getPlayerUniqueId(username));
    }

    public CompletableFuture<String> getPlayerName(UUID uniqueId) {
        return makeFuture(() -> this.implementation.getPlayerName(uniqueId));
    }
}
