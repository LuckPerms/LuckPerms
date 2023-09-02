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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.google.gson.JsonElement;
import me.lucko.luckperms.common.dependencies.relocation.Relocation;
import me.lucko.luckperms.common.dependencies.relocation.RelocationHandler;
import me.lucko.luckperms.common.storage.StorageType;
import net.luckperms.api.platform.Platform;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Applies LuckPerms specific behaviour for {@link Dependency}s.
 */
public class DependencyRegistry {

    private static final SetMultimap<StorageType, Dependency> STORAGE_DEPENDENCIES = ImmutableSetMultimap.<StorageType, Dependency>builder()
            .putAll(StorageType.YAML,           Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_YAML, Dependency.SNAKEYAML)
            .putAll(StorageType.YAML_COMBINED,  Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_YAML, Dependency.SNAKEYAML)
            .putAll(StorageType.JSON,           Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_GSON)
            .putAll(StorageType.JSON_COMBINED,  Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_GSON)
            .putAll(StorageType.HOCON,          Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_HOCON, Dependency.HOCON_CONFIG)
            .putAll(StorageType.HOCON_COMBINED, Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_HOCON, Dependency.HOCON_CONFIG)
            .putAll(StorageType.TOML,           Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_TOML, Dependency.TOML4J)
            .putAll(StorageType.TOML_COMBINED,  Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_TOML, Dependency.TOML4J)
            .putAll(StorageType.MONGODB,        Dependency.MONGODB_DRIVER_CORE, Dependency.MONGODB_DRIVER_LEGACY, Dependency.MONGODB_DRIVER_SYNC, Dependency.MONGODB_DRIVER_BSON)
            .putAll(StorageType.MARIADB,        Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI, Dependency.MARIADB_DRIVER)
            .putAll(StorageType.MYSQL,          Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI, Dependency.MYSQL_DRIVER)
            .putAll(StorageType.POSTGRESQL,     Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI, Dependency.POSTGRESQL_DRIVER)
            .putAll(StorageType.SQLITE,         Dependency.SQLITE_DRIVER)
            .putAll(StorageType.H2,             Dependency.H2_DRIVER)
            .build();

    private static final Set<Platform.Type> SNAKEYAML_PROVIDED_BY_PLATFORM = ImmutableSet.of(
            Platform.Type.BUKKIT, Platform.Type.BUNGEECORD, Platform.Type.SPONGE, Platform.Type.NUKKIT
    );

    private final Platform.Type platformType;

    public DependencyRegistry(Platform.Type platformType) {
        this.platformType = platformType;
    }

    public Set<Dependency> resolveStorageDependencies(Set<StorageType> storageTypes, boolean redis, boolean rabbitmq, boolean nats) {
        Set<Dependency> dependencies = new LinkedHashSet<>();
        for (StorageType storageType : storageTypes) {
            dependencies.addAll(STORAGE_DEPENDENCIES.get(storageType));
        }

        if (redis) {
            dependencies.add(Dependency.COMMONS_POOL_2);
            dependencies.add(Dependency.JEDIS);
            dependencies.add(Dependency.SLF4J_API);
            dependencies.add(Dependency.SLF4J_SIMPLE);
        }

        if (nats) {
            dependencies.add(Dependency.NATS);
        }

        if (rabbitmq) {
            dependencies.add(Dependency.RABBITMQ);
        }

        // don't load slf4j if it's already present
        if ((dependencies.contains(Dependency.SLF4J_API) || dependencies.contains(Dependency.SLF4J_SIMPLE)) && slf4jPresent()) {
            dependencies.remove(Dependency.SLF4J_API);
            dependencies.remove(Dependency.SLF4J_SIMPLE);
        }

        // don't load snakeyaml if it's provided by the platform
        if (dependencies.contains(Dependency.SNAKEYAML) && SNAKEYAML_PROVIDED_BY_PLATFORM.contains(this.platformType)) {
            dependencies.remove(Dependency.SNAKEYAML);
        }

        return dependencies;
    }

    public void applyRelocationSettings(Dependency dependency, List<Relocation> relocations) {
        // support for LuckPerms legacy (bukkit 1.7.10)
        if (!RelocationHandler.DEPENDENCIES.contains(dependency) && isGsonRelocated()) {
            relocations.add(Relocation.of("guava", "com{}google{}common"));
            relocations.add(Relocation.of("gson", "com{}google{}gson"));
        }

        // relocate yaml within configurate if its being provided by LP
        if (dependency == Dependency.CONFIGURATE_YAML && !SNAKEYAML_PROVIDED_BY_PLATFORM.contains(this.platformType)) {
            relocations.add(Relocation.of("yaml", "org{}yaml{}snakeyaml"));
        }
    }

    public boolean shouldAutoLoad(Dependency dependency) {
        switch (dependency) {
            // all used within 'isolated' classloaders, and are therefore not
            // relocated.
            case ASM:
            case ASM_COMMONS:
            case JAR_RELOCATOR:
            case H2_DRIVER:
            case H2_DRIVER_LEGACY:
            case SQLITE_DRIVER:
                return false;
            default:
                return true;
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean isGsonRelocated() {
        return JsonElement.class.getName().startsWith("me.lucko");
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

}
