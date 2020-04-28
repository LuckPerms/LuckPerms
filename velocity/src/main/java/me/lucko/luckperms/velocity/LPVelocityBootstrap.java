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

package me.lucko.luckperms.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import me.lucko.luckperms.common.dependencies.classloader.PluginClassLoader;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.common.plugin.logging.Slf4jPluginLogger;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;

import net.luckperms.api.platform.Platform;

import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Bootstrap plugin for LuckPerms running on Velocity.
 */
@Plugin(
        id = "luckperms",
        name = "LuckPerms",
        version = "@version@",
        authors = "Luck",
        description = "A permissions plugin",
        url = "https://luckperms.net"
)
public class LPVelocityBootstrap implements LuckPermsBootstrap {

    /**
     * The plugin logger
     */
    private final PluginLogger logger;

    /**
     * A scheduler adapter for the platform
     */
    private final SchedulerAdapter schedulerAdapter;

    /**
     * The plugin classloader
     */
    private final PluginClassLoader classLoader;

    /**
     * The plugin instance
     */
    private final LPVelocityPlugin plugin;

    /**
     * The time when the plugin was enabled
     */
    private Instant startTime;

    // load/enable latches
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);

    @Inject
    private ProxyServer proxy;

    @Inject
    @DataDirectory
    private Path configDirectory;

    @Inject
    public LPVelocityBootstrap(Logger logger) {
        this.logger = new Slf4jPluginLogger(logger);
        this.schedulerAdapter = new VelocitySchedulerAdapter(this);
        this.classLoader = new VelocityClassLoader(this);
        this.plugin = new LPVelocityPlugin(this);
    }

    // provide adapters

    @Override
    public PluginLogger getPluginLogger() {
        return this.logger;
    }

    @Override
    public SchedulerAdapter getScheduler() {
        return this.schedulerAdapter;
    }

    @Override
    public PluginClassLoader getPluginClassLoader() {
        return this.classLoader;
    }

    // lifecycle

    @Subscribe(order = PostOrder.FIRST)
    public void onEnable(ProxyInitializeEvent e) {
        this.startTime = Instant.now();
        try {
            this.plugin.load();
        } finally {
            this.loadLatch.countDown();
        }

        try {
            this.plugin.enable();
        } finally {
            this.enableLatch.countDown();
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onDisable(ProxyShutdownEvent e) {
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

    // getters for the injected velocity instances

    public ProxyServer getProxy() {
        return this.proxy;
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
        return Platform.Type.VELOCITY;
    }

    @Override
    public String getServerBrand() {
        return this.proxy.getVersion().getName();
    }

    @Override
    public String getServerVersion() {
        return this.proxy.getVersion().getVersion();
    }

    @Override
    public Path getDataDirectory() {
        return this.configDirectory.toAbsolutePath();
    }

    @Override
    public InputStream getResourceStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    @Override
    public Optional<Player> getPlayer(UUID uniqueId) {
        return this.proxy.getPlayer(uniqueId);
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
        return this.proxy.getPlayerCount();
    }

    @Override
    public Collection<String> getPlayerList() {
        Collection<Player> players = this.proxy.getAllPlayers();
        List<String> list = new ArrayList<>(players.size());
        for (Player player : players) {
            list.add(player.getUsername());
        }
        return list;
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        Collection<Player> players = this.proxy.getAllPlayers();
        List<UUID> list = new ArrayList<>(players.size());
        for (Player player : players) {
            list.add(player.getUniqueId());
        }
        return list;
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
        Player player = this.proxy.getPlayer(uniqueId).orElse(null);
        return player != null && player.isActive();
    }
}
