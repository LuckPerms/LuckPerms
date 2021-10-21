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

package me.lucko.luckperms.waterdog;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.WaterdogPE;
import dev.waterdog.waterdogpe.logger.Logger;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.plugin.Plugin;

import me.lucko.luckperms.common.loader.LoaderBootstrap;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;
import me.lucko.luckperms.common.plugin.classpath.JarInJarClassPathAppender;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import net.luckperms.api.platform.Platform;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Bootstrap plugin for LuckPerms running on Waterdog.
 */
public class LPWaterdogBootstrap implements LuckPermsBootstrap, LoaderBootstrap {
    private final Plugin loader;

    /**
     * The plugin logger
     */
    private PluginLogger logger = null;

    /**
     * A scheduler adapter for the platform
     */
    private final SchedulerAdapter schedulerAdapter;

    /**
     * The plugin class path appender
     */
    private final ClassPathAppender classPathAppender;

    /**
     * The plugin instance
     */
    private final LPWaterdogPlugin plugin;

    /**
     * The time when the plugin was enabled
     */
    private Instant startTime;

    // load/enable latches
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);

    public LPWaterdogBootstrap(Plugin loader) {
        this.loader = loader;

        this.schedulerAdapter = new WaterdogSchedulerAdapter(this);
        this.classPathAppender = new JarInJarClassPathAppender(getClass().getClassLoader());
        this.plugin = new LPWaterdogPlugin(this);
    }

    // provide adapters

    public Plugin getLoader() {
        return this.loader;
    }

    public ProxyServer getProxy() {
        return ProxyServer.getInstance();
    }

    @Override
    public PluginLogger getPluginLogger() {
        if (this.logger == null) {
            throw new IllegalStateException("Logger has not been initialised yet");
        }
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
        this.logger = new WaterdogPluginLogger(this.loader.getLogger());

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
        return this.loader.getDescription().getVersion();
    }

    @Override
    public Instant getStartupTime() {
        return this.startTime;
    }

    // provide information about the platform

    @Override
    public Platform.Type getType() {
        return Platform.Type.WATERDOG;
    }

    @Override
    public String getServerBrand() {
        return "WaterdogPE Proxy";
    }

    @Override
    public String getServerVersion() {
        return WaterdogPE.version().baseVersion();
    }

    @Override
    public Path getDataDirectory() {
        return this.loader.getDataFolder().toPath().toAbsolutePath();
    }

    @Override
    public Optional<ProxiedPlayer> getPlayer(UUID uniqueId) {
        return Optional.ofNullable(getProxy().getPlayer(uniqueId));
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
        return getProxy().getPlayers().size();
    }

    @Override
    public Collection<String> getPlayerList() {
        Collection<ProxiedPlayer> players = getProxy().getPlayers().values();
        List<String> list = new ArrayList<>(players.size());

        for (ProxiedPlayer player : players) {
            list.add(player.getName());
        }
        return list;
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        Collection<ProxiedPlayer> players = getProxy().getPlayers().values();
        List<UUID> list = new ArrayList<>(players.size());

        for (ProxiedPlayer player : players) {
            list.add(player.getUniqueId());
        }

        return list;
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
        return getProxy().getPlayer(uniqueId) != null;
    }
}
