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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.core.NodeModel;
import me.lucko.luckperms.common.core.UserIdentifier;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.managers.impl.GenericUserManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.holder.NodeHeldPermission;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
public class YAMLBacking extends FlatfileBacking {
    private static Yaml getYaml() {
        DumperOptions options = new DumperOptions();
        options.setAllowUnicode(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(options);
    }

    public YAMLBacking(LuckPermsPlugin plugin, File pluginDir, String dataFolderName) {
        super(plugin, "YAML", pluginDir, ".yml", dataFolderName);
    }

    public boolean writeMapToFile(File file, Map<String, Object> values) {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            getYaml().dump(values, writer);
            writer.flush();
            return true;
        } catch (Throwable t) {
            plugin.getLog().warn("Exception whilst writing to file: " + file.getAbsolutePath());
            t.printStackTrace();
            return false;
        }
    }

    public Map<String, Object> readMapFromFile(File file) {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return (Map<String, Object>) getYaml().load(reader);
        } catch (Throwable t) {
            plugin.getLog().warn("Exception whilst reading from file: " + file.getAbsolutePath());
            t.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean applyBulkUpdate(BulkUpdate bulkUpdate) {
        return call("null", () -> {
            if (bulkUpdate.getDataType().isIncludingUsers()) {
                File[] files = usersDir.listFiles((dir, name1) -> name1.endsWith(".yml"));
                if (files == null) return false;

                for (File file : files) {
                    call(file.getName(), () -> {
                        registerFileAction("users", file);

                        Map<String, Object> values = readMapFromFile(file);

                        Set<NodeModel> nodes = new HashSet<>();
                        nodes.addAll(deserializePermissions((List<Object>) values.get("permissions")));

                        Set<NodeModel> results = nodes.stream()
                                .map(n -> Optional.ofNullable(bulkUpdate.apply(n)))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.toSet());

                        values.put("permissions", serializePermissions(results));
                        writeMapToFile(file, values);
                        return true;
                    }, true);
                }
            }

            if (bulkUpdate.getDataType().isIncludingGroups()) {
                File[] files = groupsDir.listFiles((dir, name1) -> name1.endsWith(".yml"));
                if (files == null) return false;

                for (File file : files) {
                    call(file.getName(), () -> {
                        registerFileAction("groups", file);

                        Map<String, Object> values = readMapFromFile(file);

                        Set<NodeModel> nodes = new HashSet<>();
                        nodes.addAll(deserializePermissions((List<Object>) values.get("permissions")));

                        Set<NodeModel> results = nodes.stream()
                                .map(n -> Optional.ofNullable(bulkUpdate.apply(n)))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.toSet());

                        values.put("permissions", serializePermissions(results));
                        writeMapToFile(file, values);
                        return true;
                    }, true);
                }
            }

            return true;
        }, false);
    }

    @Override
    public boolean loadUser(UUID uuid, String username) {
        User user = plugin.getUserManager().getOrMake(UserIdentifier.of(uuid, username));
        user.getIoLock().lock();
        try {
            return call(uuid.toString(), () -> {
                File userFile = new File(usersDir, uuid.toString() + ".yml");
                registerFileAction("users", userFile);
                if (userFile.exists()) {
                    Map<String, Object> values = readMapFromFile(userFile);

                    // User exists, let's load.
                    String name = (String) values.get("name");
                    user.getPrimaryGroup().setStoredValue((String) values.get("primary-group"));

                    Set<NodeModel> data = deserializePermissions((List<Object>) values.get("permissions"));
                    Set<Node> nodes = data.stream().map(NodeModel::toNode).collect(Collectors.toSet());
                    user.setNodes(nodes);

                    boolean save = plugin.getUserManager().giveDefaultIfNeeded(user, false);

                    if (user.getName() == null || user.getName().equalsIgnoreCase("null")) {
                        user.setName(name);
                    } else {
                        if (!name.equalsIgnoreCase(user.getName())) {
                            save = true;
                        }
                    }

                    if (save) {
                        saveUser(user);
                    }
                    return true;
                } else {
                    if (GenericUserManager.shouldSave(user)) {
                        user.clearNodes();
                        user.getPrimaryGroup().setStoredValue(null);
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
            return call(user.getUuid().toString(), () -> {
                File userFile = new File(usersDir, user.getUuid().toString() + ".yml");
                registerFileAction("users", userFile);
                if (!GenericUserManager.shouldSave(user)) {
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

                Map<String, Object> values = new LinkedHashMap<>();
                values.put("uuid", user.getUuid().toString());
                values.put("name", user.getName());
                values.put("primary-group", user.getPrimaryGroup().getStoredValue());

                Set<NodeModel> data = user.getNodes().values().stream().map(NodeModel::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
                values.put("permissions", serializePermissions(data));

                return writeMapToFile(userFile, values);
            }, false);
        } finally {
            user.getIoLock().unlock();
        }
    }

    @Override
    public boolean cleanupUsers() {
        return call("null", () -> {
            File[] files = usersDir.listFiles((dir, name1) -> name1.endsWith(".yml"));
            if (files == null) return false;

            for (File file : files) {
                call(file.getName(), () -> {
                    registerFileAction("users", file);

                    Map<String, Object> values = readMapFromFile(file);

                    Set<NodeModel> nodes = new HashSet<>();
                    nodes.addAll(deserializePermissions((List<Object>) values.get("permissions")));

                    boolean shouldDelete = false;
                    if (nodes.size() == 1) {
                        for (NodeModel e : nodes) {
                            // There's only one
                            shouldDelete = e.getPermission().equalsIgnoreCase("group.default") && e.isValue();
                        }
                    }

                    if (shouldDelete) {
                        file.delete();
                    }
                    return true;
                }, true);
            }
            return true;
        }, false);
    }

    @Override
    public List<HeldPermission<UUID>> getUsersWithPermission(String permission) {
        ImmutableList.Builder<HeldPermission<UUID>> held = ImmutableList.builder();
        boolean success = call("null", () -> {
            File[] files = usersDir.listFiles((dir, name1) -> name1.endsWith(".yml"));
            if (files == null) return false;

            for (File file : files) {
                call(file.getName(), () -> {
                    registerFileAction("users", file);

                    UUID holder = UUID.fromString(file.getName().substring(0, file.getName().length() - 4));

                    Map<String, Object> values = readMapFromFile(file);

                    Set<NodeModel> nodes = new HashSet<>();
                    nodes.addAll(deserializePermissions((List<Object>) values.get("permissions")));

                    for (NodeModel e : nodes) {
                        if (!e.getPermission().equalsIgnoreCase(permission)) {
                            continue;
                        }

                        held.add(NodeHeldPermission.of(holder, e));
                    }
                    return true;
                }, true);
            }
            return true;
        }, false);
        return success ? held.build() : null;
    }

    @Override
    public boolean createAndLoadGroup(String name) {
        Group group = plugin.getGroupManager().getOrMake(name);
        group.getIoLock().lock();
        try {
            return call(name, () -> {
                File groupFile = new File(groupsDir, name + ".yml");
                registerFileAction("groups", groupFile);

                if (groupFile.exists()) {
                    Map<String, Object> values = readMapFromFile(groupFile);
                    Set<NodeModel> data = deserializePermissions((List<Object>) values.get("permissions"));
                    Set<Node> nodes = data.stream().map(NodeModel::toNode).collect(Collectors.toSet());
                    group.setNodes(nodes);
                    return true;
                } else {
                    try {
                        groupFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }

                    Map<String, Object> values = new LinkedHashMap<>();
                    values.put("name", group.getName());
                    Set<NodeModel> data = group.getNodes().values().stream().map(NodeModel::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
                    values.put("permissions", serializePermissions(data));
                    return writeMapToFile(groupFile, values);
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
            return call(name, () -> {
                File groupFile = new File(groupsDir, name + ".yml");
                registerFileAction("groups", groupFile);

                if (!groupFile.exists()) {
                    return false;
                }

                Map<String, Object> values = readMapFromFile(groupFile);
                Set<NodeModel> data = deserializePermissions((List<Object>) values.get("permissions"));
                Set<Node> nodes = data.stream().map(NodeModel::toNode).collect(Collectors.toSet());
                group.setNodes(nodes);
                return true;
            }, false);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean saveGroup(Group group) {
        group.getIoLock().lock();
        try {
            return call(group.getName(), () -> {
                File groupFile = new File(groupsDir, group.getName() + ".yml");
                registerFileAction("groups", groupFile);

                if (!groupFile.exists()) {
                    try {
                        groupFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }

                Map<String, Object> values = new LinkedHashMap<>();
                values.put("name", group.getName());
                Set<NodeModel> data = group.getNodes().values().stream().map(NodeModel::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
                values.put("permissions", serializePermissions(data));
                return writeMapToFile(groupFile, values);
            }, false);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public List<HeldPermission<String>> getGroupsWithPermission(String permission) {
        ImmutableList.Builder<HeldPermission<String>> held = ImmutableList.builder();
        boolean success = call("null", () -> {
            File[] files = groupsDir.listFiles((dir, name1) -> name1.endsWith(".yml"));
            if (files == null) return false;

            for (File file : files) {
                call(file.getName(), () -> {
                    registerFileAction("groups", file);

                    String holder = file.getName().substring(0, file.getName().length() - 4);

                    Map<String, Object> values = readMapFromFile(file);

                    Set<NodeModel> nodes = new HashSet<>();
                    nodes.addAll(deserializePermissions((List<Object>) values.get("permissions")));

                    for (NodeModel e : nodes) {
                        if (!e.getPermission().equalsIgnoreCase(permission)) {
                            continue;
                        }

                        held.add(NodeHeldPermission.of(holder, e));
                    }

                    return true;
                }, true);
            }
            return true;
        }, false);
        return success ? held.build() : null;
    }

    @Override
    public boolean createAndLoadTrack(String name) {
        Track track = plugin.getTrackManager().getOrMake(name);
        track.getIoLock().lock();
        try {
            return call(name, () -> {
                File trackFile = new File(tracksDir, name + ".yml");
                registerFileAction("tracks", trackFile);

                if (trackFile.exists()) {
                    Map<String, Object> values = readMapFromFile(trackFile);
                    track.setGroups((List<String>) values.get("groups"));
                    return true;
                } else {
                    try {
                        trackFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }

                    Map<String, Object> values = new LinkedHashMap<>();
                    values.put("name", track.getName());
                    values.put("groups", track.getGroups());

                    return writeMapToFile(trackFile, values);
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
            return call(name, () -> {
                File trackFile = new File(tracksDir, name + ".yml");
                registerFileAction("tracks", trackFile);

                if (!trackFile.exists()) {
                    return false;
                }

                Map<String, Object> values = readMapFromFile(trackFile);
                track.setGroups((List<String>) values.get("groups"));
                return true;
            }, false);
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean saveTrack(Track track) {
        track.getIoLock().lock();
        try {
            return call(track.getName(), () -> {
                File trackFile = new File(tracksDir, track.getName() + ".yml");
                registerFileAction("tracks", trackFile);

                if (!trackFile.exists()) {
                    try {
                        trackFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }

                Map<String, Object> values = new LinkedHashMap<>();
                values.put("name", track.getName());
                values.put("groups", track.getGroups());
                return writeMapToFile(trackFile, values);
            }, false);
        } finally {
            track.getIoLock().unlock();
        }
    }

    public static Set<NodeModel> deserializePermissions(List<Object> permissionsSection) {
        Set<NodeModel> nodes = new HashSet<>();

        for (Object perm : permissionsSection) {

            if (!(perm instanceof Map)) {
                continue;
            }

            Map<String, Object> data = (Map<String, Object>) perm;
            Map.Entry<String, Object> entry = Iterables.getFirst(data.entrySet(), null);

            if (entry == null) {
                continue;
            }

            String permission = entry.getKey();

            if (entry.getValue() != null && entry.getValue() instanceof Map) {
                Map<String, Object> attributes = (Map<String, Object>) entry.getValue();

                boolean value = true;
                String server = "global";
                String world = "global";
                long expiry = 0L;
                ImmutableSetMultimap context = ImmutableSetMultimap.of();

                if (attributes.containsKey("value")) {
                    value = (boolean) attributes.get("value");
                }
                if (attributes.containsKey("server")) {
                    server = attributes.get("server").toString();
                }
                if (attributes.containsKey("world")) {
                    world = attributes.get("world").toString();
                }
                if (attributes.containsKey("expiry")) {
                    Object exp = attributes.get("expiry");
                    if (exp instanceof Long || exp.getClass().isPrimitive()) {
                        expiry = (long) exp;
                    } else {
                        expiry = (int) exp;
                    }
                }

                if (attributes.get("context") != null && attributes.get("context") instanceof Map) {
                    Map<String, Object> contexts = (Map<String, Object>) attributes.get("context");
                    ImmutableSetMultimap.Builder<String, String> map = ImmutableSetMultimap.builder();

                    for (Map.Entry<String, Object> e : contexts.entrySet()) {
                        Object val = e.getValue();
                        if (val instanceof List) {
                            map.putAll(e.getKey(), ((List<String>) val));
                        } else {
                            map.put(e.getKey(), val.toString());
                        }
                    }

                    context = map.build();
                }

                nodes.add(NodeModel.of(permission, value, server, world, expiry, ImmutableContextSet.fromMultimap(context)));
            }
        }

        return nodes;
    }

    public static List<Map<String, Object>> serializePermissions(Set<NodeModel> nodes) {
        List<Map<String, Object>> data = new ArrayList<>();

        for (NodeModel node : nodes) {
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("value", node.isValue());

            if (!node.getServer().equals("global")) {
                attributes.put("server", node.getServer());
            }

            if (!node.getWorld().equals("global")) {
                attributes.put("world", node.getWorld());
            }

            if (node.getExpiry() != 0L) {
                attributes.put("expiry", node.getExpiry());
            }

            if (!node.getContexts().isEmpty()) {
                Map<String, Object> context = new HashMap<>();
                Map<String, Collection<String>> map = node.getContexts().toMultimap().asMap();

                for (Map.Entry<String, Collection<String>> e : map.entrySet()) {
                    List<String> vals = new ArrayList<>(e.getValue());
                    int size = vals.size();

                    if (size == 1) {
                        context.put(e.getKey(), vals.get(0));;
                    } else if (size > 1) {
                        context.put(e.getKey(), vals);
                    }
                }

                attributes.put("context", context);
            }

            Map<String, Object> perm = new HashMap<>();
            perm.put(node.getPermission(), attributes);
            data.add(perm);
        }

        return data;
    }
}
