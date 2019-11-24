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

package me.lucko.luckperms.common.dependencies;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.gson.JsonElement;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.dependencies.relocation.Relocation;
import me.lucko.luckperms.common.dependencies.relocation.RelocationHandler;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.StorageType;

import net.luckperms.api.platform.Platform;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DependencyRegistry {

    private static final ListMultimap<StorageType, Dependency> STORAGE_DEPENDENCIES = ImmutableListMultimap.<StorageType, Dependency>builder()
            .putAll(StorageType.YAML, Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_YAML)
            .putAll(StorageType.JSON, Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_GSON)
            .putAll(StorageType.HOCON, Dependency.HOCON_CONFIG, Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_HOCON)
            .putAll(StorageType.TOML, Dependency.TOML4J, Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_TOML)
            .putAll(StorageType.YAML_COMBINED, Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_YAML)
            .putAll(StorageType.JSON_COMBINED, Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_GSON)
            .putAll(StorageType.HOCON_COMBINED, Dependency.HOCON_CONFIG, Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_HOCON)
            .putAll(StorageType.TOML_COMBINED, Dependency.TOML4J, Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_TOML)
            .putAll(StorageType.MONGODB, Dependency.MONGODB_DRIVER)
            .putAll(StorageType.MARIADB, Dependency.MARIADB_DRIVER, Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI)
            .putAll(StorageType.MYSQL, Dependency.MYSQL_DRIVER, Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI)
            .putAll(StorageType.POSTGRESQL, Dependency.POSTGRESQL_DRIVER, Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI)
            .putAll(StorageType.SQLITE, Dependency.SQLITE_DRIVER)
            .putAll(StorageType.H2, Dependency.H2_DRIVER)
            .build();

    private final LuckPermsPlugin plugin;

    public DependencyRegistry(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    public Set<Dependency> resolveStorageDependencies(Set<StorageType> storageTypes) {
        Set<Dependency> dependencies = new LinkedHashSet<>();
        for (StorageType storageType : storageTypes) {
            dependencies.addAll(STORAGE_DEPENDENCIES.get(storageType));
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.REDIS_ENABLED)) {
            dependencies.add(Dependency.COMMONS_POOL_2);
            dependencies.add(Dependency.JEDIS);
            dependencies.add(Dependency.SLF4J_API);
            dependencies.add(Dependency.SLF4J_SIMPLE);
        }

        // don't load slf4j if it's already present
        if ((dependencies.contains(Dependency.SLF4J_API) || dependencies.contains(Dependency.SLF4J_SIMPLE)) && slf4jPresent()) {
            dependencies.remove(Dependency.SLF4J_API);
            dependencies.remove(Dependency.SLF4J_SIMPLE);
        }

        return dependencies;
    }

    public void applyRelocationSettings(Dependency dependency, List<Relocation> relocations) {
        Platform.Type type = this.plugin.getBootstrap().getType();

        // support for LuckPerms legacy (bukkit 1.7.10)
        if (!RelocationHandler.DEPENDENCIES.contains(dependency) && JsonElement.class.getName().startsWith("me.lucko")) {
            relocations.add(Relocation.of("guava", "com{}google{}common"));
            relocations.add(Relocation.of("gson", "com{}google{}gson"));
        }

        // relocate yaml within configurate when running velocity
        if (dependency == Dependency.CONFIGURATE_YAML && type == Platform.Type.VELOCITY) {
            relocations.add(Relocation.of("yaml", "org{}yaml{}snakeyaml"));
        }
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean slf4jPresent() {
        return classExists("org.slf4j.Logger") && classExists("org.slf4j.LoggerFactory");
    }

    public static boolean shouldAutoLoad(Dependency dependency) {
        switch (dependency) {
            // all used within 'isolated' classloaders, and are therefore not
            // relocated.
            case ASM:
            case ASM_COMMONS:
            case JAR_RELOCATOR:
            case H2_DRIVER:
            case SQLITE_DRIVER:
                return false;
            default:
                return true;
        }
    }

}
