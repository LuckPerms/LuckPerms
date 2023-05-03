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

package me.lucko.luckperms.common.extension;

import com.google.gson.JsonObject;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.classpath.URLClassLoaderAccess;
import me.lucko.luckperms.common.util.gson.GsonProvider;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.extension.Extension;
import net.luckperms.api.extension.ExtensionManager;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleExtensionManager implements ExtensionManager, AutoCloseable {
    private final LuckPermsPlugin plugin;
    private final Set<LoadedExtension> extensions = new HashSet<>();

    public SimpleExtensionManager(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void close() {
        for (LoadedExtension extension : this.extensions) {
            try {
                extension.instance.unload();
            } catch (Exception e) {
                this.plugin.getLogger().warn("Exception unloading extension", e);
            }
        }
        this.extensions.clear();
    }

    @Override
    public void loadExtension(Extension extension) {
        if (this.extensions.stream().anyMatch(e -> e.instance.equals(extension))) {
            return;
        }
        this.plugin.getLogger().info("Loading extension: " + extension.getClass().getName());
        this.extensions.add(new LoadedExtension(extension, null));
        extension.load();
        this.plugin.getEventDispatcher().dispatchExtensionLoad(extension);
    }

    public void loadExtensions(Path directory) {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return;
        }

        try (Stream<Path> stream = Files.list(directory)) {
            stream.forEach(path -> {
                if (path.getFileName().toString().endsWith(".jar")) {
                    try {
                        loadExtension(path);
                    } catch (Exception e) {
                        this.plugin.getLogger().warn("Exception loading extension from " + path, e);
                    }
                }
            });
        } catch (IOException e) {
            this.plugin.getLogger().warn("Exception loading extensions from " + directory, e);
        }
    }

    @Override
    public @NonNull Extension loadExtension(Path path) throws IOException {
        if (this.extensions.stream().anyMatch(e -> path.equals(e.path))) {
            throw new IllegalStateException("Extension at path " + path.toString() + " already loaded.");
        }

        if (!Files.exists(path)) {
            throw new NoSuchFileException("No file at " + path);
        }

        String className;
        boolean useParentClassLoader = false;
        try (JarFile jar = new JarFile(path.toFile())) {
            JarEntry extensionJarEntry = jar.getJarEntry("extension.json");
            if (extensionJarEntry == null) {
                throw new IllegalStateException("extension.json not present");
            }
            try (InputStream in = jar.getInputStream(extensionJarEntry)) {
                if (in == null) {
                    throw new IllegalStateException("extension.json not present");
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    JsonObject parsed = GsonProvider.parser().parse(reader).getAsJsonObject();
                    className = parsed.get("class").getAsString();
                    if (parsed.has("useParentClassLoader")) {
                        useParentClassLoader = parsed.get("useParentClassLoader").getAsBoolean();
                    }
                }
            }
        }

        if (className == null) {
            throw new IllegalArgumentException("class is null");
        }

        if (useParentClassLoader && isJarInJar()) {
            try {
                addJarToParentClasspath(path);
            } catch (Throwable e) {
                throw new RuntimeException("Exception whilst classloading extension", e);
            }
        } else {
            this.plugin.getBootstrap().getClassPathAppender().addJarToClasspath(path);
        }

        Class<? extends Extension> extensionClass;
        try {
            extensionClass = Class.forName(className).asSubclass(Extension.class);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        this.plugin.getLogger().info("Loading extension: " + extensionClass.getName() + " (" + path.getFileName().toString() + ")");

        Extension extension = null;

        try {
            Constructor<? extends Extension> constructor = extensionClass.getConstructor(LuckPerms.class);
            extension = constructor.newInstance(this.plugin.getApiProvider());
        } catch (NoSuchMethodException e) {
            // ignore
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        if (extension == null) {
            try {
                Constructor<? extends Extension> constructor = extensionClass.getConstructor();
                extension = constructor.newInstance();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Unable to find valid constructor in " + extensionClass.getName());
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        this.extensions.add(new LoadedExtension(extension, path));
        extension.load();
        this.plugin.getEventDispatcher().dispatchExtensionLoad(extension);
        return extension;
    }

    @Override
    public @NonNull Collection<Extension> getLoadedExtensions() {
        return this.extensions.stream().map(e -> e.instance).collect(Collectors.toSet());
    }

    private static boolean isJarInJar() {
        String thisClassLoaderName = SimpleExtensionManager.class.getClassLoader().getClass().getName();
        return thisClassLoaderName.equals("me.lucko.luckperms.common.loader.JarInJarClassLoader");
    }

    @Deprecated
    private static void addJarToParentClasspath(Path path) throws Exception {
        ClassLoader parentClassLoader = SimpleExtensionManager.class.getClassLoader().getParent();
        if (!(parentClassLoader instanceof URLClassLoader)) {
            throw new RuntimeException("useParentClassLoader is true but parent is not a URLClassLoader");
        }

        URLClassLoaderAccess.create(((URLClassLoader) parentClassLoader)).addURL(path.toUri().toURL());
    }

    private static final class LoadedExtension {
        private final Extension instance;
        private final Path path;

        private LoadedExtension(Extension instance, Path path) {
            this.instance = instance;
            this.path = path;
        }
    }
}
