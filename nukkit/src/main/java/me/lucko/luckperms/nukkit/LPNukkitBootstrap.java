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

package me.lucko.luckperms.nukkit;

import me.lucko.luckperms.common.dependencies.classloader.PluginClassLoader;
import me.lucko.luckperms.common.dependencies.classloader.ReflectionClassLoader;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;

import net.luckperms.api.platform.Platform;

import cn.nukkit.Player;
import cn.nukkit.plugin.PluginBase;

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
 * Bootstrap plugin for LuckPerms running on Nukkit.
 */
public class LPNukkitBootstrap extends PluginBase implements LuckPermsBootstrap {

    /**
     * The plugin logger
     */
    private PluginLogger logger = null;

    /**
     * A scheduler adapter for the platform
     */
    private final NukkitSchedulerAdapter schedulerAdapter;

    /**
     * The plugin classloader
     */
    private final PluginClassLoader classLoader;

    /**
     * The plugin instance
     */
    private final LPNukkitPlugin plugin;

    /**
     * The time when the plugin was enabled
     */
    private Instant startTime;

    // load/enable latches
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);

    public LPNukkitBootstrap() {
        this.schedulerAdapter = new NukkitSchedulerAdapter(this);
        this.classLoader = new ReflectionClassLoader(this);
        this.plugin = new LPNukkitPlugin(this);
    }

    // provide adapters

    @Override
    public PluginLogger getPluginLogger() {
        if (this.logger == null) {
            throw new IllegalStateException("Logger has not been initialised yet");
        }
        return this.logger;
    }

    @Override
    public NukkitSchedulerAdapter getScheduler() {
        return this.schedulerAdapter;
    }

    @Override
    public PluginClassLoader getPluginClassLoader() {
        return this.classLoader;
    }

    // lifecycle

    @Override
    public void onLoad() {
        this.logger = new NukkitPluginLogger(getLogger());

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
        return getDescription().getVersion();
    }

    @Override
    public Instant getStartupTime() {
        return this.startTime;
    }

    // provide information about the platform

    @Override
    public Platform.Type getType() {
        return Platform.Type.NUKKIT;
    }

    @Override
    public String getServerBrand() {
        return getServer().getName();
    }

    @Override
    public String getServerVersion() {
        return getServer().getVersion() + " - " + getServer().getNukkitVersion();
    }

    @Override
    public Path getDataDirectory() {
        return getDataFolder().toPath().toAbsolutePath();
    }

    @Override
    public InputStream getResourceStream(String path) {
        return getResource(path);
    }

    @Override
    public Optional<Player> getPlayer(UUID uniqueId) {
        return Optional.ofNullable(getServer().getOnlinePlayers().get(uniqueId));
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
        return getServer().getOnlinePlayers().size();
    }

    @Override
    public Collection<String> getPlayerList() {
        Collection<Player> players = getServer().getOnlinePlayers().values();
        List<String> list = new ArrayList<>(players.size());
        for (Player player : players) {
            list.add(player.getName());
        }
        return list;
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        return new ArrayList<>(getServer().getOnlinePlayers().keySet());
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
        Player player = getServer().getOnlinePlayers().get(uniqueId);
        return player != null && player.isOnline();
    }
}
