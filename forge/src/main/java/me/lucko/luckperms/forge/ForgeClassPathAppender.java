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

package me.lucko.luckperms.forge;

import cpw.mods.cl.ModuleClassLoader;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformingClassLoader;
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ForgeClassPathAppender extends URLClassLoader implements ClassPathAppender {
    private final PluginLogger logger;
    private final Map<String, ClassLoader> parentLoaders;

    public ForgeClassPathAppender(PluginLogger logger) {
        super(new URL[0], (ModuleClassLoader) Launcher.class.getClassLoader());
        this.logger = logger;
        this.parentLoaders = getParentLoaders((TransformingClassLoader) Thread.currentThread().getContextClassLoader());
    }

    @Override
    public void addJarToClasspath(Path file) {
        // For debugging purposes
        // this.logger.info("Adding " + file + " to the classpath");

        try {
            addURL(file.toUri().toURL());
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }

        if (this.parentLoaders == null) {
            return;
        }

        try (JarFile jarFile = new JarFile(file.toFile(), false)) {
            for (Enumeration<JarEntry> enumeration = jarFile.entries(); enumeration.hasMoreElements(); ) {
                JarEntry jarEntry = enumeration.nextElement();
                if (jarEntry.isDirectory() || !jarEntry.getName().endsWith(".class")) {
                    continue;
                }

                if (jarEntry.getName().endsWith("module-info.class")) {
                    continue;
                }

                String name = jarEntry.getName();
                String packageName = name.substring(0, name.lastIndexOf('/')).replace('/', '.');
                if (this.parentLoaders.putIfAbsent(packageName, this) == null) {
                    // For debugging purposes
                    // this.logger.info("Adding " + packageName + " to the parent loaders");
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void close() {
        // no-op
    }

    @SuppressWarnings("unchecked")
    private Map<String, ClassLoader> getParentLoaders(TransformingClassLoader classLoader) {
        try {
            // sun.misc.Unsafe.theUnsafe
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);

            // cpw.mods.cl.ModuleClassLoader.parentLoaders
            Field parentLoadersField = ModuleClassLoader.class.getDeclaredField("parentLoaders");
            long parentLoadersOffset = unsafe.objectFieldOffset(parentLoadersField);
            return (Map<String, ClassLoader>) unsafe.getObject(classLoader, parentLoadersOffset);
        } catch (Throwable throwable) {
            this.logger.severe("Encountered an error getting parent loaders", throwable);
            classLoader.setFallbackClassLoader(this);
            return null;
        }
    }

}
