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

import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.hypixel.hytale.server.core.permissions.provider.PermissionProvider;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.Universe;
import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.dependencies.DependencyManager;
import me.lucko.luckperms.common.dependencies.DependencyManagerImpl;
import me.lucko.luckperms.common.dependencies.DependencyRepository;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager;
import me.lucko.luckperms.common.model.manager.track.StandardTrackManager;
import me.lucko.luckperms.common.model.manager.user.StandardUserManager;
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.hytale.calculator.HytaleCalculatorFactory;
import me.lucko.luckperms.hytale.calculator.virtualgroups.VirtualGroupsLookupProvider;
import me.lucko.luckperms.hytale.chat.LuckPermsChatFormatter;
import me.lucko.luckperms.hytale.context.HytaleContextManager;
import me.lucko.luckperms.hytale.context.HytalePlayerCalculator;
import me.lucko.luckperms.hytale.listeners.HytaleConnectionListener;
import me.lucko.luckperms.hytale.listeners.HytalePlatformListener;
import me.lucko.luckperms.hytale.service.LuckPermsPermissionProvider;
import me.lucko.luckperms.hytale.service.PlayerVirtualGroupsMap;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.query.QueryOptions;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * LuckPerms implementation for Hytale.
 */
public class LPHytalePlugin extends AbstractLuckPermsPlugin {
    private final LPHytaleBootstrap bootstrap;

    private HytaleSenderFactory senderFactory;
    private HytaleConnectionListener connectionListener;
    private HytaleCommandManager commandManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private HytaleContextManager contextManager;

    private PlayerVirtualGroupsMap playerVirtualGroupsMap;
    private VirtualGroupsLookupProvider virtualGroupsLookupProvider;
    private LuckPermsPermissionProvider luckPermsPermissionProvider;

    public LPHytalePlugin(LPHytaleBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public LPHytaleBootstrap getBootstrap() {
        return this.bootstrap;
    }

    public JavaPlugin getLoader() {
        return this.bootstrap.getLoader();
    }

    @Override
    protected DependencyManager createDependencyManager() {
        boolean loadDepsFromJar = false;

        JavaPlugin loader = this.bootstrap.getLoader();
        try {
            Field loadDepsFromJarField = loader.getClass().getField("LOAD_DEPS_FROM_JAR");
            loadDepsFromJar = loadDepsFromJarField.getBoolean(loader);
        } catch (Exception e) {
            // ignore
        }

        if (loadDepsFromJar) {
            getLogger().info("Will load dependencies from local jar (jar-in-jar)");
            return new DependencyManagerImpl(this, List.of(DependencyRepository.JAR_IN_JAR));
        }

        return super.createDependencyManager();
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new HytaleSenderFactory(this);
    }

    @Override
    protected Set<Dependency> getGlobalDependencies() {
        Set<Dependency> dependencies = super.getGlobalDependencies();
        // required for loading the LP config
        dependencies.add(Dependency.CONFIGURATE_CORE);
        dependencies.add(Dependency.CONFIGURATE_YAML);
        dependencies.add(Dependency.SNAKEYAML);
        return dependencies;
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new HytaleConfigAdapter(this, resolveConfig("config.yml"));
    }

    @Override
    protected void registerPlatformListeners() {
        this.connectionListener = new HytaleConnectionListener(this);
        this.connectionListener.register(this.bootstrap.getLoader().getEventRegistry());
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new MessagingFactory<>(this);
    }

    @Override
    protected void registerCommands() {
        this.commandManager = new HytaleCommandManager(this);
        this.commandManager.register();
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        this.virtualGroupsLookupProvider = new VirtualGroupsLookupProvider();
        return new HytaleCalculatorFactory(this, this.virtualGroupsLookupProvider);
    }

    @Override
    protected void setupContextManager() {
        this.playerVirtualGroupsMap = new PlayerVirtualGroupsMap();
        this.contextManager = new HytaleContextManager(this, this.playerVirtualGroupsMap);

        HytalePlayerCalculator playerCalculator = new HytalePlayerCalculator(this, getConfiguration().get(ConfigKeys.DISABLED_CONTEXTS));
        playerCalculator.registerEvents(this.bootstrap.getLoader().getEventRegistry());
        playerCalculator.registerSystems(this.bootstrap.getLoader().getEntityStoreRegistry());
        this.contextManager.registerCalculator(playerCalculator);
    }

    @Override
    protected void setupPlatformHooks() {
        // permissions
        PermissionsModule permissionsModule = PermissionsModule.get();

        // find the hytale provider
        HytalePermissionsProvider hytaleProvider = null;
        for (PermissionProvider provider : permissionsModule.getProviders()) {
            if (provider instanceof HytalePermissionsProvider hpp) {
                hytaleProvider = hpp;
                break;
            }
        }

        // register our provider
        this.luckPermsPermissionProvider = new LuckPermsPermissionProvider(this, hytaleProvider, this.playerVirtualGroupsMap);
        permissionsModule.addProvider(this.luckPermsPermissionProvider);

        // remove all other providers
        for (PermissionProvider provider : permissionsModule.getProviders()) {
            if (provider != this.luckPermsPermissionProvider) {
                permissionsModule.removeProvider(provider);
            }
        }

        // chat
        if (getConfiguration().get(ConfigKeys.CHAT_FORMATTER_ENABLED)) {
            LuckPermsChatFormatter chatFormatter = new LuckPermsChatFormatter(this);
            chatFormatter.register(this.bootstrap.getLoader().getEventRegistry());
        }

        // general
        HytalePlatformListener platformListener = new HytalePlatformListener(this);
        platformListener.register(this.bootstrap.getLoader().getEventRegistry());
    }

    @Override
    protected void removePlatformHooks() {
        PermissionsModule permissionsModule = PermissionsModule.get();
        HytalePermissionsProvider hytaleProvider = this.luckPermsPermissionProvider.getHytaleProvider();
        if (hytaleProvider != null) {
            permissionsModule.addProvider(hytaleProvider);
        }
        permissionsModule.removeProvider(this.luckPermsPermissionProvider);
    }

    @Override
    protected AbstractEventBus<?> provideEventBus(LuckPermsApiProvider apiProvider) {
        return new HytaleEventBus(this, apiProvider);
    }

    @Override
    protected void registerApiOnPlatform(LuckPerms api) {

    }

    @Override
    protected void performFinalSetup() {

    }

    @Override
    public Optional<QueryOptions> getQueryOptionsForUser(User user) {
        return this.bootstrap.getPlayer(user.getUniqueId()).map(player -> this.contextManager.getQueryOptions(player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        return Stream.concat(
                Stream.of(getConsoleSender()),
                Universe.get().getPlayers().stream().map(p -> getSenderFactory().wrap(p))
        );
    }

    @Override
    public Sender getConsoleSender() {
        return getSenderFactory().wrap(ConsoleSender.INSTANCE);
    }

    public HytaleSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    public VirtualGroupsLookupProvider getVirtualGroupsLookupProvider() {
        return this.virtualGroupsLookupProvider;
    }

    @Override
    public AbstractConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public HytaleCommandManager getCommandManager() {
        return this.commandManager;
    }

    @Override
    public StandardUserManager getUserManager() {
        return this.userManager;
    }

    @Override
    public StandardGroupManager getGroupManager() {
        return this.groupManager;
    }

    @Override
    public StandardTrackManager getTrackManager() {
        return this.trackManager;
    }

    @Override
    public HytaleContextManager getContextManager() {
        return this.contextManager;
    }

}
