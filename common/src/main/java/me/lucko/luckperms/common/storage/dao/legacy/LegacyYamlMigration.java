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

package me.lucko.luckperms.common.storage.dao.legacy;

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.common.node.LegacyNodeFactory;
import me.lucko.luckperms.common.node.NodeModel;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.dao.file.YamlDao;

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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public class LegacyYamlMigration implements Runnable {
    private final LuckPermsPlugin plugin;
    private final YamlDao backing;
    private final File oldDataFolder;
    private final File newDataFolder;

    private final Yaml yaml = getYaml();

    private static Yaml getYaml() {
        DumperOptions options = new DumperOptions();
        options.setAllowUnicode(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(options);
    }

    public void writeMapToFile(File file, Map<String, Object> values) {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            yaml.dump(values, writer);
            writer.flush();
        } catch (Throwable t) {
            plugin.getLog().warn("Exception whilst writing to file: " + file.getAbsolutePath());
            t.printStackTrace();
        }
    }

    public Map<String, Object> readMapFromFile(File file) {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return (Map<String, Object>) yaml.load(reader);
        } catch (Throwable t) {
            plugin.getLog().warn("Exception whilst reading from file: " + file.getAbsolutePath());
            t.printStackTrace();
            return null;
        }
    }

    @Override
    public void run() {
        plugin.getLog().warn("Moving existing files to their new location.");
        relocateFile(oldDataFolder, newDataFolder, "actions.log");
        relocateFile(oldDataFolder, newDataFolder, "uuidcache.txt");
        relocateFile(oldDataFolder, newDataFolder, "tracks");

        plugin.getLog().warn("Migrating group files");
        File oldGroupsDir = new File(oldDataFolder, "groups");
        if (oldGroupsDir.exists() && oldGroupsDir.isDirectory()) {
            File newGroupsDir = new File(newDataFolder, "groups");
            newGroupsDir.mkdir();

            File[] toMigrate = oldGroupsDir.listFiles((dir, name) -> name.endsWith(backing.getFileExtension()));
            if (toMigrate != null) {
                for (File oldFile : toMigrate) {
                    try {
                        File replacementFile = new File(newGroupsDir, oldFile.getName());

                        Map<String, Object> data = readMapFromFile(oldFile);

                        String name = (String) data.get("name");
                        Map<String, Boolean> perms = new HashMap<>((Map<String, Boolean>) data.get("perms"));

                        Set<NodeModel> nodes = perms.entrySet().stream()
                                .map(e -> LegacyNodeFactory.fromLegacyString(e.getKey(), e.getValue()))
                                .map(NodeModel::fromNode)
                                .collect(Collectors.toCollection(LinkedHashSet::new));

                        if (!replacementFile.exists()) {
                            try {
                                replacementFile.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        Map<String, Object> values = new LinkedHashMap<>();
                        values.put("name", name);
                        values.put("permissions", serializePermissions(nodes));
                        writeMapToFile(replacementFile, values);

                        oldFile.delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        plugin.getLog().warn("Migrated group files, now migrating user files.");

        File oldUsersDir = new File(oldDataFolder, "users");
        if (oldUsersDir.exists() && oldUsersDir.isDirectory()) {
            File newUsersDir = new File(newDataFolder, "users");
            newUsersDir.mkdir();

            File[] toMigrate = oldUsersDir.listFiles((dir, name) -> name.endsWith(backing.getFileExtension()));
            if (toMigrate != null) {
                for (File oldFile : toMigrate) {
                    try {
                        File replacementFile = new File(newUsersDir, oldFile.getName());

                        Map<String, Object> data = readMapFromFile(oldFile);

                        String uuid = (String) data.get("uuid");
                        String name = (String) data.get("name");
                        String primaryGroup = (String) data.get("primary-group");
                        Map<String, Boolean> perms = new HashMap<>((Map<String, Boolean>) data.get("perms"));

                        Set<NodeModel> nodes = perms.entrySet().stream()
                                .map(e -> LegacyNodeFactory.fromLegacyString(e.getKey(), e.getValue()))
                                .map(NodeModel::fromNode)
                                .collect(Collectors.toCollection(LinkedHashSet::new));

                        if (!replacementFile.exists()) {
                            try {
                                replacementFile.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        Map<String, Object> values = new LinkedHashMap<>();
                        values.put("uuid", uuid);
                        values.put("name", name);
                        values.put("primary-group", primaryGroup);
                        values.put("permissions", serializePermissions(nodes));
                        writeMapToFile(replacementFile, values);

                        oldFile.delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        plugin.getLog().warn("Migrated user files.");

        // rename the old data file
        oldDataFolder.renameTo(new File(oldDataFolder.getParent(), "old-data-backup"));

        plugin.getLog().warn("Legacy schema migration complete.");
    }

    private static void relocateFile(File dirFrom, File dirTo, String fileName) {
        File file = new File(dirFrom, fileName);
        if (file.exists()) {
            try {
                Files.move(file.toPath(), new File(dirTo, fileName).toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static List<Object> serializePermissions(Set<NodeModel> nodes) {
        List<Object> data = new ArrayList<>();

        for (NodeModel node : nodes) {
            // just a raw, default node.
            boolean single = node.getValue() &&
                    node.getServer().equalsIgnoreCase("global") &&
                    node.getWorld().equalsIgnoreCase("global") &&
                    node.getExpiry() == 0L &&
                    node.getContexts().isEmpty();

            // just add a string to the list.
            if (single) {
                data.add(node.getPermission());
                continue;
            }

            // otherwise, this node has some other special context which needs to be saved.
            // we serialise this way so it gets represented nicely in YAML.

            // create a map of node attributes
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("value", node.getValue());

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
                        context.put(e.getKey(), vals.get(0));
                    } else if (size > 1) {
                        context.put(e.getKey(), vals);
                    }
                }

                attributes.put("context", context);
            }

            // create a new map to represent this entry in the list
            // the map will only contain one entry. (the permission --> attributes)
            Map<String, Object> perm = new HashMap<>();

            // add the node to the map
            perm.put(node.getPermission(), attributes);

            // add the map to the object list, and continue
            data.add(perm);
        }

        return data;
    }
}
