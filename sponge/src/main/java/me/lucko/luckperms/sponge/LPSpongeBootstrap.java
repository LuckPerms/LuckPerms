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

package me.lucko.luckperms.sponge;

import com.google.inject.Inject;

import me.lucko.luckperms.common.dependencies.classloader.PluginClassLoader;
import me.lucko.luckperms.common.dependencies.classloader.ReflectionClassLoader;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.common.plugin.logging.Slf4jPluginLogger;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.util.MoreFiles;

import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.scheduler.AsynchronousExecutor;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.scheduler.SynchronousExecutor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Bootstrap plugin for LuckPerms running on Sponge.
 */
@Plugin(
        id = "luckperms",
        name = "LuckPerms",
        version = "@version@",
        authors = "Luck",
        description = "A permissions plugin",
        url = "https://luckperms.net",
        dependencies = {
                // explicit dependency on spongeapi with no defined API version
                @Dependency(id = "spongeapi")
        }
)
public class LPSpongeBootstrap implements LuckPermsBootstrap {

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
    private final LPSpongePlugin plugin;

    /**
     * The time when the plugin was enabled
     */
    private Instant startTime;

    // load/enable latches
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);

    /**
     * Reference to the central {@link Game} instance in the API
     */
    @Inject
    private Game game;

    /**
     * Reference to the sponge scheduler
     */
    private final Scheduler spongeScheduler;

    /**
     * Injected configuration directory for the plugin
     */
    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDirectory;

    /**
     * Injected plugin container for the plugin
     */
    @Inject
    private PluginContainer pluginContainer;

    @Inject
    public LPSpongeBootstrap(Logger logger, @SynchronousExecutor SpongeExecutorService syncExecutor, @AsynchronousExecutor SpongeExecutorService asyncExecutor) {
        this.logger = new Slf4jPluginLogger(logger);
        this.spongeScheduler = Sponge.getScheduler();
        this.schedulerAdapter = new SpongeSchedulerAdapter(this, this.spongeScheduler, syncExecutor, asyncExecutor);
        this.classLoader = new ReflectionClassLoader(this);
        this.plugin = new LPSpongePlugin(this);
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

    @Listener(order = Order.FIRST)
    public void onEnable(GamePreInitializationEvent event) {
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

    @Listener(order = Order.LATE)
    public void onLateEnable(GamePreInitializationEvent event) {
        this.plugin.lateEnable();
    }

    @Listener
    public void onDisable(GameStoppingServerEvent event) {
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

    // getters for the injected sponge instances

    public Game getGame() {
        return this.game;
    }

    public Optional<Server> getServer() {
        return this.game.isServerAvailable() ? Optional.of(this.game.getServer()) : Optional.empty();
    }

    public Scheduler getSpongeScheduler() {
        return this.spongeScheduler;
    }

    public PluginContainer getPluginContainer() {
        return this.pluginContainer;
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
    public net.luckperms.api.platform.Platform.Type getType() {
        return net.luckperms.api.platform.Platform.Type.SPONGE;
    }

    @Override
    public net.luckperms.api.platform.Platform.Environment getEnvironment() {
        return this.game.getPlatform().getType().isClient() ? net.luckperms.api.platform.Platform.Environment.CLIENT : net.luckperms.api.platform.Platform.Environment.SERVER;
    }

    @Override
    public String getServerBrand() {
        return this.game.getPlatform().getContainer(Platform.Component.IMPLEMENTATION).getName();
    }

    @Override
    public String getServerVersion() {
        PluginContainer api = this.game.getPlatform().getContainer(Platform.Component.API);
        PluginContainer impl = this.game.getPlatform().getContainer(Platform.Component.IMPLEMENTATION);
        return api.getName() + ": " + api.getVersion().orElse("null") + " - " + impl.getName() + ": " + impl.getVersion().orElse("null");
    }
    
    @Override
    public Path getDataDirectory() {
        Path dataDirectory = this.game.getGameDirectory().toAbsolutePath().resolve("luckperms");
        try {
            MoreFiles.createDirectoriesIfNotExists(dataDirectory);
        } catch (IOException e) {
            this.logger.warn("Unable to create LuckPerms directory", e);
        }
        return dataDirectory;
    }

    @Override
    public Path getConfigDirectory() {
        return this.configDirectory.toAbsolutePath();
    }

    @Override
    public InputStream getResourceStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    @Override
    public Optional<Player> getPlayer(UUID uniqueId) {
        return getServer().flatMap(s -> s.getPlayer(uniqueId));
    }

    @Override
    public Optional<UUID> lookupUniqueId(String username) {
        return getServer().flatMap(server -> server.getGameProfileManager().get(username)
                .thenApply(p -> Optional.of(p.getUniqueId()))
                .exceptionally(x -> Optional.empty())
                .join()
        );
    }

    @Override
    public Optional<String> lookupUsername(UUID uniqueId) {
        return getServer().flatMap(server -> server.getGameProfileManager().get(uniqueId)
                .thenApply(GameProfile::getName)
                .exceptionally(x -> Optional.empty())
                .join()
        );
    }

    @Override
    public int getPlayerCount() {
        return getServer().map(server -> server.getOnlinePlayers().size()).orElse(0);
    }

    @Override
    public Collection<String> getPlayerList() {
        return getServer().map(server -> {
            Collection<Player> players = server.getOnlinePlayers();
            List<String> list = new ArrayList<>(players.size());
            for (Player player : players) {
                list.add(player.getName());
            }
            return list;
        }).orElse(Collections.emptyList());
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        return getServer().map(server -> {
            Collection<Player> players = server.getOnlinePlayers();
            List<UUID> list = new ArrayList<>(players.size());
            for (Player player : players) {
                list.add(player.getUniqueId());
            }
            return list;
        }).orElse(Collections.emptyList());
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
        return getServer().flatMap(server -> server.getPlayer(uniqueId).map(Player::isOnline)).orElse(false);
    }
    
}
