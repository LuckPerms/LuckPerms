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

package me.lucko.luckperms.common.storage.backing.utils;

import lombok.RequiredArgsConstructor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.NodeModel;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.backing.JSONBacking;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public class LegacyJSONSchemaMigration implements Runnable {
    private final LuckPermsPlugin plugin;
    private final JSONBacking backing;
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

                        JsonObject values = backing.readObjectFromFile(oldFile);

                        Map<String, Boolean> perms = new HashMap<>();
                        String name = values.get("name").getAsString();
                        JsonObject permsSection = values.get("perms").getAsJsonObject();
                        for (Map.Entry<String, JsonElement> e : permsSection.entrySet()) {
                            perms.put(e.getKey(), e.getValue().getAsBoolean());
                        }


                        Set<NodeModel> nodes = perms.entrySet().stream()
                                .map(e -> NodeFactory.fromSerializedNode(e.getKey(), e.getValue()))
                                .map(NodeModel::fromNode)
                                .collect(Collectors.toCollection(LinkedHashSet::new));

                        if (!replacementFile.exists()) {
                            try {
                                replacementFile.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        JsonObject data = new JsonObject();
                        data.addProperty("name", name);
                        data.add("permissions", JSONBacking.serializePermissions(nodes));
                        backing.writeElementToFile(replacementFile, data);

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

                        JsonObject values = backing.readObjectFromFile(oldFile);

                        Map<String, Boolean> perms = new HashMap<>();
                        String uuid = values.get("uuid").getAsString();
                        String name = values.get("name").getAsString();
                        String primaryGroup = values.get("primaryGroup").getAsString();
                        JsonObject permsSection = values.get("perms").getAsJsonObject();
                        for (Map.Entry<String, JsonElement> e : permsSection.entrySet()) {
                            perms.put(e.getKey(), e.getValue().getAsBoolean());
                        }

                        Set<NodeModel> nodes = perms.entrySet().stream()
                                .map(e -> NodeFactory.fromSerializedNode(e.getKey(), e.getValue()))
                                .map(NodeModel::fromNode)
                                .collect(Collectors.toCollection(LinkedHashSet::new));

                        if (!replacementFile.exists()) {
                            try {
                                replacementFile.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        JsonObject data = new JsonObject();
                        data.addProperty("uuid", uuid);
                        data.addProperty("name", name);
                        data.addProperty("primaryGroup", primaryGroup);
                        data.add("permissions", JSONBacking.serializePermissions(nodes));
                        backing.writeElementToFile(replacementFile, data);

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
