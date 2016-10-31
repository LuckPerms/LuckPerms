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

package me.lucko.luckperms.common.storage.backing;

import lombok.Cleanup;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.groups.GroupManager;
import me.lucko.luckperms.common.tracks.Track;
import me.lucko.luckperms.common.tracks.TrackManager;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.common.users.UserIdentifier;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static me.lucko.luckperms.common.core.PermissionHolder.exportToLegacy;

@SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
public class YAMLBacking extends FlatfileBacking {
    public YAMLBacking(LuckPermsPlugin plugin, File pluginDir) {
        super(plugin, "Flatfile - YAML", pluginDir);
    }

    private static Yaml getYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(options);
    }

    private boolean doRead(File file, ReadOperation readOperation) {
        boolean success = false;
        try {
            @Cleanup FileReader fileReader = new FileReader(file);
            @Cleanup BufferedReader bufferedReader = new BufferedReader(fileReader);
            success = readOperation.onRun((Map<String, Object>) getYaml().load(bufferedReader));
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return success;
    }

    private boolean doWrite(File file, Map<String, Object> values) {
        try {
            @Cleanup FileWriter fileWriter = new FileWriter(file);
            @Cleanup BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            getYaml().dump(values, bufferedWriter);
            bufferedWriter.flush();
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean loadUser(UUID uuid, String username) {
        User user = plugin.getUserManager().getOrMake(UserIdentifier.of(uuid, username));
        user.getIoLock().lock();
        try {
            return call(() -> {
                File userFile = new File(usersDir, uuid.toString() + ".yml");
                if (userFile.exists()) {
                    return doRead(userFile, values -> {
                        // User exists, let's load.
                        String name = (String) values.get("name");
                        user.setPrimaryGroup((String) values.get("primary-group"));
                        Map<String, Boolean> perms = (Map<String, Boolean>) values.get("perms");
                        for (Map.Entry<String, Boolean> e : perms.entrySet()) {
                            user.addNodeUnchecked(NodeFactory.fromSerialisedNode(e.getKey(), e.getValue()));
                        }

                        boolean save = plugin.getUserManager().giveDefaultIfNeeded(user, false);

                        if (user.getName() == null || user.getName().equalsIgnoreCase("null")) {
                            user.setName(name);
                        } else {
                            if (!name.equals(user.getName())) {
                                save = true;
                            }
                        }

                        if (save) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("uuid", user.getUuid().toString());
                            data.put("name", user.getName());
                            data.put("primary-group", user.getPrimaryGroup());
                            data.put("perms", exportToLegacy(user.getNodes()));
                            doWrite(userFile, data);
                        }
                        return true;
                    });
                } else {
                    if (plugin.getUserManager().shouldSave(user)) {
                        user.clearNodes();
                        user.setPrimaryGroup(null);
                        plugin.getUserManager().giveDefaultIfNeeded(user, false);
                    }
                    return true;
                }
            }, false);
        } finally {
            user.getIoLock().unlock();
            user.getRefreshBuffer().requestDirectly();
        }
    }

    @Override
    public boolean saveUser(User user) {
        user.getIoLock().lock();
        try {
            return call(() -> {
                File userFile = new File(usersDir, user.getUuid().toString() + ".yml");
                if (!plugin.getUserManager().shouldSave(user)) {
                    if (userFile.exists()) {
                        userFile.delete();
                    }
                    return true;
                }

                if (!userFile.exists()) {
                    try {
                        userFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }

                Map<String, Object> values = new HashMap<>();
                values.put("uuid", user.getUuid().toString());
                values.put("name", user.getName());
                values.put("primary-group", user.getPrimaryGroup());
                values.put("perms", exportToLegacy(user.getNodes()));
                return doWrite(userFile, values);
            }, false);
        } finally {
            user.getIoLock().unlock();
        }
    }

    @Override
    public boolean cleanupUsers() {
        return call(() -> {
            File[] files = usersDir.listFiles((dir, name1) -> name1.endsWith(".yml"));
            if (files == null) return false;

            for (File file : files) {
                Map<String, Boolean> nodes = new HashMap<>();
                doRead(file, values -> {
                    Map<String, Boolean> perms = (Map<String, Boolean>) values.get("perms");
                    nodes.putAll(perms);
                    return true;
                });

                boolean shouldDelete = false;
                if (nodes.size() == 1) {
                    for (Map.Entry<String, Boolean> e : nodes.entrySet()) {
                        // There's only one
                        shouldDelete = e.getKey().equalsIgnoreCase("group.default") && e.getValue();
                    }
                }

                if (shouldDelete) {
                    file.delete();
                }
            }
            return true;
        }, false);
    }

    @Override
    public Set<UUID> getUniqueUsers() {
        String[] fileNames = usersDir.list((dir, name) -> name.endsWith(".yml"));
        if (fileNames == null) return null;
        return Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - 4))
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean createAndLoadGroup(String name) {
        Group group = plugin.getGroupManager().getOrMake(name);
        group.getIoLock().lock();
        try {
            return call(() -> {
                File groupFile = new File(groupsDir, name + ".yml");
                if (groupFile.exists()) {
                    return doRead(groupFile, values -> {
                        Map<String, Boolean> perms = (Map<String, Boolean>) values.get("perms");
                        for (Map.Entry<String, Boolean> e : perms.entrySet()) {
                            group.addNodeUnchecked(NodeFactory.fromSerialisedNode(e.getKey(), e.getValue()));
                        }
                        return true;
                    });
                } else {
                    try {
                        groupFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }

                    Map<String, Object> values = new HashMap<>();
                    values.put("name", group.getName());
                    values.put("perms", exportToLegacy(group.getNodes()));
                    return doWrite(groupFile, values);
                }
            }, false);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean loadGroup(String name) {
        Group group = plugin.getGroupManager().getOrMake(name);
        group.getIoLock().lock();
        try {
            return call(() -> {
                File groupFile = new File(groupsDir, name + ".yml");
                return groupFile.exists() && doRead(groupFile, values -> {
                    Map<String, Boolean> perms = (Map<String, Boolean>) values.get("perms");
                    for (Map.Entry<String, Boolean> e : perms.entrySet()) {
                        group.addNodeUnchecked(NodeFactory.fromSerialisedNode(e.getKey(), e.getValue()));
                    }
                    return true;
                });

            }, false);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean loadAllGroups() {
        String[] fileNames = groupsDir.list((dir, name) -> name.endsWith(".yml"));
        if (fileNames == null) return false;
        List<String> groups = Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - 4))
                .collect(Collectors.toList());

        groups.forEach(this::loadGroup);

        GroupManager gm = plugin.getGroupManager();
        gm.getAll().values().stream()
                .filter(g -> !groups.contains(g.getName()))
                .forEach(gm::unload);
        return true;
    }

    @Override
    public boolean saveGroup(Group group) {
        group.getIoLock().lock();
        try {
            return call(() -> {
                File groupFile = new File(groupsDir, group.getName() + ".yml");
                if (!groupFile.exists()) {
                    try {
                        groupFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }

                Map<String, Object> values = new HashMap<>();
                values.put("name", group.getName());
                values.put("perms", exportToLegacy(group.getNodes()));
                return doWrite(groupFile, values);
            }, false);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean deleteGroup(Group group) {
        group.getIoLock().lock();
        try {
            return call(() -> {
                File groupFile = new File(groupsDir, group.getName() + ".yml");
                if (groupFile.exists()) {
                    groupFile.delete();
                }
                return true;
            }, false);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean createAndLoadTrack(String name) {
        Track track = plugin.getTrackManager().getOrMake(name);
        track.getIoLock().lock();
        try {
            return call(() -> {
                File trackFile = new File(tracksDir, name + ".yml");
                if (trackFile.exists()) {
                    return doRead(trackFile, values -> {
                        track.setGroups((List<String>) values.get("groups"));
                        return true;
                    });
                } else {
                    try {
                        trackFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }

                    Map<String, Object> values = new HashMap<>();
                    values.put("name", track.getName());
                    values.put("groups", track.getGroups());

                    return doWrite(trackFile, values);
                }
            }, false);
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean loadTrack(String name) {
        Track track = plugin.getTrackManager().getOrMake(name);
        track.getIoLock().lock();
        try {
            return call(() -> {
                File trackFile = new File(tracksDir, name + ".yml");
                return trackFile.exists() && doRead(trackFile, values -> {
                    track.setGroups((List<String>) values.get("groups"));
                    return true;
                });
            }, false);
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean loadAllTracks() {
        String[] fileNames = tracksDir.list((dir, name) -> name.endsWith(".yml"));
        if (fileNames == null) return false;
        List<String> tracks = Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - 4))
                .collect(Collectors.toList());

        tracks.forEach(this::loadTrack);

        TrackManager tm = plugin.getTrackManager();
        tm.getAll().values().stream()
                .filter(t -> !tracks.contains(t.getName()))
                .forEach(tm::unload);
        return true;
    }

    @Override
    public boolean saveTrack(Track track) {
        track.getIoLock().lock();
        try {
            return call(() -> {
                File trackFile = new File(tracksDir, track.getName() + ".yml");
                if (!trackFile.exists()) {
                    try {
                        trackFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }

                Map<String, Object> values = new HashMap<>();
                values.put("name", track.getName());
                values.put("groups", track.getGroups());
                return doWrite(trackFile, values);
            }, false);
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean deleteTrack(Track track) {
        track.getIoLock().lock();
        try {
            return call(() -> {
                File trackFile = new File(tracksDir, track.getName() + ".yml");
                if (trackFile.exists()) {
                    trackFile.delete();
                }
                return true;
            }, false);
        } finally {
            track.getIoLock().unlock();
        }
    }

    private static <T> T call(Callable<T> c, T def) {
        try {
            return c.call();
        } catch (Exception e) {
            e.printStackTrace();
            return def;
        }
    }

    interface ReadOperation {
        boolean onRun(Map<String, Object> values);
    }
}
