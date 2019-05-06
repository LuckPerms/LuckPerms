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
import com.google.common.io.ByteStreams;

import me.lucko.luckperms.common.dependencies.classloader.IsolatedClassLoader;
import me.lucko.luckperms.common.dependencies.relocation.Relocation;
import me.lucko.luckperms.common.dependencies.relocation.RelocationHandler;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.StorageType;
import me.lucko.luckperms.common.util.MoreFiles;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for loading runtime dependencies.
 */
public class DependencyManager {
    private final LuckPermsPlugin plugin;
    private final MessageDigest digest;
    private final DependencyRegistry registry;
    private final EnumMap<Dependency, Path> loaded = new EnumMap<>(Dependency.class);
    private final Map<ImmutableSet<Dependency>, IsolatedClassLoader> loaders = new HashMap<>();
    private RelocationHandler relocationHandler = null;

    public DependencyManager(LuckPermsPlugin plugin) {
        this.plugin = plugin;
        try {
            this.digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        this.registry = new DependencyRegistry(plugin);
    }

    private synchronized RelocationHandler getRelocationHandler() {
        if (this.relocationHandler == null) {
            this.relocationHandler = new RelocationHandler(this);
        }
        return this.relocationHandler;
    }

    private Path getSaveDirectory() {
        Path saveDirectory = this.plugin.getBootstrap().getDataDirectory().resolve("lib");
        try {
            MoreFiles.createDirectoriesIfNotExists(saveDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create lib directory", e);
        }
        return saveDirectory;
    }

    public IsolatedClassLoader obtainClassLoaderWith(Set<Dependency> dependencies) {
        ImmutableSet<Dependency> set = ImmutableSet.copyOf(dependencies);

        for (Dependency dependency : dependencies) {
            if (!this.loaded.containsKey(dependency)) {
                throw new IllegalStateException("Dependency " + dependency + " is not loaded.");
            }
        }

        synchronized (this.loaders) {
            IsolatedClassLoader classLoader = this.loaders.get(set);
            if (classLoader != null) {
                return classLoader;
            }

            URL[] urls = set.stream()
                    .map(this.loaded::get)
                    .map(file -> {
                        try {
                            return file.toUri().toURL();
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toArray(URL[]::new);

            classLoader = new IsolatedClassLoader(urls);
            this.loaders.put(set, classLoader);
            return classLoader;
        }
    }

    public void loadStorageDependencies(Set<StorageType> storageTypes) {
        loadDependencies(this.registry.resolveStorageDependencies(storageTypes));
    }

    public void loadDependencies(Set<Dependency> dependencies) {
        Path saveDirectory = getSaveDirectory();

        // create a list of file sources
        List<Source> sources = new ArrayList<>();

        // obtain a file for each of the dependencies
        for (Dependency dependency : dependencies) {
            if (this.loaded.containsKey(dependency)) {
                continue;
            }

            try {
                Path file = downloadDependency(saveDirectory, dependency);
                sources.add(new Source(dependency, file));
            } catch (Throwable e) {
                this.plugin.getLogger().severe("Exception whilst downloading dependency " + dependency.name());
                e.printStackTrace();
            }
        }

        // apply any remapping rules to the files
        List<Source> remappedJars = new ArrayList<>(sources.size());
        for (Source source : sources) {
            try {
                // apply remap rules
                List<Relocation> relocations = new ArrayList<>(source.dependency.getRelocations());
                this.registry.applyRelocationSettings(source.dependency, relocations);

                if (relocations.isEmpty()) {
                    remappedJars.add(source);
                    continue;
                }

                Path input = source.file;
                Path output = input.getParent().resolve("remapped-" + input.getFileName().toString());

                // if the remapped file exists already, just use that.
                if (Files.exists(output)) {
                    remappedJars.add(new Source(source.dependency, output));
                    continue;
                }

                // init the relocation handler
                RelocationHandler relocationHandler = getRelocationHandler();

                // attempt to remap the jar.
                relocationHandler.remap(input, output, relocations);

                remappedJars.add(new Source(source.dependency, output));
            } catch (Throwable e) {
                this.plugin.getLogger().severe("Unable to remap the source file '" + source.dependency.name() + "'.");
                e.printStackTrace();
            }
        }

        // load each of the jars
        for (Source source : remappedJars) {
            if (!DependencyRegistry.shouldAutoLoad(source.dependency)) {
                this.loaded.put(source.dependency, source.file);
                continue;
            }

            try {
                this.plugin.getBootstrap().getPluginClassLoader().loadJar(source.file);
                this.loaded.put(source.dependency, source.file);
            } catch (Throwable e) {
                this.plugin.getLogger().severe("Failed to load dependency jar '" + source.file.getFileName().toString() + "'.");
                e.printStackTrace();
            }
        }
    }

    private Path downloadDependency(Path saveDirectory, Dependency dependency) throws Exception {
        String fileName = dependency.name().toLowerCase() + "-" + dependency.getVersion() + ".jar";
        Path file = saveDirectory.resolve(fileName);

        // if the file already exists, don't attempt to re-download it.
        if (Files.exists(file)) {
            return file;
        }

        boolean success = false;
        Exception lastError = null;

        // getUrls returns two possible sources of the dependency.
        // [0] is a mirror of Maven Central, used to reduce load on central. apparently they don't like being used as a CDN
        // [1] is Maven Central itself

        // side note: the relative "security" of the mirror is less than central, but it actually doesn't matter.
        // we compare the downloaded file against a checksum here, so even if the mirror became compromised, RCE wouldn't be possible.
        // if the mirror download doesn't match the checksum, we just try maven central instead.

        List<URL> urls = dependency.getUrls();
        for (int i = 0; i < urls.size() && !success; i++) {
            URL url = urls.get(i);

            try {
                URLConnection connection = url.openConnection();

                // i == 0 when we're trying to use the mirror repo.
                // set some timeout properties so when/if this repository goes offline, we quickly fallback to central.
                if (i == 0) {
                    connection.setRequestProperty("User-Agent", "luckperms");
                    connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(5));
                    connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(10));
                }

                try (InputStream in = connection.getInputStream()) {
                    // download the jar content
                    byte[] bytes = ByteStreams.toByteArray(in);
                    if (bytes.length == 0) {
                        throw new RuntimeException("Empty stream");
                    }


                    // compute a hash for the downloaded file
                    byte[] hash = this.digest.digest(bytes);

                    // ensure the hash matches the expected checksum
                    if (!Arrays.equals(hash, dependency.getChecksum())) {
                        throw new RuntimeException("Downloaded file had an invalid hash. " +
                                "Expected: " + Base64.getEncoder().encodeToString(dependency.getChecksum()) + " " +
                                "Actual: " + Base64.getEncoder().encodeToString(hash));
                    }

                    // if the checksum matches, save the content to disk
                    Files.write(file, bytes);
                    success = true;
                }
            } catch (Exception e) {
                lastError = e;
            }
        }

        if (!success) {
            throw new RuntimeException("Unable to download", lastError);
        }

        // ensure the file saved correctly
        if (!Files.exists(file)) {
            throw new IllegalStateException("File not present: " + file.toString());
        } else {
            return file;
        }
    }

    private static final class Source {
        private final Dependency dependency;
        private final Path file;

        private Source(Dependency dependency, Path file) {
            this.dependency = dependency;
            this.file = file;
        }
    }

}
