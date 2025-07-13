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

package me.lucko.luckperms.library;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Supplier;

import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.loader.LoaderBootstrap;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.library.sender.PlayerLibrarySender;
import me.lucko.luckperms.library.stub.LibrarySchedulerAdapter;
import net.luckperms.api.platform.Platform;

public class LPLibraryBootstrap implements LuckPermsBootstrap, LoaderBootstrap {

    private final PluginLogger logger;
    private final Supplier<LuckPermsLibraryManager> manager;
    private final LuckPermsLibrary library;

    private final LibrarySchedulerAdapter schedulerAdapter;
    private final ClassPathAppender classPathAppender;
    private final LPLibraryPlugin plugin;

    private Instant startTime;
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);

    public LPLibraryBootstrap(boolean loadDefaultDependencies, Consumer<Set<Dependency>> modifyDependencies,
            PluginLogger logger, Supplier<LuckPermsLibraryManager> manager, LuckPermsLibrary library) {
        this.logger = logger;
        this.manager = manager;
        this.library = library;

        this.schedulerAdapter = new LibrarySchedulerAdapter(this);
        this.classPathAppender = new URLClassLoaderClassPathAppender();
        this.plugin = new LPLibraryPlugin(loadDefaultDependencies, modifyDependencies, manager, library, this);
    }

    public LPLibraryPlugin getPlugin() {
        return plugin;
    }

    // provide adapters

    @Override
    public PluginLogger getPluginLogger() {
        return this.logger;
    }

    @Override
    public LibrarySchedulerAdapter getScheduler() {
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
        return "@version@";
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
        return manager.get().getServerBrand();
    }

    @Override
    public String getServerVersion() {
        return manager.get().getServerVersion();
    }

    @Override
    public Path getDataDirectory() {
        return manager.get().getDataDirectory().toAbsolutePath();
    }

    @Override
    public Optional<PlayerLibrarySender> getPlayer(UUID uuid) {
        return library.getPlayer(uuid);
    }

    @Override
    public Optional<UUID> lookupUniqueId(String username) {
        return library.lookupUniqueId(username).or(() -> manager.get().lookupUniqueId(username));
    }

    @Override
    public Optional<String> lookupUsername(UUID uuid) {
        return library.lookupUsername(uuid).or(() -> manager.get().lookupUsername(uuid));
    }

    @Override
    public int getPlayerCount() {
        return library.getPlayerCount();
    }

    @Override
    public Collection<String> getPlayerList() {
        return library.getPlayerList();
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        return library.getOnlinePlayers();
    }

    @Override
    public boolean isPlayerOnline(UUID uuid) {
        return library.isPlayerOnline(uuid);
    }

}
