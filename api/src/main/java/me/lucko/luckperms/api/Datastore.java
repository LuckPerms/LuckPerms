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

package me.lucko.luckperms.api;

import me.lucko.luckperms.api.data.Callback;

import java.util.Set;
import java.util.UUID;

/**
 * Interface for the internal Datastore instance
 *
 * @deprecated as of version 2.14 in favour of {@link Storage}.
 */
@Deprecated
public interface Datastore {

    String getName();

    boolean isAcceptingLogins();

    @Deprecated
    Sync sync();

    @Deprecated
    Async async();

    @Deprecated
    Future future();

    @Deprecated
    interface Sync {
        boolean logAction(LogEntry entry);

        Log getLog();

        @Deprecated
        boolean loadOrCreateUser(UUID uuid, String username);

        @Deprecated
        boolean loadUser(UUID uuid);

        boolean loadUser(UUID uuid, String username);

        boolean saveUser(User user);

        boolean cleanupUsers();

        Set<UUID> getUniqueUsers();

        boolean createAndLoadGroup(String name);

        boolean loadGroup(String name);

        boolean loadAllGroups();

        boolean saveGroup(Group group);

        boolean deleteGroup(Group group);

        boolean createAndLoadTrack(String name);

        boolean loadTrack(String name);

        boolean loadAllTracks();

        boolean saveTrack(Track track);

        boolean deleteTrack(Track track);

        boolean saveUUIDData(String username, UUID uuid);

        UUID getUUID(String username);
    }

    @Deprecated
    interface Async {
        void logAction(LogEntry entry, Callback<Boolean> callback);

        void getLog(Callback<Log> callback);

        @Deprecated
        void loadOrCreateUser(UUID uuid, String username, Callback<Boolean> callback);

        @Deprecated
        void loadUser(UUID uuid, Callback<Boolean> callback);

        void loadUser(UUID uuid, String username, Callback<Boolean> callback);

        void saveUser(User user, Callback<Boolean> callback);

        void cleanupUsers(Callback<Boolean> callback);

        void getUniqueUsers(Callback<Set<UUID>> callback);

        void createAndLoadGroup(String name, Callback<Boolean> callback);

        void loadGroup(String name, Callback<Boolean> callback);

        void loadAllGroups(Callback<Boolean> callback);

        void saveGroup(Group group, Callback<Boolean> callback);

        void deleteGroup(Group group, Callback<Boolean> callback);

        void createAndLoadTrack(String name, Callback<Boolean> callback);

        void loadTrack(String name, Callback<Boolean> callback);

        void loadAllTracks(Callback<Boolean> callback);

        void saveTrack(Track track, Callback<Boolean> callback);

        void deleteTrack(Track track, Callback<Boolean> callback);

        void saveUUIDData(String username, UUID uuid, Callback<Boolean> callback);

        void getUUID(String username, Callback<UUID> callback);
    }

    @Deprecated
    interface Future {
        java.util.concurrent.Future<Boolean> logAction(LogEntry entry);

        java.util.concurrent.Future<Log> getLog();

        @Deprecated
        java.util.concurrent.Future<Boolean> loadOrCreateUser(UUID uuid, String username);

        @Deprecated
        java.util.concurrent.Future<Boolean> loadUser(UUID uuid);

        java.util.concurrent.Future<Boolean> loadUser(UUID uuid, String username);

        java.util.concurrent.Future<Boolean> saveUser(User user);

        java.util.concurrent.Future<Boolean> cleanupUsers();

        java.util.concurrent.Future<Set<UUID>> getUniqueUsers();

        java.util.concurrent.Future<Boolean> createAndLoadGroup(String name);

        java.util.concurrent.Future<Boolean> loadGroup(String name);

        java.util.concurrent.Future<Boolean> loadAllGroups();

        java.util.concurrent.Future<Boolean> saveGroup(Group group);

        java.util.concurrent.Future<Boolean> deleteGroup(Group group);

        java.util.concurrent.Future<Boolean> createAndLoadTrack(String name);

        java.util.concurrent.Future<Boolean> loadTrack(String name);

        java.util.concurrent.Future<Boolean> loadAllTracks();

        java.util.concurrent.Future<Boolean> saveTrack(Track track);

        java.util.concurrent.Future<Boolean> deleteTrack(Track track);

        java.util.concurrent.Future<Boolean> saveUUIDData(String username, UUID uuid);

        java.util.concurrent.Future<UUID> getUUID(String username);
    }
}
