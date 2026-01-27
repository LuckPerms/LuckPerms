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

package me.lucko.luckperms.standalone.loader;

import me.lucko.luckperms.common.loader.JarInJarClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Loader bootstrap for LuckPerms running as a "standalone" app.
 *
 * There are two main modules:
 * 1. the loader (this)
 *      - performs jar-in-jar loading
 * 2. the application
 *      - allows the user to interact through a basic terminal layer
 */
public class StandaloneLoader {
    public static final Logger LOGGER = LogManager.getLogger(StandaloneLoader.class);

    private static final String JAR_NAME = "luckperms-standalone.jarinjar";
    private static final String APPLICATION_CLASS = "me.lucko.luckperms.standalone.LuckPermsApplication";
    private static final String DEPENDENCY_PRELOADER_CLASS = "me.lucko.luckperms.standalone.StandaloneDependencyPreloader";

    // Entrypoint
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.error("Exception in thread " + t.getName(), e));

        ClassLoader loader = new JarInJarClassLoader(StandaloneLoader.class.getClassLoader(), JAR_NAME);

        // special case for dependency preload command
        if (args.length == 1 && args[0].equals("preloadDependencies")) {
            try {
                Class<?> clazz = loader.loadClass(DEPENDENCY_PRELOADER_CLASS);
                clazz.getMethod("main").invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        // start the application
        try {
            loader.loadClass(APPLICATION_CLASS).getConstructor(String[].class).newInstance(new Object[] {args});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
