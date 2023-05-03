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

package me.lucko.luckperms.standalone;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.dependencies.DependencyManager;
import me.lucko.luckperms.common.dependencies.DependencyManagerImpl;
import me.lucko.luckperms.common.dependencies.relocation.RelocationHandler;
import me.lucko.luckperms.common.util.MoreFiles;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pre-loads and pre-relocates all possible dependencies.
 */
public class StandaloneDependencyPreloader {

    public static void main(String[] args) throws Exception {
        main();
    }

    public static void main() throws Exception {
        Path cacheDirectory = Paths.get("data").resolve("libs");
        MoreFiles.createDirectoriesIfNotExists(cacheDirectory);

        ExecutorService executorService = Executors.newFixedThreadPool(8, new ThreadFactoryBuilder().setDaemon(true).build());
        DependencyManager dependencyManager = new DependencyManagerImpl(cacheDirectory, executorService);

        Set<Dependency> dependencies = new HashSet<>(Arrays.asList(Dependency.values()));
        System.out.println("Preloading " + dependencies.size() + " dependencies...");

        dependencies.removeAll(RelocationHandler.DEPENDENCIES);
        dependencyManager.loadDependencies(RelocationHandler.DEPENDENCIES);
        dependencyManager.loadDependencies(dependencies);

        System.out.println("Done!");
    }
}

