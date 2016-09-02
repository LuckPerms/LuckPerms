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

package me.lucko.luckperms.storage.methods;

import lombok.Cleanup;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.Node;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static me.lucko.luckperms.core.PermissionHolder.exportToLegacy;

@SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
public class YAMLDatastore extends FlatfileDatastore {
    public YAMLDatastore(LuckPermsPlugin plugin, File pluginDir) {
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
        User user = plugin.getUserManager().make(uuid, username);
        boolean success = false;

        File userFile = new File(usersDir, uuid.toString() + ".yml");
        if (userFile.exists()) {
            final String[] name = {null};
            success = doRead(userFile, values -> {
                name[0] = (String) values.get("name");
                user.setPrimaryGroup((String) values.get("primary-group"));
                Map<String, Boolean> perms = (Map<String, Boolean>) values.get("perms");
                for (Map.Entry<String, Boolean> e : perms.entrySet()) {
                    user.getNodes().add(Node.fromSerialisedNode(e.getKey(), e.getValue()));
                }
                return true;
            });

            if (user.getName().equalsIgnoreCase("null")) {
                user.setName(name[0]);
            } else {
                if (!name[0].equals(user.getName())) {
                    Map<String, Object> values = new HashMap<>();
                    values.put("uuid", user.getUuid().toString());
                    values.put("name", user.getName());
                    values.put("primary-group", user.getPrimaryGroup());
                    values.put("perms", exportToLegacy(user.getNodes()));
                    doWrite(userFile, values);
                }
            }

        } else {
            success = true;
        }

        if (success) plugin.getUserManager().updateOrSet(user);
        return success;
    }

    @Override
    public boolean saveUser(User user) {
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
    }

    @Override
    public boolean cleanupUsers() {
        File[] files = usersDir.listFiles((dir, name) -> name.endsWith(".yml"));
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
        Group group = plugin.getGroupManager().make(name);

        File groupFile = new File(groupsDir, name + ".yml");
        if (!groupFile.exists()) {
            try {
                groupFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            Map<String, Object> values = new HashMap<>();
            values.put("name", group.getName());
            values.put("perms", exportToLegacy(group.getNodes()));

            if (!doWrite(groupFile, values)) {
                return false;
            }
        }

        boolean success = doRead(groupFile, values -> {
            Map<String, Boolean> perms = (Map<String, Boolean>) values.get("perms");
            for (Map.Entry<String, Boolean> e : perms.entrySet()) {
                group.getNodes().add(Node.fromSerialisedNode(e.getKey(), e.getValue()));
            }
            return true;
        });

        if (success) plugin.getGroupManager().updateOrSet(group);
        return success;
    }

    @Override
    public boolean loadGroup(String name) {
        Group group = plugin.getGroupManager().make(name);

        File groupFile = new File(groupsDir, name + ".yml");
        if (!groupFile.exists()) {
            return false;
        }

        boolean success = doRead(groupFile, values -> {
            Map<String, Boolean> perms = (Map<String, Boolean>) values.get("perms");
            for (Map.Entry<String, Boolean> e : perms.entrySet()) {
                group.getNodes().add(Node.fromSerialisedNode(e.getKey(), e.getValue()));
            }
            return true;
        });

        if (success) plugin.getGroupManager().updateOrSet(group);
        return success;
    }

    @Override
    public boolean loadAllGroups() {
        String[] fileNames = groupsDir.list((dir, name) -> name.endsWith(".yml"));
        if (fileNames == null) return false;
        List<String> groups = Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - 4))
                .collect(Collectors.toList());

        plugin.getGroupManager().unloadAll();
        groups.forEach(this::loadGroup);
        return true;
    }

    @Override
    public boolean saveGroup(Group group) {
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
    }

    @Override
    public boolean deleteGroup(Group group) {
        File groupFile = new File(groupsDir, group.getName() + ".yml");
        if (groupFile.exists()) {
            groupFile.delete();
        }
        return true;
    }

    @Override
    public boolean createAndLoadTrack(String name) {
        Track track = plugin.getTrackManager().make(name);
        List<String> groups = new ArrayList<>();

        File trackFile = new File(tracksDir, name + ".yml");
        if (!trackFile.exists()) {
            try {
                trackFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            Map<String, Object> values = new HashMap<>();
            values.put("name", track.getName());
            values.put("groups", track.getGroups());

            if (!doWrite(trackFile, values)) {
                return false;
            }
        }

        boolean success = doRead(trackFile, values -> {
            groups.addAll((List<String>) values.get("groups"));
            return true;
        });

        track.setGroups(groups);
        if (success) plugin.getTrackManager().updateOrSet(track);
        return success;
    }

    @Override
    public boolean loadTrack(String name) {
        Track track = plugin.getTrackManager().make(name);
        List<String> groups = new ArrayList<>();

        File trackFile = new File(tracksDir, name + ".yml");
        if (!trackFile.exists()) {
            return false;
        }

        boolean success = doRead(trackFile, values -> {
            groups.addAll((List<String>) values.get("groups"));
            return true;
        });

        track.setGroups(groups);
        if (success) plugin.getTrackManager().updateOrSet(track);
        return success;
    }

    @Override
    public boolean loadAllTracks() {
        String[] fileNames = tracksDir.list((dir, name) -> name.endsWith(".yml"));
        if (fileNames == null) return false;
        List<String> tracks = Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - 4))
                .collect(Collectors.toList());

        plugin.getTrackManager().unloadAll();
        tracks.forEach(this::loadTrack);
        return true;
    }

    @Override
    public boolean saveTrack(Track track) {
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
    }

    @Override
    public boolean deleteTrack(Track track) {
        File trackFile = new File(tracksDir, track.getName() + ".yml");
        if (trackFile.exists()) {
            trackFile.delete();
        }
        return true;
    }

    interface ReadOperation {
        boolean onRun(Map<String, Object> values) throws IOException;
    }
}
