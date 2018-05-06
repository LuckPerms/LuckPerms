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

package me.lucko.luckperms.common.storage.dao;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.PlayerSaveResult;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.bulkupdate.comparisons.Constraint;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

public abstract class AbstractDao {

    protected final LuckPermsPlugin plugin;
    public final String name;

    protected AbstractDao(LuckPermsPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    public String getName() {
        return this.name;
    }

    public abstract void init() throws Exception;

    public abstract void shutdown();

    public Map<String, String> getMeta() {
        return Collections.emptyMap();
    }

    public abstract void logAction(LogEntry entry) throws Exception;

    public abstract Log getLog() throws Exception;

    public abstract void applyBulkUpdate(BulkUpdate bulkUpdate) throws Exception;

    public abstract User loadUser(UUID uuid, String username) throws Exception;

    public abstract void saveUser(User user) throws Exception;

    public abstract Set<UUID> getUniqueUsers() throws Exception;

    public abstract List<HeldPermission<UUID>> getUsersWithPermission(Constraint constraint) throws Exception;

    public abstract Group createAndLoadGroup(String name) throws Exception;

    public abstract Optional<Group> loadGroup(String name) throws Exception;

    public abstract void loadAllGroups() throws Exception;

    public abstract void saveGroup(Group group) throws Exception;

    public abstract void deleteGroup(Group group) throws Exception;

    public abstract List<HeldPermission<String>> getGroupsWithPermission(Constraint constraint) throws Exception;

    public abstract Track createAndLoadTrack(String name) throws Exception;

    public abstract Optional<Track> loadTrack(String name) throws Exception;

    public abstract void loadAllTracks() throws Exception;

    public abstract void saveTrack(Track track) throws Exception;

    public abstract void deleteTrack(Track track) throws Exception;

    public abstract PlayerSaveResult savePlayerData(UUID uuid, String username) throws Exception;

    @Nullable
    public abstract UUID getPlayerUuid(String username) throws Exception;

    @Nullable
    public abstract String getPlayerName(UUID uuid) throws Exception;

}
