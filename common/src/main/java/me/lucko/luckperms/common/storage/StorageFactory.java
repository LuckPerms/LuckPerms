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

package me.lucko.luckperms.common.storage;

import lombok.experimental.UtilityClass;

import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.backing.AbstractBacking;
import me.lucko.luckperms.common.storage.backing.H2Backing;
import me.lucko.luckperms.common.storage.backing.JSONBacking;
import me.lucko.luckperms.common.storage.backing.MongoDBBacking;
import me.lucko.luckperms.common.storage.backing.MySQLBacking;
import me.lucko.luckperms.common.storage.backing.SQLiteBacking;
import me.lucko.luckperms.common.storage.backing.YAMLBacking;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class StorageFactory {
    private static final Set<String> TYPES = ImmutableSet.of("json", "yaml", "flatfile", "mongodb", "mysql", "sqlite", "h2");

    public static Storage getInstance(LuckPermsPlugin plugin, String defaultMethod) {
        Storage storage;

        plugin.getLog().info("Detecting storage method...");
        if (plugin.getConfiguration().isSplitStorage()) {
            plugin.getLog().info("Using split storage.");

            Map<String, String> types = plugin.getConfiguration().getSplitStorageOptions();

            types.entrySet().stream()
                    .filter(e -> !TYPES.contains(e.getValue().toLowerCase()))
                    .forEach(e -> {
                        plugin.getLog().severe("Storage method for " + e.getKey() + " - " + e.getValue() + " not recognised. " +
                                "Using the default instead.");
                        e.setValue(defaultMethod);
                    });

            Set<String> neededTypes = new HashSet<>();
            neededTypes.addAll(types.values());

            Map<String, AbstractBacking> backing = new HashMap<>();

            for (String type : neededTypes) {
                backing.put(type, backingFromString(type, plugin));
            }

            storage = AbstractStorage.wrap(plugin, new SplitBacking(plugin, backing, types));

        } else {
            String storageMethod = plugin.getConfiguration().getStorageMethod().toLowerCase();
            if (!TYPES.contains(storageMethod)) {
                plugin.getLog().severe("Storage method '" + storageMethod + "' not recognised. Using the default instead.");
                storageMethod = defaultMethod;
            }

            storage = fromString(storageMethod, plugin);
            plugin.getLog().info("Using " + storage.getName() + " as storage method.");
        }

        plugin.getLog().info("Initialising datastore...");
        storage.init();
        return storage;
    }

    private static Storage fromString(String storageMethod, LuckPermsPlugin plugin) {
        return AbstractStorage.wrap(plugin, backingFromString(storageMethod, plugin));
    }

    private static AbstractBacking backingFromString(String method, LuckPermsPlugin plugin) {
        switch (method) {
            case "mysql":
                return new MySQLBacking(plugin, plugin.getConfiguration().getDatabaseValues());
            case "sqlite":
                return new SQLiteBacking(plugin, new File(plugin.getDataFolder(), "luckperms.sqlite"));
            case "h2":
                return new H2Backing(plugin, new File(plugin.getDataFolder(), "luckperms.db"));
            case "mongodb":
                return new MongoDBBacking(plugin, plugin.getConfiguration().getDatabaseValues());
            case "yaml":
                return new YAMLBacking(plugin, plugin.getDataFolder());
            default:
                return new JSONBacking(plugin, plugin.getDataFolder());
        }
    }
}
