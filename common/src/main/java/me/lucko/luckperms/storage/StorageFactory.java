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

package me.lucko.luckperms.storage;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.storage.methods.*;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class StorageFactory {
    private static final Set<String> TYPES = ImmutableSet.of("json", "yaml", "flatfile", "mongodb", "mysql", "sqlite", "h2");

    public static Datastore getDatastore(LuckPermsPlugin plugin, String defaultMethod) {
        Datastore datastore;

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

            Map<String, Datastore> backing = new HashMap<>();

            for (String type : neededTypes) {
                backing.put(type, fromString(type, plugin));
            }

            datastore = new SplitDatastore(plugin, backing, types);

        } else {
            String storageMethod = plugin.getConfiguration().getStorageMethod().toLowerCase();
            if (!TYPES.contains(storageMethod)) {
                plugin.getLog().severe("Storage method '" + storageMethod + "' not recognised. Using the default instead.");
                storageMethod = defaultMethod;
            }

            datastore = fromString(storageMethod, plugin);
            plugin.getLog().info("Using " + datastore.getName() + " as storage method.");
        }

        plugin.getLog().info("Initialising datastore...");
        datastore.init();
        return datastore;
    }
    
    private static Datastore fromString(String storageMethod, LuckPermsPlugin plugin) {
        switch (storageMethod) {
            case "mysql":
                return new MySQLDatastore(plugin, plugin.getConfiguration().getDatabaseValues());
            case "sqlite":
                return new SQLiteDatastore(plugin, new File(plugin.getDataFolder(), "luckperms.sqlite"));
            case "h2":
                return new H2Datastore(plugin, new File(plugin.getDataFolder(), "luckperms.db"));
            case "mongodb":
                return new MongoDBDatastore(plugin, plugin.getConfiguration().getDatabaseValues());
            case "yaml":
                return new YAMLDatastore(plugin, plugin.getDataFolder());
            default:
                return new JSONDatastore(plugin, plugin.getDataFolder());
        }
    }
}
