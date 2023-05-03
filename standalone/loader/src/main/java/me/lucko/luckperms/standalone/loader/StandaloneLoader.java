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
import me.lucko.luckperms.common.loader.LoaderBootstrap;
import me.lucko.luckperms.standalone.app.LuckPermsApplication;
import me.lucko.luckperms.standalone.app.integration.ShutdownCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Loader bootstrap for LuckPerms running as a "standalone" app.
 *
 * There are three main modules:
 * 1. the loader (this)
 *      - performs jar-in-jar loading for the plugin
 *      - starts the application
 * 2. the plugin (LPStandaloneBootstrap, LPStandalonePlugin, etc)
 *      - implements the standard classes required to create an abstract LuckPerms "plugin")
 * 3. the application
 *      - allows the user to interact with the plugin through a basic terminal layer
 */
public class StandaloneLoader implements ShutdownCallback {
    public static final Logger LOGGER = LogManager.getLogger(StandaloneLoader.class);

    private static final String JAR_NAME = "luckperms-standalone.jarinjar";
    private static final String BOOTSTRAP_PLUGIN_CLASS = "me.lucko.luckperms.standalone.LPStandaloneBootstrap";
    private static final String BOOTSTRAP_DEPENDENCY_PRELOADER_CLASS = "me.lucko.luckperms.standalone.StandaloneDependencyPreloader";

    private LuckPermsApplication app;
    private JarInJarClassLoader loader;
    private LoaderBootstrap plugin;

    // Entrypoint
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.error("Exception in thread " + t.getName(), e));

        StandaloneLoader loader = new StandaloneLoader();
        loader.start(args);
    }

    public void start(String[] args) {
        // construct an application, but don't "start" it yet
        this.app = new LuckPermsApplication(this);

        // create a jar-in-jar classloader for the standalone plugin, then enable it
        // the application is passes to the plugin constructor, to allow it to pass hooks back
        this.loader = new JarInJarClassLoader(getClass().getClassLoader(), JAR_NAME);

        // special case for dependency preload command
        if (args.length == 1 && args[0].equals("preloadDependencies")) {
            try {
                Class<?> clazz = this.loader.loadClass(BOOTSTRAP_DEPENDENCY_PRELOADER_CLASS);
                clazz.getMethod("main").invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        this.plugin = this.loader.instantiatePlugin(BOOTSTRAP_PLUGIN_CLASS, LuckPermsApplication.class, this.app);
        this.plugin.onLoad();
        this.plugin.onEnable();

        // start the application
        this.app.start(args);
    }

    @Override
    public void shutdown() {
        // shutdown in reverse order
        this.app.close();
        this.plugin.onDisable();
        try {
            this.loader.close();
        } catch (IOException e) {
            LOGGER.error(e);
        }

        LogManager.shutdown(true);
    }

}
