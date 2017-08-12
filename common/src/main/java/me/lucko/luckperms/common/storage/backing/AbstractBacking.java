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

package me.lucko.luckperms.common.storage.backing;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.data.Log;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractBacking {

    @Getter
    protected final LuckPermsPlugin plugin;

    @Getter
    public final String name;

    @Getter
    @Setter
    private boolean acceptingLogins = false;

    public abstract void init();

    public abstract void shutdown();

    public Map<String, String> getMeta() {
        return Collections.emptyMap();
    }

    public abstract boolean logAction(LogEntry entry);

    public abstract Log getLog();

    public abstract boolean applyBulkUpdate(BulkUpdate bulkUpdate);

    public abstract boolean loadUser(UUID uuid, String username);

    public abstract boolean saveUser(User user);

    public abstract boolean cleanupUsers();

    public abstract Set<UUID> getUniqueUsers();

    public abstract List<HeldPermission<UUID>> getUsersWithPermission(String permission);

    public abstract boolean createAndLoadGroup(String name);

    public abstract boolean loadGroup(String name);

    public abstract boolean loadAllGroups();

    public abstract boolean saveGroup(Group group);

    public abstract boolean deleteGroup(Group group);

    public abstract List<HeldPermission<String>> getGroupsWithPermission(String permission);

    public abstract boolean createAndLoadTrack(String name);

    public abstract boolean loadTrack(String name);

    public abstract boolean loadAllTracks();

    public abstract boolean saveTrack(Track track);

    public abstract boolean deleteTrack(Track track);

    public abstract boolean saveUUIDData(String username, UUID uuid);

    public abstract UUID getUUID(String username);

    public abstract String getName(UUID uuid);

}
