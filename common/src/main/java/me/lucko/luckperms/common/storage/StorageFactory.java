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

package me.lucko.luckperms.common.storage;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.dao.AbstractDao;
import me.lucko.luckperms.common.storage.dao.SplitStorageDao;
import me.lucko.luckperms.common.storage.dao.file.HoconDao;
import me.lucko.luckperms.common.storage.dao.file.JsonDao;
import me.lucko.luckperms.common.storage.dao.file.YamlDao;
import me.lucko.luckperms.common.storage.dao.mongodb.MongoDao;
import me.lucko.luckperms.common.storage.dao.sql.SqlDao;
import me.lucko.luckperms.common.storage.dao.sql.connection.file.H2ConnectionFactory;
import me.lucko.luckperms.common.storage.dao.sql.connection.file.SQLiteConnectionFactory;
import me.lucko.luckperms.common.storage.dao.sql.connection.hikari.MariaDbConnectionFactory;
import me.lucko.luckperms.common.storage.dao.sql.connection.hikari.MySqlConnectionFactory;
import me.lucko.luckperms.common.storage.dao.sql.connection.hikari.PostgreConnectionFactory;
import me.lucko.luckperms.common.storage.provider.StorageProviders;
import me.lucko.luckperms.common.utils.ImmutableCollectors;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class StorageFactory {
    private final LuckPermsPlugin plugin;

    public StorageFactory(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    public Set<StorageType> getRequiredTypes(StorageType defaultMethod) {
        if (this.plugin.getConfiguration().get(ConfigKeys.SPLIT_STORAGE)) {
            return this.plugin.getConfiguration().get(ConfigKeys.SPLIT_STORAGE_OPTIONS).entrySet().stream()
                    .map(e -> {
                        StorageType type = StorageType.parse(e.getValue());
                        if (type == null) {
                            this.plugin.getLog().severe("Storage method for " + e.getKey() + " - " + e.getValue() + " not recognised. " +
                                    "Using the default instead.");
                            type = defaultMethod;
                        }
                        return type;
                    })
                    .collect(ImmutableCollectors.toEnumSet(StorageType.class));
        } else {
            String method = this.plugin.getConfiguration().get(ConfigKeys.STORAGE_METHOD);
            StorageType type = StorageType.parse(method);
            if (type == null) {
                this.plugin.getLog().severe("Storage method '" + method + "' not recognised. Using the default instead.");
                type = defaultMethod;
            }
            return ImmutableSet.of(type);
        }
    }

    public Storage getInstance(StorageType defaultMethod) {
        Storage storage;
        if (this.plugin.getConfiguration().get(ConfigKeys.SPLIT_STORAGE)) {
            this.plugin.getLog().info("Loading storage provider... [SPLIT STORAGE]");

            Map<SplitStorageType, StorageType> mappedTypes = this.plugin.getConfiguration().get(ConfigKeys.SPLIT_STORAGE_OPTIONS).entrySet().stream()
                    .map(e -> {
                        StorageType type = StorageType.parse(e.getValue());
                        if (type == null) {
                            type = defaultMethod;
                        }
                        return Maps.immutableEntry(e.getKey(), type);
                    })
                    .collect(ImmutableCollectors.toEnumMap(SplitStorageType.class, Map.Entry::getKey, Map.Entry::getValue));

            Map<StorageType, AbstractDao> backing = mappedTypes.values().stream()
                    .distinct()
                    .collect(ImmutableCollectors.toEnumMap(StorageType.class, e -> e, this::makeDao));

            storage = AbstractStorage.create(this.plugin, new SplitStorageDao(this.plugin, backing, mappedTypes));

        } else {
            String method = this.plugin.getConfiguration().get(ConfigKeys.STORAGE_METHOD);
            StorageType type = StorageType.parse(method);
            if (type == null) {
                type = defaultMethod;
            }

            this.plugin.getLog().info("Loading storage provider... [" + type.name() + "]");
            storage = makeInstance(type);
        }

        storage.init();
        return storage;
    }

    private Storage makeInstance(StorageType type) {
        return AbstractStorage.create(this.plugin, makeDao(type));
    }

    private AbstractDao makeDao(StorageType method) {
        switch (method) {
            case CUSTOM:
                return StorageProviders.getProvider().provide(this.plugin);
            case MARIADB:
                return new SqlDao(
                        this.plugin,
                        new MariaDbConnectionFactory(this.plugin.getConfiguration().get(ConfigKeys.DATABASE_VALUES)),
                        this.plugin.getConfiguration().get(ConfigKeys.SQL_TABLE_PREFIX)
                );
            case MYSQL:
                return new SqlDao(
                        this.plugin,
                        new MySqlConnectionFactory(this.plugin.getConfiguration().get(ConfigKeys.DATABASE_VALUES)),
                        this.plugin.getConfiguration().get(ConfigKeys.SQL_TABLE_PREFIX)
                );
            case SQLITE:
                return new SqlDao(
                        this.plugin,
                        new SQLiteConnectionFactory(this.plugin, new File(this.plugin.getDataDirectory(), "luckperms-sqlite.db")),
                        this.plugin.getConfiguration().get(ConfigKeys.SQL_TABLE_PREFIX)
                );
            case H2:
                return new SqlDao(
                        this.plugin,
                        new H2ConnectionFactory(this.plugin, new File(this.plugin.getDataDirectory(), "luckperms-h2")),
                        this.plugin.getConfiguration().get(ConfigKeys.SQL_TABLE_PREFIX)
                );
            case POSTGRESQL:
                return new SqlDao(
                        this.plugin,
                        new PostgreConnectionFactory(this.plugin.getConfiguration().get(ConfigKeys.DATABASE_VALUES)),
                        this.plugin.getConfiguration().get(ConfigKeys.SQL_TABLE_PREFIX)
                );
            case MONGODB:
                return new MongoDao(
                        this.plugin,
                        this.plugin.getConfiguration().get(ConfigKeys.DATABASE_VALUES),
                        this.plugin.getConfiguration().get(ConfigKeys.MONGODB_COLLECTION_PREFIX),
                        this.plugin.getConfiguration().get(ConfigKeys.MONGODB_CONNECTION_URI)
                );
            case YAML:
                return new YamlDao(this.plugin, "yaml-storage");
            case HOCON:
                return new HoconDao(this.plugin, "hocon-storage");
            default:
                return new JsonDao(this.plugin, "json-storage");
        }
    }
}
