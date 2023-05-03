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

package me.lucko.luckperms.common.storage.implementation.split;

import com.google.common.collect.ImmutableMap;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.matcher.ConstraintNodeMatcher;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.StorageType;
import me.lucko.luckperms.common.storage.implementation.StorageImplementation;
import me.lucko.luckperms.common.storage.misc.NodeEntry;
import net.kyori.adventure.text.Component;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.node.Node;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SplitStorage implements StorageImplementation {
    private final LuckPermsPlugin plugin;
    private final Map<StorageType, StorageImplementation> implementations;
    private final Map<SplitStorageType, StorageType> types;
    
    public SplitStorage(LuckPermsPlugin plugin, Map<StorageType, StorageImplementation> implementations, Map<SplitStorageType, StorageType> types) {
        this.plugin = plugin;
        this.implementations = ImmutableMap.copyOf(implementations);
        this.types = ImmutableMap.copyOf(types);
    }

    public Map<StorageType, StorageImplementation> getImplementations() {
        return this.implementations;
    }

    private StorageImplementation implFor(SplitStorageType type) {
        return this.implementations.get(this.types.get(type));
    }

    @Override
    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public String getImplementationName() {
        return "Split Storage";
    }

    @Override
    public void init() {
        boolean failed = false;
        for (StorageImplementation ds : this.implementations.values()) {
            try {
                ds.init();
            } catch (Exception ex) {
                failed = true;
                ex.printStackTrace();
            }
        }
        if (failed) {
            throw new RuntimeException("One of the backings failed to init");
        }
    }

    @Override
    public void shutdown() {
        for (StorageImplementation ds : this.implementations.values()) {
            try {
                ds.shutdown();
            } catch (Exception e) {
                this.plugin.getLogger().severe("Exception whilst disabling " + ds + " storage", e);
            }
        }
    }

    @Override
    public Map<Component, Component> getMeta() {
        Map<Component, Component> meta = new LinkedHashMap<>();
        meta.put(
                Component.translatable("luckperms.command.info.storage.meta.split-types-key"),
                Message.formatStringList(this.types.entrySet().stream()
                        .map(e -> e.getKey().toString().toLowerCase(Locale.ROOT) + "->" + e.getValue().getName().toLowerCase(Locale.ROOT))
                        .collect(Collectors.toList()))
        );
        for (StorageImplementation backing : this.implementations.values()) {
            meta.putAll(backing.getMeta());
        }
        return meta;
    }

    @Override
    public void logAction(Action entry) throws Exception {
        implFor(SplitStorageType.LOG).logAction(entry);
    }

    @Override
    public Log getLog() throws Exception {
        return implFor(SplitStorageType.LOG).getLog();
    }

    @Override
    public void applyBulkUpdate(BulkUpdate bulkUpdate) throws Exception {
        StorageType userType = this.types.get(SplitStorageType.USER);
        StorageType groupType = this.types.get(SplitStorageType.GROUP);

        this.implementations.get(userType).applyBulkUpdate(bulkUpdate);

        // if differs
        if (userType != groupType) {
            this.implementations.get(groupType).applyBulkUpdate(bulkUpdate);
        }
    }

    @Override
    public User loadUser(UUID uniqueId, String username) throws Exception {
        return implFor(SplitStorageType.USER).loadUser(uniqueId, username);
    }

    @Override
    public Map<UUID, User> loadUsers(Set<UUID> uniqueIds) throws Exception {
        return implFor(SplitStorageType.USER).loadUsers(uniqueIds);
    }

    @Override
    public void saveUser(User user) throws Exception {
        implFor(SplitStorageType.USER).saveUser(user);
    }

    @Override
    public Set<UUID> getUniqueUsers() throws Exception {
        return implFor(SplitStorageType.USER).getUniqueUsers();
    }

    @Override
    public <N extends Node> List<NodeEntry<UUID, N>> searchUserNodes(ConstraintNodeMatcher<N> constraint) throws Exception {
        return implFor(SplitStorageType.USER).searchUserNodes(constraint);
    }

    @Override
    public Group createAndLoadGroup(String name) throws Exception {
        return implFor(SplitStorageType.GROUP).createAndLoadGroup(name);
    }

    @Override
    public Optional<Group> loadGroup(String name) throws Exception {
        return implFor(SplitStorageType.GROUP).loadGroup(name);
    }

    @Override
    public void loadAllGroups() throws Exception {
        implFor(SplitStorageType.GROUP).loadAllGroups();
    }

    @Override
    public void saveGroup(Group group) throws Exception {
        implFor(SplitStorageType.GROUP).saveGroup(group);
    }

    @Override
    public void deleteGroup(Group group) throws Exception {
        implFor(SplitStorageType.GROUP).deleteGroup(group);
    }

    @Override
    public <N extends Node> List<NodeEntry<String, N>> searchGroupNodes(ConstraintNodeMatcher<N> constraint) throws Exception {
        return implFor(SplitStorageType.GROUP).searchGroupNodes(constraint);
    }

    @Override
    public Track createAndLoadTrack(String name) throws Exception {
        return implFor(SplitStorageType.TRACK).createAndLoadTrack(name);
    }

    @Override
    public Optional<Track> loadTrack(String name) throws Exception {
        return implFor(SplitStorageType.TRACK).loadTrack(name);
    }

    @Override
    public void loadAllTracks() throws Exception {
        implFor(SplitStorageType.TRACK).loadAllTracks();
    }

    @Override
    public void saveTrack(Track track) throws Exception {
        implFor(SplitStorageType.TRACK).saveTrack(track);
    }

    @Override
    public void deleteTrack(Track track) throws Exception {
        implFor(SplitStorageType.TRACK).deleteTrack(track);
    }

    @Override
    public PlayerSaveResult savePlayerData(UUID uniqueId, String username) throws Exception {
        return implFor(SplitStorageType.UUID).savePlayerData(uniqueId, username);
    }

    @Override
    public void deletePlayerData(UUID uniqueId) throws Exception {
        implFor(SplitStorageType.UUID).deletePlayerData(uniqueId);
    }

    @Override
    public UUID getPlayerUniqueId(String username) throws Exception {
        return implFor(SplitStorageType.UUID).getPlayerUniqueId(username);
    }

    @Override
    public String getPlayerName(UUID uniqueId) throws Exception {
        return implFor(SplitStorageType.UUID).getPlayerName(uniqueId);
    }
}
