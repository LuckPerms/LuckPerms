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

package me.lucko.luckperms.forge;

import com.mojang.authlib.GameProfile;
import me.lucko.luckperms.common.loader.LoaderBootstrap;
import me.lucko.luckperms.common.plugin.bootstrap.BootstrappedWithLoader;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;
import me.lucko.luckperms.common.plugin.classpath.JarInJarClassPathAppender;
import me.lucko.luckperms.common.plugin.logging.Log4jPluginLogger;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import net.luckperms.api.platform.Platform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.maven.artifact.versioning.ArtifactVersion;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

/**
 * Bootstrap plugin for LuckPerms running on Forge.
 */
public final class LPForgeBootstrap implements LuckPermsBootstrap, LoaderBootstrap, BootstrappedWithLoader {
    public static final String ID = "luckperms";

    /**
     * The plugin loader
     */
    private final Supplier<ModContainer> loader;

    /**
     * The plugin logger
     */
    private final PluginLogger logger;

    /**
     * A scheduler adapter for the platform
     */
    private final ForgeSchedulerAdapter schedulerAdapter;

    /**
     * The plugin class path appender
     */
    private final ClassPathAppender classPathAppender;

    /**
     * A list of all event listerners registered by the plugin.
     */
    private final List<EventListener> listeners = new ArrayList<>();

    /**
     * The plugin instance
     */
    private final LPForgePlugin plugin;

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

    public LPForgeBootstrap(Supplier<ModContainer> loader) {
        this.loader = loader;
        this.logger = new Log4jPluginLogger(LogManager.getLogger(LPForgeBootstrap.ID));
        this.schedulerAdapter = new ForgeSchedulerAdapter(this);
        this.classPathAppender = new JarInJarClassPathAppender(getClass().getClassLoader());
        this.plugin = new LPForgePlugin(this);
    }

    // provide adapters

    @Override
    public Object getLoader() {
        return this.loader;
    }

    @Override
    public PluginLogger getPluginLogger() {
        return this.logger;
    }

    @Override
    public ForgeSchedulerAdapter getScheduler() {
        return this.schedulerAdapter;
    }

    @Override
    public ClassPathAppender getClassPathAppender() {
        return this.classPathAppender;
    }

    public void registerListeners(Object target) {
        Collection<EventListener> listeners = BusGroup.DEFAULT.register(MethodHandles.lookup(), target);
        this.listeners.addAll(listeners);
    }

    public void unregisterListeners() {
        if (this.listeners.isEmpty()) {
            return;
        }
        BusGroup.DEFAULT.unregister(this.listeners);
        this.listeners.clear();
    }

    // lifecycle

    @Override
    public void onLoad() { // called by the loader on FMLCommonSetupEvent
        this.startTime = Instant.now();
        try {
            this.plugin.load();
        } finally {
            this.loadLatch.countDown();
        }

        registerListeners(this);
        this.plugin.registerEarlyListeners();
    }

    @SubscribeEvent(priority = Priority.HIGHEST)
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        this.server = event.getServer();
        try {
            this.plugin.enable();
        } finally {
            this.enableLatch.countDown();
        }
    }

    @SubscribeEvent(priority = Priority.LOWEST)
    public void onServerStopping(ServerStoppingEvent event) {
        this.plugin.disable();
        unregisterListeners();
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
        return "@version@";
    }

    @Override
    public Instant getStartupTime() {
        return this.startTime;
    }

    // provide information about the platform

    @Override
    public Platform.Type getType() {
        return Platform.Type.FORGE;
    }

    @Override
    public String getServerBrand() {
        return ModList.get().getModContainerById("forge")
                .map(ModContainer::getModInfo)
                .map(IModInfo::getDisplayName)
                .orElse("null");
    }

    @Override
    public String getServerVersion() {
        String forgeVersion = ModList.get().getModContainerById("forge")
                .map(ModContainer::getModInfo)
                .map(IModInfo::getVersion)
                .map(ArtifactVersion::toString)
                .orElse("null");

        return getServer().map(MinecraftServer::getServerVersion).orElse("null") + "-" + forgeVersion;
    }

    @Override
    public Path getDataDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve(LPForgeBootstrap.ID).toAbsolutePath();
    }

    @Override
    public Optional<ServerPlayer> getPlayer(UUID uniqueId) {
        return getServer().map(MinecraftServer::getPlayerList).map(playerList -> playerList.getPlayer(uniqueId));
    }

    @Override
    public Optional<UUID> lookupUniqueId(String username) {
        return getServer().map(MinecraftServer::getProfileCache).flatMap(profileCache -> profileCache.get(username)).map(GameProfile::getId);
    }

    @Override
    public Optional<String> lookupUsername(UUID uniqueId) {
        return getServer().map(MinecraftServer::getProfileCache).flatMap(profileCache -> profileCache.get(uniqueId)).map(GameProfile::getName);
    }

    @Override
    public int getPlayerCount() {
        return getServer().map(MinecraftServer::getPlayerCount).orElse(0);
    }

    @Override
    public Collection<String> getPlayerList() {
        return getServer().map(MinecraftServer::getPlayerList).map(PlayerList::getPlayers).map(players -> {
            List<String> list = new ArrayList<>(players.size());
            for (ServerPlayer player : players) {
                list.add(player.getGameProfile().getName());
            }
            return list;
        }).orElse(Collections.emptyList());
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        return getServer().map(MinecraftServer::getPlayerList).map(PlayerList::getPlayers).map(players -> {
            List<UUID> list = new ArrayList<>(players.size());
            for (ServerPlayer player : players) {
                list.add(player.getGameProfile().getId());
            }
            return list;
        }).orElse(Collections.emptyList());
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
        return getServer().map(MinecraftServer::getPlayerList).map(playerList -> playerList.getPlayer(uniqueId)).isPresent();
    }

}
