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

package me.lucko.luckperms.fabric;

import com.mojang.authlib.GameProfile;

import me.lucko.luckperms.common.dependencies.classloader.PluginClassLoader;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.logging.Log4jPluginLogger;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.luckperms.api.platform.Platform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
 * Bootstrap plugin for LuckPerms running on Fabric.
 */
public final class LPFabricBootstrap implements LuckPermsBootstrap, DedicatedServerModInitializer {

    private static final String MODID = "luckperms";
    private static final ModContainer MOD_CONTAINER = FabricLoader.getInstance().getModContainer(MODID)
            .orElseThrow(() -> new RuntimeException("Could not get the LuckPerms mod container."));

    /**
     * The plugin logger
     */
    private final PluginLogger logger;

    /**
     * A scheduler adapter for the platform
     */
    private final SchedulerAdapter schedulerAdapter;

    /**
     * The plugin class loader.
     */
    private final PluginClassLoader classLoader;

    /**
     * The plugin instance
     */
    private LPFabricPlugin plugin;

    /**
     * The time when the plugin was enabled
     */
    private Instant startTime;

    // load/enable latches
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);

    /**
     * The Minecraft server instance
     */
    private MinecraftServer server;
    
    public LPFabricBootstrap() {
        this.logger = new Log4jPluginLogger(LogManager.getLogger(MODID));
        this.schedulerAdapter = new FabricSchedulerAdapter(this);
        this.classLoader = new FabricClassLoader();
        this.plugin = new LPFabricPlugin(this);
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

    @Override
    public void onInitializeServer() {
        this.plugin = new LPFabricPlugin(this);
        try {
            this.plugin.load();
        } finally {
            this.loadLatch.countDown();
        }

        // Register the Server startup/shutdown events now
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        this.plugin.registerFabricListeners();
    }

    private void onServerStarting(MinecraftServer server) {
        this.server = server;
        this.startTime = Instant.now();
        this.plugin.enable();
    }

    private void onServerStopping(MinecraftServer server) {
        this.plugin.disable();
        this.server = null;
    }

    @Override
    public CountDownLatch getLoadLatch() {
        return this.loadLatch;
    }

    @Override
    public CountDownLatch getEnableLatch() {
        return this.enableLatch;
    }

    // MinecraftServer singleton getter

    public Optional<MinecraftServer> getServer() {
        return Optional.ofNullable(this.server);
    }

    // provide information about the plugin

    @Override
    public String getVersion() {
        return MOD_CONTAINER.getMetadata().getVersion().getFriendlyString();
    }

    @Override
    public Instant getStartupTime() {
        return this.startTime;
    }

    // provide information about the platform

    @Override
    public Platform.Type getType() {
        return Platform.Type.FABRIC;
    }

    @Override
    public String getServerBrand() {
        String fabricVersion = FabricLoader.getInstance().getModContainer("fabric")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");

        return "fabric@" + fabricVersion;
    }

    @Override
    public String getServerVersion() {
        String fabricApiVersion = FabricLoader.getInstance().getModContainer("fabric-api-base")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");

        return getServer().map(MinecraftServer::getVersion).orElse("null") + " - fabric-api@" + fabricApiVersion;
    }

    @Override
    public Path getDataDirectory() {
        return FabricLoader.getInstance().getGameDir().resolve("mods").resolve(MODID);
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve(MODID);
    }

    @Override
    public InputStream getResourceStream(String path) {
        try {
            return Files.newInputStream(LPFabricBootstrap.MOD_CONTAINER.getPath(path));
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Optional<ServerPlayerEntity> getPlayer(UUID uniqueId) {
        return getServer().map(MinecraftServer::getPlayerManager).map(s -> s.getPlayer(uniqueId));
    }

    @Override
    public Optional<UUID> lookupUniqueId(String username) {
        return getServer().map(MinecraftServer::getUserCache).map(c -> c.findByName(username)).map(GameProfile::getId);

    }

    @Override
    public Optional<String> lookupUsername(UUID uniqueId) {
        return getServer().map(MinecraftServer::getUserCache).map(c -> c.getByUuid(uniqueId)).map(GameProfile::getName);
    }

    @Override
    public int getPlayerCount() {
        return getServer().map(MinecraftServer::getCurrentPlayerCount).orElse(0);
    }

    @Override
    public Collection<String> getPlayerList() {
        return getServer().map(MinecraftServer::getPlayerManager)
                .map(server -> {
                    List<ServerPlayerEntity> players = server.getPlayerList();
                    List<String> list = new ArrayList<>(players.size());
                    for (ServerPlayerEntity player : players) {
                        list.add(player.getGameProfile().getName());
                    }
                    return list;
                })
                .orElse(Collections.emptyList());
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        return getServer().map(MinecraftServer::getPlayerManager)
                .map(server -> {
                    List<ServerPlayerEntity> players = server.getPlayerList();
                    List<UUID> list = new ArrayList<>(players.size());
                    for (ServerPlayerEntity player : players) {
                        list.add(player.getGameProfile().getId());
                    }
                    return list;
                })
                .orElse(Collections.emptyList());
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
        return getServer().map(MinecraftServer::getPlayerManager).map(s -> s.getPlayer(uniqueId) != null).orElse(false);
    }

}
