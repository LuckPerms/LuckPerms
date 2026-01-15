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

package me.lucko.luckperms.hytale;

import com.hypixel.hytale.common.util.java.ManifestUtil;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.PluginClassLoader;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import me.lucko.luckperms.common.loader.LoaderBootstrap;
import me.lucko.luckperms.common.plugin.bootstrap.BootstrappedWithLoader;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;
import me.lucko.luckperms.common.plugin.classpath.JarInJarClassPathAppender;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import net.luckperms.api.platform.Platform;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Bootstrap plugin for LuckPerms running on Hytale.
 */
public class LPHytaleBootstrap implements LuckPermsBootstrap, LoaderBootstrap, BootstrappedWithLoader {
    private final JavaPlugin loader;

    /**
     * The plugin logger
     */
    private final PluginLogger logger;

    /**
     * A scheduler adapter for the platform
     */
    private final HytaleSchedulerAdapter schedulerAdapter;

    /**
     * The plugin class path appender
     */
    private final JarInJarClassPathAppender classPathAppender;

    /**
     * The plugin instance
     */
    private final LPHytalePlugin plugin;

    /**
     * The time when the plugin was enabled
     */
    private Instant startTime;

    // load/enable latches
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);

    public LPHytaleBootstrap(JavaPlugin loader) {
        this.loader = loader;

        this.logger = new HytalePluginLogger(loader.getLogger());
        this.schedulerAdapter = new HytaleSchedulerAdapter(this);
        this.classPathAppender = new JarInJarClassPathAppender(getClass().getClassLoader());
        this.plugin = new LPHytalePlugin(this);
    }

    // provide adapters

    @Override
    public JavaPlugin getLoader() {
        return this.loader;
    }

    @Override
    public PluginLogger getPluginLogger() {
        return this.logger;
    }

    @Override
    public HytaleSchedulerAdapter getScheduler() {
        return this.schedulerAdapter;
    }

    @Override
    public ClassPathAppender getClassPathAppender() {
        return this.classPathAppender;
    }

    @Override
    public InputStream getResourceStream(String path) {
        // avoid picking up resources in other mods
        URL url = this.classPathAppender.getClassLoader().findResource(path);
        try {
            return url != null ? url.openStream() : null;
        } catch (IOException e) {
            return null;
        }
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
        return this.loader.getManifest().getVersion().toString();
    }

    @Override
    public Instant getStartupTime() {
        return this.startTime;
    }

    // provide information about the platform

    @Override
    public Platform.Type getType() {
        return Platform.Type.HYTALE;
    }

    @Override
    public String getServerBrand() {
        return "Hytale";
    }

    @Override
    public String getServerVersion() {
        return ManifestUtil.getImplementationVersion();
    }

    @Override
    public Path getDataDirectory() {
        return this.loader.getDataDirectory().toAbsolutePath();
    }

    @Override
    public Optional<PlayerRef> getPlayer(UUID uniqueId) {
        return Optional.ofNullable(Universe.get().getPlayer(uniqueId));
    }

    @Override
    public Optional<UUID> lookupUniqueId(String username) {
        return Optional.empty(); // TODO ?
    }

    @Override
    public Optional<String> lookupUsername(UUID uniqueId) {
        return Optional.empty(); // TODO ?
    }

    @Override
    public int getPlayerCount() {
        return Universe.get().getPlayerCount();
    }

    @Override
    public Collection<String> getPlayerList() {
        List<PlayerRef> players = Universe.get().getPlayers();
        List<String> list = new ArrayList<>(players.size());
        for (PlayerRef player : players) {
            list.add(player.getUsername());
        }
        return list;
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        List<PlayerRef> players = Universe.get().getPlayers();
        List<UUID> list = new ArrayList<>(players.size());
        for (PlayerRef player : players) {
            list.add(player.getUuid());
        }
        return list;
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
        return Universe.get().getPlayer(uniqueId) != null;
    }

    @Override
    public @Nullable String identifyClassLoader(ClassLoader classLoader) throws ReflectiveOperationException {
        if (classLoader instanceof PluginClassLoader) {
            Field pluginField = PluginClassLoader.class.getDeclaredField("plugin");
            pluginField.setAccessible(true);

            JavaPlugin plugin = (JavaPlugin) pluginField.get(classLoader);
            return plugin.getName();
        }
        return null;
    }
}
