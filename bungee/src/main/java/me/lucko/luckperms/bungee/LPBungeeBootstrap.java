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

package me.lucko.luckperms.bungee;

import me.lucko.luckperms.bungee.util.RedisBungeeUtil;
import me.lucko.luckperms.common.loader.LoaderBootstrap;
import me.lucko.luckperms.common.plugin.bootstrap.BootstrappedWithLoader;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;
import me.lucko.luckperms.common.plugin.classpath.JarInJarClassPathAppender;
import me.lucko.luckperms.common.plugin.logging.JavaPluginLogger;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import net.luckperms.api.platform.Platform;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

/**
 * Bootstrap plugin for LuckPerms running on BungeeCord.
 */
public class LPBungeeBootstrap implements LuckPermsBootstrap, LoaderBootstrap, BootstrappedWithLoader {
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
    private final LPBungeePlugin plugin;

    /**
     * The time when the plugin was enabled
     */
    private Instant startTime;

    // load/enable latches
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);

    // if the plugin has been loaded on an incompatible version
    private boolean incompatibleVersion = false;

    public LPBungeeBootstrap(Plugin loader) {
        this.loader = loader;

        this.schedulerAdapter = new BungeeSchedulerAdapter(this);
        this.classPathAppender = new JarInJarClassPathAppender(getClass().getClassLoader());
        this.plugin = new LPBungeePlugin(this);
    }

    // provide adapters

    @Override
    public Plugin getLoader() {
        return this.loader;
    }

    public ProxyServer getProxy() {
        return this.loader.getProxy();
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
        this.logger = new JavaPluginLogger(this.loader.getLogger());

        if (checkIncompatibleVersion()) {
            this.incompatibleVersion = true;
            return;
        }

        try {
            this.plugin.load();
        } finally {
            this.loadLatch.countDown();
        }
    }

    @Override
    public void onEnable() {
        if (this.incompatibleVersion) {
            Logger logger = this.loader.getLogger();
            logger.severe("----------------------------------------------------------------------");
            logger.severe("Your proxy version is not compatible with this build of LuckPerms. :(");
            logger.severe("");
            logger.severe("This is most likely because you are using an old/outdated version of BungeeCord.");
            logger.severe("If you need 1.7 support, replace your BungeeCord.jar file with the latest build of");
            logger.severe("'Travertine' from here:");
            logger.severe("==> https://papermc.io/downloads#Travertine");
            logger.severe("");
            logger.severe("The proxy will now shutdown.");
            logger.severe("----------------------------------------------------------------------");
            getProxy().stop();
            return;
        }

        this.startTime = Instant.now();
        try {
            this.plugin.enable();
        } finally {
            this.enableLatch.countDown();
        }
    }

    @Override
    public void onDisable() {
        if (this.incompatibleVersion) {
            return;
        }

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
        return Platform.Type.BUNGEECORD;
    }

    @Override
    public String getServerBrand() {
        return getProxy().getName();
    }

    @Override
    public String getServerVersion() {
        return getProxy().getVersion();
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
        if (getProxy().getPluginManager().getPlugin("RedisBungee") != null) {
            try {
                return RedisBungeeUtil.lookupUuid(username);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> lookupUsername(UUID uniqueId) {
        if (getProxy().getPluginManager().getPlugin("RedisBungee") != null) {
            try {
                return RedisBungeeUtil.lookupUsername(uniqueId);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        return Optional.empty();
    }

    @Override
    public int getPlayerCount() {
        return getProxy().getOnlineCount();
    }

    @Override
    public Collection<String> getPlayerList() {
        Collection<ProxiedPlayer> players = getProxy().getPlayers();
        List<String> list = new ArrayList<>(players.size());
        for (ProxiedPlayer player : players) {
            list.add(player.getName());
        }
        return list;
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        Collection<ProxiedPlayer> players = getProxy().getPlayers();
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

    @Override
    public @Nullable String identifyClassLoader(ClassLoader classLoader) throws Exception {
        Class<?> pluginClassLoader = Class.forName("net.md_5.bungee.api.plugin.PluginClassloader");
        if (pluginClassLoader.isInstance(classLoader)) {
            Field descriptionField = pluginClassLoader.getDeclaredField("desc");
            descriptionField.setAccessible(true);
            
            PluginDescription desc = (PluginDescription) descriptionField.get(classLoader);
            return desc.getName();
        }
        return null;
    }

    private static boolean checkIncompatibleVersion() {
        try {
            Class.forName("com.google.gson.internal.bind.TreeTypeAdapter");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
