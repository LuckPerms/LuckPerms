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

import lombok.experimental.UtilityClass;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import me.lucko.luckperms.api.PlatformType;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.StorageType;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class DependencyManager {
    private static final Method ADD_URL_METHOD;

    static {
        Method addUrlMethod = null;
        try {
            addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrlMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        ADD_URL_METHOD = addUrlMethod;
    }

    public static final Map<StorageType, List<Dependency>> STORAGE_DEPENDENCIES = ImmutableMap.<StorageType, List<Dependency>>builder()
            .put(StorageType.JSON, ImmutableList.of())
            .put(StorageType.YAML, ImmutableList.of())
            .put(StorageType.MONGODB, ImmutableList.of(Dependency.MONGODB_DRIVER))
            .put(StorageType.MARIADB, ImmutableList.of(Dependency.MARIADB_DRIVER, Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI))
            .put(StorageType.MYSQL, ImmutableList.of(Dependency.MYSQL_DRIVER, Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI))
            .put(StorageType.POSTGRESQL, ImmutableList.of(Dependency.POSTGRESQL_DRIVER, Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI))
            .put(StorageType.SQLITE, ImmutableList.of(Dependency.SQLITE_DRIVER))
            .put(StorageType.H2, ImmutableList.of(Dependency.H2_DRIVER))
            .build();

    public static void loadDependencies(LuckPermsPlugin plugin, Set<StorageType> storageTypes) {
        List<Dependency> dependencies = new ArrayList<>();
        for (StorageType storageType : storageTypes) {
            dependencies.addAll(STORAGE_DEPENDENCIES.get(storageType));
        }

        if (plugin.getConfiguration().get(ConfigKeys.REDIS_ENABLED)) {
            dependencies.add(Dependency.JEDIS);
        }

        // don't load slf4j on sponge.
        if (plugin.getServerType() == PlatformType.SPONGE) {
            dependencies.remove(Dependency.SLF4J_API);
            dependencies.remove(Dependency.SLF4J_SIMPLE);
        }

        loadDependencies(plugin, dependencies);
    }

    public static void loadDependencies(LuckPermsPlugin plugin, List<Dependency> dependencies) {
        plugin.getLog().info("Identified the following dependencies: " + dependencies.toString());

        File data = new File(plugin.getDataDirectory(), "lib");
        data.mkdirs();

        // Download files.
        List<Map.Entry<Dependency, File>> toLoad = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            try {
                toLoad.add(Maps.immutableEntry(dependency, downloadDependency(plugin, data, dependency)));
            } catch (Throwable e) {
                plugin.getLog().severe("Exception whilst downloading dependency " + dependency.name());
                e.printStackTrace();
            }
        }

        // Load classes.
        for (Map.Entry<Dependency, File> e : toLoad) {
            try {
                loadJar(plugin, e.getValue());
            } catch (Throwable t) {
                plugin.getLog().severe("Failed to load jar for dependency " + e.getKey().name());
                t.printStackTrace();
            }
        }
    }

    private static File downloadDependency(LuckPermsPlugin plugin, File libDir, Dependency dependency) throws Exception {
        String name = dependency.name().toLowerCase() + "-" + dependency.getVersion() + ".jar";

        File file = new File(libDir, name);
        if (file.exists()) {
            return file;
        }

        URL url = new URL(dependency.getUrl());

        plugin.getLog().info("Dependency '" + name + "' could not be found. Attempting to download.");
        try (InputStream in = url.openStream()) {
            Files.copy(in, file.toPath());
        }

        if (!file.exists()) {
            throw new IllegalStateException("File not present. - " + file.toString());
        } else {
            plugin.getLog().info("Dependency '" + name + "' successfully downloaded.");
            return file;
        }
    }

    private static void loadJar(LuckPermsPlugin plugin, File file) {
        // get the classloader to load into
        ClassLoader classLoader = plugin.getClass().getClassLoader();

        if (classLoader instanceof URLClassLoader) {
            try {
                ADD_URL_METHOD.invoke(classLoader, file.toURI().toURL());
            } catch (IllegalAccessException | InvocationTargetException | MalformedURLException e) {
                throw new RuntimeException("Unable to invoke URLClassLoader#addURL", e);
            }
        } else {
            throw new RuntimeException("Unknown classloader: " + classLoader.getClass());
        }
    }

}
