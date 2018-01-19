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

import com.google.common.io.ByteStreams;
import me.lucko.jarrelocator.JarRelocator;
import me.lucko.jarrelocator.Relocation;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.StorageType;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Responsible for loading runtime dependencies.
 */
public class DependencyManager {
    private final LuckPermsPlugin plugin;
    private final MessageDigest digest;
    private final DependencyRegistry registry;
    private final EnumSet<Dependency> alreadyLoaded = EnumSet.noneOf(Dependency.class);

    public DependencyManager(LuckPermsPlugin plugin) {
        this.plugin = plugin;
        try {
            this.digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        this.registry = new DependencyRegistry(plugin);
    }

    public void loadStorageDependencies(Set<StorageType> storageTypes) {
        loadDependencies(this.registry.resolveStorageDependencies(storageTypes));
    }

    public void loadDependencies(Set<Dependency> dependencies) {
        loadDependencies(dependencies, true);
    }

    public void loadDependencies(Set<Dependency> dependencies, boolean applyRemapping) {
        if (applyRemapping) {
            this.plugin.getLog().info("Identified the following dependencies: " + dependencies.toString());
        }

        File saveDirectory = new File(this.plugin.getDataDirectory(), "lib");
        if (!(saveDirectory.exists() || saveDirectory.mkdirs())) {
            throw new RuntimeException("Unable to create lib dir - " + saveDirectory.getPath());
        }

        // create a list of file sources
        List<Source> sources = new ArrayList<>();

        // obtain a file for each of the dependencies
        for (Dependency dependency : dependencies) {
            if (!this.alreadyLoaded.add(dependency)) {
                continue;
            }

            try {
                File file = downloadDependency(saveDirectory, dependency);
                sources.add(new Source(dependency, file));
            } catch (Throwable e) {
                this.plugin.getLog().severe("Exception whilst downloading dependency " + dependency.name());
                e.printStackTrace();
            }
        }

        // apply any remapping rules to the files
        List<File> remappedJars = new ArrayList<>(sources.size());
        if (applyRemapping) {
            for (Source source : sources) {
                try {
                    // apply remap rules
                    List<Relocation> relocations = source.dependency.getRelocations();

                    if (relocations.isEmpty()) {
                        remappedJars.add(source.file);
                        continue;
                    }

                    File input = source.file;
                    File output = new File(input.getParentFile(), "remap-" + input.getName());

                    // if the remapped file exists already, just use that.
                    if (output.exists()) {
                        remappedJars.add(output);
                        continue;
                    }

                    // make sure ASM is loaded
                    Set<Dependency> asmDepends = EnumSet.noneOf(Dependency.class);
                    if (!DependencyRegistry.asmPresent()) {
                        asmDepends.add(Dependency.ASM);
                    }
                    if (!DependencyRegistry.asmCommonsPresent()) {
                        asmDepends.add(Dependency.ASM_COMMONS);
                    }
                    if (!asmDepends.isEmpty()) {
                        // load asm before calling the jar relocator
                        loadDependencies(asmDepends, false);
                    }

                    // attempt to remap the jar.
                    this.plugin.getLog().info("Attempting to remap " + input.getName() + "...");
                    JarRelocator relocator = new JarRelocator(input, output, relocations);
                    relocator.run();

                    remappedJars.add(output);
                } catch (Throwable e) {
                    this.plugin.getLog().severe("Unable to remap the source file '" + source.dependency.name() + "'.");
                    e.printStackTrace();
                }
            }
        } else {
            for (Source source : sources) {
                remappedJars.add(source.file);
            }
        }

        // load each of the jars
        for (File file : remappedJars) {
            try {
                this.plugin.getPluginClassLoader().loadJar(file);
            } catch (Throwable e) {
                this.plugin.getLog().severe("Failed to load dependency jar '" + file.getName() + "'.");
                e.printStackTrace();
            }
        }
    }

    private File downloadDependency(File saveDirectory, Dependency dependency) throws Exception {
        String fileName = dependency.name().toLowerCase() + "-" + dependency.getVersion() + ".jar";
        File file = new File(saveDirectory, fileName);

        // if the file already exists, don't attempt to re-download it.
        if (file.exists()) {
            return file;
        }

        URL url = new URL(dependency.getUrl());
        try (InputStream in = url.openStream()) {

            // download the jar content
            byte[] bytes = ByteStreams.toByteArray(in);
            if (bytes.length == 0) {
                throw new RuntimeException("Empty stream");
            }


            // compute a hash for the downloaded file
            byte[] hash = this.digest.digest(bytes);

            this.plugin.getLog().info("Successfully downloaded '" + fileName + "' with checksum: " + Base64.getEncoder().encodeToString(hash));

            // ensure the hash matches the expected checksum
            if (!Arrays.equals(hash, dependency.getChecksum())) {
                throw new RuntimeException("Downloaded file had an invalid hash. Expected: " + Base64.getEncoder().encodeToString(dependency.getChecksum()));
            }

            // if the checksum matches, save the content to disk
            Files.write(file.toPath(), bytes);
        }

        // ensure the file saved correctly
        if (!file.exists()) {
            throw new IllegalStateException("File not present. - " + file.toString());
        } else {
            return file;
        }
    }

    private static final class Source {
        private final Dependency dependency;
        private final File file;

        private Source(Dependency dependency, File file) {
            this.dependency = dependency;
            this.file = file;
        }
    }

}
