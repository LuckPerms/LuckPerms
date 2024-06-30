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

package me.lucko.luckperms.common.api;

import me.lucko.luckperms.common.api.implementation.ApiActionFilterFactory;
import me.lucko.luckperms.common.api.implementation.ApiActionLogger;
import me.lucko.luckperms.common.api.implementation.ApiContextManager;
import me.lucko.luckperms.common.api.implementation.ApiGroupManager;
import me.lucko.luckperms.common.api.implementation.ApiMessagingService;
import me.lucko.luckperms.common.api.implementation.ApiMetaStackFactory;
import me.lucko.luckperms.common.api.implementation.ApiNodeBuilderRegistry;
import me.lucko.luckperms.common.api.implementation.ApiNodeMatcherFactory;
import me.lucko.luckperms.common.api.implementation.ApiPlatform;
import me.lucko.luckperms.common.api.implementation.ApiPlayerAdapter;
import me.lucko.luckperms.common.api.implementation.ApiQueryOptionsRegistry;
import me.lucko.luckperms.common.api.implementation.ApiTrackManager;
import me.lucko.luckperms.common.api.implementation.ApiUserManager;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.messaging.LuckPermsMessagingService;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.bootstrap.BootstrappedWithLoader;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.actionlog.ActionLogger;
import net.luckperms.api.actionlog.filter.ActionFilterFactory;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.messaging.MessagingService;
import net.luckperms.api.messenger.MessengerProvider;
import net.luckperms.api.metastacking.MetaStackFactory;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.NodeBuilderRegistry;
import net.luckperms.api.node.matcher.NodeMatcherFactory;
import net.luckperms.api.platform.Health;
import net.luckperms.api.platform.Platform;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.platform.PluginMetadata;
import net.luckperms.api.query.QueryOptionsRegistry;
import net.luckperms.api.track.TrackManager;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Implements the LuckPerms API using the plugin instance
 */
public class LuckPermsApiProvider implements LuckPerms {

    private final LuckPermsPlugin plugin;

    private final ApiPlatform platform;
    private final UserManager userManager;
    private final GroupManager groupManager;
    private final TrackManager trackManager;
    private final PlayerAdapter<?> playerAdapter;
    private final ActionLogger actionLogger;
    private final ContextManager contextManager;
    private final MetaStackFactory metaStackFactory;

    public LuckPermsApiProvider(LuckPermsPlugin plugin) {
        this.plugin = plugin;

        this.platform = new ApiPlatform(plugin);
        this.userManager = new ApiUserManager(plugin, plugin.getUserManager());
        this.groupManager = new ApiGroupManager(plugin, plugin.getGroupManager());
        this.trackManager = new ApiTrackManager(plugin, plugin.getTrackManager());
        this.playerAdapter = new ApiPlayerAdapter<>(plugin.getUserManager(), plugin.getContextManager());
        this.actionLogger = new ApiActionLogger(plugin);
        this.contextManager = new ApiContextManager(plugin, plugin.getContextManager());
        this.metaStackFactory = new ApiMetaStackFactory(plugin);
    }

    public void ensureApiWasLoadedByPlugin() {
        LuckPermsBootstrap bootstrap = this.plugin.getBootstrap();
        ClassLoader pluginClassLoader;
        if (bootstrap instanceof BootstrappedWithLoader) {
            pluginClassLoader = ((BootstrappedWithLoader) bootstrap).getLoader().getClass().getClassLoader();
        } else {
            pluginClassLoader = bootstrap.getClass().getClassLoader();
        }

        for (Class<?> apiClass : new Class[]{LuckPerms.class, LuckPermsProvider.class}) {
            ClassLoader apiClassLoader = apiClass.getClassLoader();

            if (!apiClassLoader.equals(pluginClassLoader)) {
                String guilty = "unknown";
                try {
                    guilty = bootstrap.identifyClassLoader(apiClassLoader);
                } catch (Exception e) {
                    // ignore
                }

                PluginLogger logger = this.plugin.getLogger();
                logger.warn("It seems that the LuckPerms API has been (class)loaded by a plugin other than LuckPerms!");
                logger.warn("The API was loaded by " + apiClassLoader + " (" + guilty + ") and the " +
                        "LuckPerms plugin was loaded by " + pluginClassLoader.toString() + ".");
                logger.warn("This indicates that the other plugin has incorrectly \"shaded\" the " +
                        "LuckPerms API into its jar file. This can cause errors at runtime and should be fixed.");
                return;
            }
        }
    }

    @Override
    public @NonNull String getServerName() {
        return this.plugin.getConfiguration().get(ConfigKeys.SERVER);
    }

    @Override
    public @NonNull Platform getPlatform() {
        return this.platform;
    }

    @Override
    public @NonNull PluginMetadata getPluginMetadata() {
        return this.platform;
    }

    @Override
    public @NonNull UserManager getUserManager() {
        return this.userManager;
    }

    @Override
    public @NonNull GroupManager getGroupManager() {
        return this.groupManager;
    }

    @Override
    public @NonNull TrackManager getTrackManager() {
        return this.trackManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> @NonNull PlayerAdapter<T> getPlayerAdapter(@NonNull Class<T> playerClass) {
        Objects.requireNonNull(playerClass, "playerClass");
        Class<?> expectedClass = this.plugin.getContextManager().getPlayerClass();
        if (!expectedClass.equals(playerClass)) {
            throw new IllegalArgumentException("Player class " + playerClass.getName() + " does not equal " + expectedClass.getName());
        }
        return (PlayerAdapter<T>) this.playerAdapter;
    }

    @Override
    public @NonNull CompletableFuture<Void> runUpdateTask() {
        return this.plugin.getSyncTaskBuffer().request();
    }

    @Override
    public @NonNull Health runHealthCheck() {
        return this.plugin.runHealthCheck();
    }

    @Override
    public @NonNull AbstractEventBus<?> getEventBus() {
        return this.plugin.getEventDispatcher().getEventBus();
    }

    @Override
    public @NonNull Optional<MessagingService> getMessagingService() {
        return this.plugin.getMessagingService().map(ApiMessagingService::new);
    }

    @Override
    public void registerMessengerProvider(@NonNull MessengerProvider messengerProvider) {
        if (this.plugin.getConfiguration().get(ConfigKeys.MESSAGING_SERVICE).equals("custom")) {
            this.plugin.setMessagingService(new LuckPermsMessagingService(this.plugin, messengerProvider));
        }
    }

    @Override
    public @NonNull ActionLogger getActionLogger() {
        return this.actionLogger;
    }

    @Override
    public @NonNull ContextManager getContextManager() {
        return this.contextManager;
    }

    @Override
    public @NonNull NodeBuilderRegistry getNodeBuilderRegistry() {
        return ApiNodeBuilderRegistry.INSTANCE;
    }

    @Override
    public @NonNull QueryOptionsRegistry getQueryOptionsRegistry() {
        return ApiQueryOptionsRegistry.INSTANCE;
    }

    @Override
    public @NonNull MetaStackFactory getMetaStackFactory() {
        return this.metaStackFactory;
    }

    @Override
    public @NonNull NodeMatcherFactory getNodeMatcherFactory() {
        return ApiNodeMatcherFactory.INSTANCE;
    }

    @Override
    public @NonNull ActionFilterFactory getActionFilterFactory() {
        return ApiActionFilterFactory.INSTANCE;
    }
}
