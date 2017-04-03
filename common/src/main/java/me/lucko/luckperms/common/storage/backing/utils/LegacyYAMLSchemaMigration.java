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

package me.lucko.luckperms.common.storage.backing.utils;

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.backing.YAMLBacking;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public class LegacyYAMLSchemaMigration implements Runnable {
    private final LuckPermsPlugin plugin;
    private final YAMLBacking backing;
    private final File oldDataFolder;
    private final File newDataFolder;

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

                        AtomicReference<String> name = new AtomicReference<>(null);
                        Map<String, Boolean> perms = new HashMap<>();
                        backing.readMapFromFile(oldFile, values -> {
                            name.set((String) values.get("name"));
                            perms.putAll((Map<String, Boolean>) values.get("perms"));
                            return true;
                        });

                        Set<NodeDataHolder> nodes = perms.entrySet().stream()
                                .map(e -> NodeFactory.fromSerialisedNode(e.getKey(), e.getValue()))
                                .map(NodeDataHolder::fromNode)
                                .collect(Collectors.toCollection(LinkedHashSet::new));

                        if (!replacementFile.exists()) {
                            try {
                                replacementFile.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        Map<String, Object> values = new LinkedHashMap<>();
                        values.put("name", name.get());
                        values.put("permissions", YAMLBacking.serializePermissions(nodes));
                        backing.writeMapToFile(replacementFile, values);

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

                        AtomicReference<String> uuid = new AtomicReference<>(null);
                        AtomicReference<String> name = new AtomicReference<>(null);
                        AtomicReference<String> primaryGroup = new AtomicReference<>(null);
                        Map<String, Boolean> perms = new HashMap<>();
                        backing.readMapFromFile(oldFile, values -> {
                            uuid.set((String) values.get("uuid"));
                            name.set((String) values.get("name"));
                            primaryGroup.set((String) values.get("primary-group"));
                            perms.putAll((Map<String, Boolean>) values.get("perms"));
                            return true;
                        });

                        Set<NodeDataHolder> nodes = perms.entrySet().stream()
                                .map(e -> NodeFactory.fromSerialisedNode(e.getKey(), e.getValue()))
                                .map(NodeDataHolder::fromNode)
                                .collect(Collectors.toCollection(LinkedHashSet::new));

                        if (!replacementFile.exists()) {
                            try {
                                replacementFile.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        Map<String, Object> values = new LinkedHashMap<>();
                        values.put("uuid", uuid.get());
                        values.put("name", name.get());
                        values.put("primary-group", primaryGroup.get());
                        values.put("permissions", YAMLBacking.serializePermissions(nodes));
                        backing.writeMapToFile(replacementFile, values);

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
}
