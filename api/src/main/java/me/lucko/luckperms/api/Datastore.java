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

import java.util.UUID;

/**
 * Wrapper interface for the internal Datastore instance
 *
 * <p> The implementations of this interface limit access to the datastore and add parameter checks to further prevent
 * errors and ensure all API interactions to not damage the state of the plugin.
 */
@SuppressWarnings("unused")
public interface Datastore {

    String getName();
    boolean isAcceptingLogins();

    Async async();
    Sync sync();

    interface Async {
        void loadOrCreateUser(UUID uuid, String username, Callback<Boolean> callback);
        void loadUser(UUID uuid, Callback<Boolean> callback);
        void saveUser(User user, Callback<Boolean> callback);
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

    interface Sync {
        boolean loadOrCreateUser(UUID uuid, String username);
        boolean loadUser(UUID uuid);
        boolean saveUser(User user);
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
}
