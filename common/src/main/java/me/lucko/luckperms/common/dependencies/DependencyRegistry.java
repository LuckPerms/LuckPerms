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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.api.platform.PlatformType;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.StorageType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DependencyRegistry {

    private static final Map<StorageType, List<Dependency>> STORAGE_DEPENDENCIES = ImmutableMap.<StorageType, List<Dependency>>builder()
            .put(StorageType.JSON, ImmutableList.of(Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_GSON))
            .put(StorageType.YAML, ImmutableList.of(Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_YAML))
            .put(StorageType.HOCON, ImmutableList.of(Dependency.HOCON_CONFIG, Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_HOCON))
            .put(StorageType.MONGODB, ImmutableList.of(Dependency.MONGODB_DRIVER))
            .put(StorageType.MARIADB, ImmutableList.of(Dependency.MARIADB_DRIVER, Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI))
            .put(StorageType.MYSQL, ImmutableList.of(Dependency.MYSQL_DRIVER, Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI))
            .put(StorageType.POSTGRESQL, ImmutableList.of(Dependency.POSTGRESQL_DRIVER, Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI))
            .put(StorageType.SQLITE, ImmutableList.of(Dependency.SQLITE_DRIVER))
            .put(StorageType.H2, ImmutableList.of(Dependency.H2_DRIVER))
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
            dependencies.add(Dependency.JEDIS);
        }

        // don't load slf4j if it's already present
        if (slf4jPresent()) {
            dependencies.remove(Dependency.SLF4J_API);
            dependencies.remove(Dependency.SLF4J_SIMPLE);
        }

        // don't load configurate dependencies on sponge
        if (this.plugin.getServerType() == PlatformType.SPONGE) {
            dependencies.remove(Dependency.CONFIGURATE_CORE);
            dependencies.remove(Dependency.CONFIGURATE_GSON);
            dependencies.remove(Dependency.CONFIGURATE_YAML);
            dependencies.remove(Dependency.CONFIGURATE_HOCON);
            dependencies.remove(Dependency.HOCON_CONFIG);
        }

        return dependencies;
    }

    public static boolean classExists(String className) {
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

    // used to remap dependencies - we check to see if they're present to avoid loading unnecessarily.

    public static boolean asmPresent() {
        return classExists("org.objectweb.asm.ClassReader") &&
                classExists("org.objectweb.asm.ClassVisitor") &&
                classExists("org.objectweb.asm.ClassWriter");
    }

    public static boolean asmCommonsPresent() {
        return classExists("org.objectweb.asm.commons.ClassRemapper") &&
                classExists("org.objectweb.asm.commons.Remapper");
    }

}
