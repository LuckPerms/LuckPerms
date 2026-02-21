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

import me.lucko.luckperms.common.minecraft.MinecraftLuckPermsBootstrap;
import me.lucko.luckperms.common.minecraft.MinecraftSchedulerAdapter;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;
import me.lucko.luckperms.common.plugin.logging.Log4jPluginLogger;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.luckperms.api.platform.Platform;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/**
 * Bootstrap plugin for LuckPerms running on Fabric.
 */
public final class LPFabricBootstrap extends MinecraftLuckPermsBootstrap implements LuckPermsBootstrap, DedicatedServerModInitializer {

    private static final String MODID = "luckperms";

    /**
     * The mod container
     */
    private final ModContainer modContainer;

    /**
     * The plugin logger
     */
    private final PluginLogger logger;

    /**
     * A scheduler adapter for the platform
     */
    private final MinecraftSchedulerAdapter schedulerAdapter;

    /**
     * The plugin class path appender
     */
    private final ClassPathAppender classPathAppender;

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
        this.modContainer = FabricLoader.getInstance().getModContainer(MODID)
                .orElseThrow(() -> new RuntimeException("Could not get the LuckPerms mod container."));
        this.logger = new Log4jPluginLogger(LogManager.getLogger(MODID));
        this.schedulerAdapter = new MinecraftSchedulerAdapter(this);
        this.classPathAppender = new FabricClassPathAppender();
        this.plugin = new LPFabricPlugin(this);
    }
    
    // provide adapters

    @Override
    public PluginLogger getPluginLogger() {
        return this.logger;
    }

    @Override
    public MinecraftSchedulerAdapter getScheduler() {
        return this.schedulerAdapter;
    }

    @Override
    public ClassPathAppender getClassPathAppender() {
        return this.classPathAppender;
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

    @Override
    public Optional<MinecraftServer> getServer() {
        return Optional.ofNullable(this.server);
    }

    // provide information about the plugin

    @Override
    public String getVersion() {
        return this.modContainer.getMetadata().getVersion().getFriendlyString();
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

        return getServer().map(MinecraftServer::getServerVersion).orElse("null") + " - fabric-api@" + fabricApiVersion;
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
            return Files.newInputStream(this.modContainer.getPath(path));
        } catch (IOException e) {
            return null;
        }
    }



}
