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
import java.util.Set;

@UtilityClass
public class StorageFactory {
    private static final Set<String> TYPES = ImmutableSet.of("flatfile", "mongodb", "mysql", "sqlite", "h2");

    public static Datastore getDatastore(LuckPermsPlugin plugin, String defaultMethod) {
        Datastore datastore;

        plugin.getLog().info("Detecting storage method...");
        String storageMethod = plugin.getConfiguration().getStorageMethod().toLowerCase();

        if (!TYPES.contains(storageMethod)) {
            plugin.getLog().severe("Storage method '" + storageMethod + "' not recognised. Using the default instead.");
            storageMethod = defaultMethod;
        }

        switch (storageMethod) {
            case "mysql":
                plugin.getLog().info("Using MySQL as storage method.");
                datastore = new MySQLDatastore(plugin, plugin.getConfiguration().getDatabaseValues());
                break;
            case "sqlite":
                plugin.getLog().info("Using SQLite as storage method.");
                datastore = new SQLiteDatastore(plugin, new File(plugin.getDataFolder(), "luckperms.sqlite"));
                break;
            case "h2":
                plugin.getLog().info("Using H2 as storage method.");
                datastore = new H2Datastore(plugin, new File(plugin.getDataFolder(), "luckperms.db"));
                break;
            case "mongodb":
                plugin.getLog().info("Using MongoDB as storage method.");
                datastore = new MongoDBDatastore(plugin, plugin.getConfiguration().getDatabaseValues());
                break;
            default:
                plugin.getLog().info("Using Flatfile (JSON) as storage method.");
                datastore = new FlatfileDatastore(plugin, plugin.getDataFolder());
                break;
        }

        plugin.getLog().info("Initialising datastore...");
        datastore.init();
        return datastore;
    }
}
