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

import me.lucko.luckperms.common.loader.LoaderBootstrap;
import me.lucko.luckperms.common.plugin.bootstrap.BootstrappedWithLoader;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;
import me.lucko.luckperms.common.plugin.classpath.JarInJarClassPathAppender;
import me.lucko.luckperms.common.plugin.logging.Log4jPluginLogger;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.common.plugin.scheduler.JavaSchedulerAdapter;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.standalone.app.LuckPermsApplication;
import net.luckperms.api.platform.Platform;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Bootstrap plugin for LuckPerms running as a standalone app.
 */
public class LPStandaloneBootstrap implements LuckPermsBootstrap, LoaderBootstrap, BootstrappedWithLoader {
    private final LuckPermsApplication loader;

    private final PluginLogger logger;
    private final SchedulerAdapter schedulerAdapter;
    private final ClassPathAppender classPathAppender;
    private final LPStandalonePlugin plugin;

    private Instant startTime;
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);

    public LPStandaloneBootstrap(LuckPermsApplication loader) {
        this.loader = loader;

        this.logger = new Log4jPluginLogger(LuckPermsApplication.LOGGER);
        this.schedulerAdapter = new JavaSchedulerAdapter(this);
        this.classPathAppender = new JarInJarClassPathAppender(getClass().getClassLoader());
        this.plugin = new LPStandalonePlugin(this);
    }

    // visible for testing
    protected LPStandaloneBootstrap(LuckPermsApplication loader, ClassPathAppender classPathAppender) {
        this.loader = loader;

        this.logger = new Log4jPluginLogger(LuckPermsApplication.LOGGER);
        this.schedulerAdapter = new JavaSchedulerAdapter(this);
        this.classPathAppender = classPathAppender;
        this.plugin = createTestPlugin();
    }

    // visible for testing
    protected LPStandalonePlugin createTestPlugin() {
        return new LPStandalonePlugin(this);
    }

    // provide adapters

    @Override
    public LuckPermsApplication getLoader() {
        return this.loader;
    }

    @Override
    public PluginLogger getPluginLogger() {
        return this.logger;
    }

    @Override
    public SchedulerAdapter getScheduler() {
        return this.schedulerAdapter;
    }

    @Override
    public ClassPathAppender getClassPathAppender() {
        return this.classPathAppender;
    }

    // lifecycle

    @Override
    public void onLoad() {
        try {
            this.plugin.load();
        } finally {
            this.loadLatch.countDown();
        }
    }

    @Override
    public void onEnable() {
        this.startTime = Instant.now();
        try {
            this.plugin.enable();
        } finally {
            this.enableLatch.countDown();
        }
    }

    @Override
    public void onDisable() {
        this.plugin.disable();
    }

    @Override
    public CountDownLatch getEnableLatch() {
        return this.enableLatch;
    }

    @Override
    public CountDownLatch getLoadLatch() {
        return this.loadLatch;
    }

    // provide information about the plugin

    @Override
    public String getVersion() {
        return this.loader.getVersion();
    }

    @Override
    public Instant getStartupTime() {
        return this.startTime;
    }

    // provide information about the platform

    @Override
    public Platform.Type getType() {
        return Platform.Type.STANDALONE;
    }

    @Override
    public String getServerBrand() {
        return "standalone";
    }

    @Override
    public String getServerVersion() {
        return "n/a";
    }

    @Override
    public Path getDataDirectory() {
        return Paths.get("data").toAbsolutePath();
    }

    @Override
    public Optional<?> getPlayer(UUID uniqueId) {
        return Optional.empty();
    }

    @Override
    public Optional<UUID> lookupUniqueId(String username) {
        return Optional.empty();
    }

    @Override
    public Optional<String> lookupUsername(UUID uniqueId) {
        return Optional.empty();
    }

    @Override
    public int getPlayerCount() {
        return 0;
    }

    @Override
    public Collection<String> getPlayerList() {
        return Collections.emptyList();
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        return Collections.emptyList();
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
       return false;
    }

}
