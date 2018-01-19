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

package me.lucko.luckperms.common.dependencies.relocation;

import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.dependencies.DependencyManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles class runtime relocation of packages in downloaded dependencies
 */
public class RelocationHandler implements AutoCloseable {
    private final DependencyManager dependencyManager;

    private URLClassLoader classLoader;
    private Constructor<?> relocatorConstructor;
    private Method relocatorRunMethod;

    public RelocationHandler(DependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager;
        try {
            setup();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setup() throws Exception {
        File saveDirectory = this.dependencyManager.getSaveDirectory();

        // download the required dependencies for the remapping.
        File asm = this.dependencyManager.downloadDependency(saveDirectory, Dependency.ASM);
        File asmCommons = this.dependencyManager.downloadDependency(saveDirectory, Dependency.ASM_COMMONS);
        File jarRelocator = this.dependencyManager.downloadDependency(saveDirectory, Dependency.JAR_RELOCATOR);

        URL[] urls = new URL[]{
                asm.toURI().toURL(),
                asmCommons.toURI().toURL(),
                jarRelocator.toURI().toURL()
        };

        // construct an isolated classloader instance containing the dependencies needed
        this.classLoader = new URLClassLoader(urls, null);

        // load the relocator class
        Class<?> relocatorClass = this.classLoader.loadClass("me.lucko.jarrelocator.JarRelocator");

        // prepare the the reflected constructor & method instances
        this.relocatorConstructor = relocatorClass.getDeclaredConstructor(File.class, File.class, Map.class);
        this.relocatorConstructor.setAccessible(true);

        this.relocatorRunMethod = relocatorClass.getDeclaredMethod("run");
        this.relocatorRunMethod.setAccessible(true);
    }

    public void remap(File input, File output, List<Relocation> relocations) throws Exception {
        if (this.classLoader == null) {
            throw new IllegalStateException("ClassLoader is closed");
        }

        Map<String, String> mappings = new HashMap<>();
        for (Relocation relocation : relocations) {
            mappings.put(relocation.getPattern(), relocation.getRelocatedPattern());
        }

        // create and invoke a new relocator
        Object relocator = this.relocatorConstructor.newInstance(input, output, mappings);
        this.relocatorRunMethod.invoke(relocator);
    }

    @Override
    public void close() throws IOException {
        if (this.classLoader == null) {
            return;
        }

        this.classLoader.close();
        this.classLoader = null;
    }
}
