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
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.fabricmc.fabric.api.event.server.ServerStopCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.luckperms.api.platform.Platform;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

/**
 * Since fabric can run on both an integrated server (which belongs to a client) and a dedicated server, we have to leave some details to each platform's environment.
 */
public abstract class AbstractFabricBootstrap implements LuckPermsBootstrap {

    private static final String MODID = "luckperms";
    private static final ModContainer MOD_CONTAINER = FabricLoader.getInstance().getModContainer(MODID)
            .orElseThrow(() -> new RuntimeException("Could not get the LuckPerms mod container. This a bug with fabric loader and should be reported."));

    public AbstractFabricBootstrap() {
        this.classLoader = new FabricClassLoader();
        // fixme: Load deps first
        this.plugin = new LPFabricPlugin(this);
    }

    /**
     * The plugin logger
     */
    private PluginLogger logger = new FabricPluginLogger();

    /**
     * A scheduler adapter for the platform
     */
    private SchedulerAdapter schedulerAdapter;

    /**
     * The plugin class loader.
     */
    private PluginClassLoader classLoader;

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

    @Override
    public CountDownLatch getLoadLatch() {
        return this.loadLatch;
    }

    @Override
    public CountDownLatch getEnableLatch() {
        return this.enableLatch;
    }

    @Override
    public String getVersion() {
        return MOD_CONTAINER.getMetadata().getVersion().getFriendlyString();
    }

    @Override
    public Instant getStartupTime() {
        return this.startTime;
    }

    @Override
    public Platform.Type getType() {
        return Platform.Type.FABRIC;
    }

    @Override
    public String getServerBrand() {
        return this.getServer().getServerModName();
    }

    @Override
    public String getServerVersion() {
        return this.getServer().getVersion();
    }

    @Override
    public Path getDataDirectory() {
        return FabricLoader.getInstance().getGameDirectory().toPath().resolve("mods").resolve("LuckPerms");
    }

    @Override
    public InputStream getResourceStream(String path) {
        return FabricLauncherBase.getLauncher().getResourceAsStream(path);
    }

    @Override
    public Optional<ServerPlayerEntity> getPlayer(UUID uniqueId) {
        return Optional.ofNullable(this.getServer().getPlayerManager().getPlayer(uniqueId));
    }

    @Override
    public Optional<UUID> lookupUniqueId(String username) {
        GameProfile profile = this.getServer().getUserCache().findByName(username);

        if (profile != null && profile.getId() != null) {
            return Optional.of(profile.getId());
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> lookupUsername(UUID uniqueId) {
        GameProfile profile = this.getServer().getUserCache().getByUuid(uniqueId);

        if (profile != null && profile.getId() != null) {
            return Optional.of(profile.getName());
        }

        return Optional.empty();
    }

    @Override
    public int getPlayerCount() {
        return this.getServer().getCurrentPlayerCount();
    }

    @Override
    public Stream<String> getPlayerList() {
        return this.getServer().getPlayerManager().getPlayerList().stream().map(PlayerEntity::getEntityName);
    }

    @Override
    public Stream<UUID> getOnlinePlayers() {
        return this.getServer().getPlayerManager().getPlayerList().stream().map(PlayerEntity::getUuid);
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
        return this.getServer().getPlayerManager().getPlayer(uniqueId) != null;
    }

    /**
     * Starts LuckPerms
     */
    protected final void onInitialize() {
        this.plugin = new LPFabricPlugin(this);
        try {
            this.plugin.load();
        } finally {
            this.loadLatch.countDown();
        }
        // Register the Server startup/shutdown events now
        ServerStartCallback.EVENT.register(this::onStartServer);
        ServerStopCallback.EVENT.register(this::onStopServer);
        this.plugin.setupFabricListeners();
    }

    private void onStartServer(MinecraftServer server) {
        this.startTime = Instant.now();
        // We need to create a new scheduler adapter every time we start the server.
        // This is because an integrated server will shutdown the executor services, which cannot be started back up.
        this.schedulerAdapter = new FabricSchedulerAdapter(this);
        this.plugin.enable();
    }

    private void onStopServer(MinecraftServer server) {
        this.plugin.disable();
    }

    /**
     * Depending on the environment, the server might not always be present on fabric.
     *
     * <p>On the Client, the server is only present when the integrated server (single player) is running.</p>
     *
     * <p>On a dedicated server, the server instance is always available.</p>
     *
     * @return The current server instance.
     * @throws IllegalStateException If the server is not available.
     */
    public abstract MinecraftServer getServer();
}
