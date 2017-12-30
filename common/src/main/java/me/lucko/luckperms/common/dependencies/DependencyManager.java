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
import com.google.common.io.ByteStreams;

import me.lucko.luckperms.api.platform.PlatformType;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for loading runtime dependencies.
 */
public class DependencyManager {
    private static final Method ADD_URL_METHOD;

    static {
        Method addUrlMethod;
        try {
            addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrlMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
        ADD_URL_METHOD = addUrlMethod;
    }

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
    private final MessageDigest digest;

    public DependencyManager(LuckPermsPlugin plugin) {
        this.plugin = plugin;
        try {
            this.digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadStorageDependencies(Set<StorageType> storageTypes) {
        Set<Dependency> dependencies = new LinkedHashSet<>();
        for (StorageType storageType : storageTypes) {
            dependencies.addAll(STORAGE_DEPENDENCIES.get(storageType));
        }

        if (plugin.getConfiguration().get(ConfigKeys.REDIS_ENABLED)) {
            dependencies.add(Dependency.JEDIS);
        }

        // don't load slf4j if it's already present
        if (classExists("org.slf4j.Logger") && classExists("org.slf4j.LoggerFactory")) {
            dependencies.remove(Dependency.SLF4J_API);
            dependencies.remove(Dependency.SLF4J_SIMPLE);
        }

        // don't load configurate dependencies on sponge
        if (plugin.getServerType() == PlatformType.SPONGE) {
            dependencies.remove(Dependency.CONFIGURATE_CORE);
            dependencies.remove(Dependency.CONFIGURATE_GSON);
            dependencies.remove(Dependency.CONFIGURATE_YAML);
            dependencies.remove(Dependency.CONFIGURATE_HOCON);
            dependencies.remove(Dependency.HOCON_CONFIG);
        }

        loadDependencies(dependencies);
    }

    public void loadDependencies(Set<Dependency> dependencies) {
        plugin.getLog().info("Identified the following dependencies: " + dependencies.toString());

        File libDir = new File(plugin.getDataDirectory(), "lib");
        if (!(libDir.exists() || libDir.mkdirs())) {
            throw new RuntimeException("Unable to create lib dir - " + libDir.getPath());
        }

        // Download files.
        List<File> filesToLoad = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            try {
                filesToLoad.add(downloadDependency(libDir, dependency));
            } catch (Throwable e) {
                plugin.getLog().severe("Exception whilst downloading dependency " + dependency.name());
                e.printStackTrace();
            }
        }

        // Load classes.
        for (File file : filesToLoad) {
            try {
                loadJar(file);
            } catch (Throwable t) {
                plugin.getLog().severe("Failed to load dependency jar " + file.getName());
                t.printStackTrace();
            }
        }
    }

    private File downloadDependency(File libDir, Dependency dependency) throws Exception {
        String fileName = dependency.name().toLowerCase() + "-" + dependency.getVersion() + ".jar";

        File file = new File(libDir, fileName);
        if (file.exists()) {
            return file;
        }

        URL url = new URL(dependency.getUrl());

        plugin.getLog().info("Dependency '" + fileName + "' could not be found. Attempting to download.");
        try (InputStream in = url.openStream()) {
            byte[] bytes = ByteStreams.toByteArray(in);
            if (bytes.length == 0) {
                throw new RuntimeException("Empty stream");
            }

            byte[] hash = this.digest.digest(bytes);

            plugin.getLog().info("Successfully downloaded '" + fileName + "' with checksum: " + Base64.getEncoder().encodeToString(hash));

            if (!Arrays.equals(hash, dependency.getChecksum())) {
                throw new RuntimeException("Downloaded file had an invalid hash. Expected: " + Base64.getEncoder().encodeToString(dependency.getChecksum()));
            }

            Files.write(file.toPath(), bytes);
        }

        if (!file.exists()) {
            throw new IllegalStateException("File not present. - " + file.toString());
        } else {
            return file;
        }
    }

    private void loadJar(File file) {
        // get the classloader to load into
        ClassLoader classLoader = plugin.getClass().getClassLoader();

        if (classLoader instanceof URLClassLoader) {
            try {
                ADD_URL_METHOD.invoke(classLoader, file.toURI().toURL());
            } catch (IllegalAccessException | InvocationTargetException | MalformedURLException e) {
                throw new RuntimeException("Unable to invoke URLClassLoader#addURL", e);
            }
        } else {
            throw new RuntimeException("Unknown classloader type: " + classLoader.getClass());
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

}
