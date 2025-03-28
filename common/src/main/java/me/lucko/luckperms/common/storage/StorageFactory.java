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
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.StorageImplementation;
import me.lucko.luckperms.common.storage.implementation.custom.CustomStorageProviders;
import me.lucko.luckperms.common.storage.implementation.file.CombinedConfigurateStorage;
import me.lucko.luckperms.common.storage.implementation.file.SeparatedConfigurateStorage;
import me.lucko.luckperms.common.storage.implementation.file.loader.HoconLoader;
import me.lucko.luckperms.common.storage.implementation.file.loader.JsonLoader;
import me.lucko.luckperms.common.storage.implementation.file.loader.TomlLoader;
import me.lucko.luckperms.common.storage.implementation.file.loader.YamlLoader;
import me.lucko.luckperms.common.storage.implementation.mongodb.MongoStorage;
import me.lucko.luckperms.common.storage.implementation.rest.RestStorage;
import me.lucko.luckperms.common.storage.implementation.split.SplitStorage;
import me.lucko.luckperms.common.storage.implementation.split.SplitStorageType;
import me.lucko.luckperms.common.storage.implementation.sql.SqlStorage;
import me.lucko.luckperms.common.storage.implementation.sql.connection.file.H2ConnectionFactory;
import me.lucko.luckperms.common.storage.implementation.sql.connection.file.SqliteConnectionFactory;
import me.lucko.luckperms.common.storage.implementation.sql.connection.hikari.MariaDbConnectionFactory;
import me.lucko.luckperms.common.storage.implementation.sql.connection.hikari.MySqlConnectionFactory;
import me.lucko.luckperms.common.storage.implementation.sql.connection.hikari.PostgresConnectionFactory;
import me.lucko.luckperms.common.util.ImmutableCollectors;

import java.util.Map;
import java.util.Set;

public class StorageFactory {
    private final LuckPermsPlugin plugin;

    public StorageFactory(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    public Set<StorageType> getRequiredTypes() {
        if (this.plugin.getConfiguration().get(ConfigKeys.SPLIT_STORAGE)) {
            return ImmutableSet.copyOf(this.plugin.getConfiguration().get(ConfigKeys.SPLIT_STORAGE_OPTIONS).values());
        } else {
            return ImmutableSet.of(this.plugin.getConfiguration().get(ConfigKeys.STORAGE_METHOD));
        }
    }

    public Storage getInstance() {
        Storage storage;
        if (this.plugin.getConfiguration().get(ConfigKeys.SPLIT_STORAGE)) {
            this.plugin.getLogger().info("Loading storage provider... [SPLIT STORAGE]");

            Map<SplitStorageType, StorageType> mappedTypes = this.plugin.getConfiguration().get(ConfigKeys.SPLIT_STORAGE_OPTIONS);
            Map<StorageType, StorageImplementation> backing = mappedTypes.values().stream()
                    .distinct()
                    .collect(ImmutableCollectors.toEnumMap(StorageType.class, e -> e, this::createNewImplementation));

            // make a base implementation
            storage = new Storage(this.plugin, new SplitStorage(this.plugin, backing, mappedTypes));

        } else {
            StorageType type = this.plugin.getConfiguration().get(ConfigKeys.STORAGE_METHOD);
            this.plugin.getLogger().info("Loading storage provider... [" + type.name() + "]");
            storage = new Storage(this.plugin, createNewImplementation(type));
        }

        storage.init();
        return storage;
    }

    private StorageImplementation createNewImplementation(StorageType method) {
        switch (method) {
            case CUSTOM:
                return CustomStorageProviders.getProvider().provide(this.plugin);
            case MARIADB:
                return new SqlStorage(
                        this.plugin,
                        new MariaDbConnectionFactory(this.plugin.getConfiguration().get(ConfigKeys.DATABASE_VALUES)),
                        this.plugin.getConfiguration().get(ConfigKeys.SQL_TABLE_PREFIX)
                );
            case MYSQL:
                return new SqlStorage(
                        this.plugin,
                        new MySqlConnectionFactory(this.plugin.getConfiguration().get(ConfigKeys.DATABASE_VALUES)),
                        this.plugin.getConfiguration().get(ConfigKeys.SQL_TABLE_PREFIX)
                );
            case SQLITE:
                return new SqlStorage(
                        this.plugin,
                        new SqliteConnectionFactory(this.plugin.getBootstrap().getDataDirectory().resolve("luckperms-sqlite.db")),
                        this.plugin.getConfiguration().get(ConfigKeys.SQL_TABLE_PREFIX)
                );
            case H2:
                return new SqlStorage(
                        this.plugin,
                        new H2ConnectionFactory(this.plugin.getBootstrap().getDataDirectory().resolve("luckperms-h2-v2")),
                        this.plugin.getConfiguration().get(ConfigKeys.SQL_TABLE_PREFIX)
                );
            case POSTGRESQL:
                return new SqlStorage(
                        this.plugin,
                        new PostgresConnectionFactory(this.plugin.getConfiguration().get(ConfigKeys.DATABASE_VALUES)),
                        this.plugin.getConfiguration().get(ConfigKeys.SQL_TABLE_PREFIX)
                );
            case MONGODB:
                return new MongoStorage(
                        this.plugin,
                        this.plugin.getConfiguration().get(ConfigKeys.DATABASE_VALUES),
                        this.plugin.getConfiguration().get(ConfigKeys.MONGODB_COLLECTION_PREFIX),
                        this.plugin.getConfiguration().get(ConfigKeys.MONGODB_CONNECTION_URI)
                );
            case REST:
                return new RestStorage(
                        this.plugin,
                        this.plugin.getConfiguration().get(ConfigKeys.REST_STORAGE_URL),
                        this.plugin.getConfiguration().get(ConfigKeys.REST_STORAGE_AUTH_KEY)
                );
            case YAML:
                return new SeparatedConfigurateStorage(this.plugin, "YAML", new YamlLoader(), ".yml", "yaml-storage");
            case JSON:
                return new SeparatedConfigurateStorage(this.plugin, "JSON", new JsonLoader(), ".json", "json-storage");
            case HOCON:
                return new SeparatedConfigurateStorage(this.plugin, "HOCON", new HoconLoader(), ".conf", "hocon-storage");
            case TOML:
                return new SeparatedConfigurateStorage(this.plugin, "TOML", new TomlLoader(), ".toml", "toml-storage");
            case YAML_COMBINED:
                return new CombinedConfigurateStorage(this.plugin, "YAML Combined", new YamlLoader(), ".yml", "yaml-storage");
            case JSON_COMBINED:
                return new CombinedConfigurateStorage(this.plugin, "JSON Combined", new JsonLoader(), ".json", "json-storage");
            case HOCON_COMBINED:
                return new CombinedConfigurateStorage(this.plugin, "HOCON Combined", new HoconLoader(), ".conf", "hocon-storage");
            case TOML_COMBINED:
                return new CombinedConfigurateStorage(this.plugin, "TOML Combined", new TomlLoader(), ".toml", "toml-storage");
            default:
                throw new RuntimeException("Unknown method: " + method);
        }
    }
}
