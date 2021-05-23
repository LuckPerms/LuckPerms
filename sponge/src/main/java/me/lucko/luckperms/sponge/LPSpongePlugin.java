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

import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.command.abstraction.Command;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.track.StandardTrackManager;
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin;
import me.lucko.luckperms.common.sender.DummyConsoleSender;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.sponge.calculator.SpongeCalculatorFactory;
import me.lucko.luckperms.sponge.commands.SpongeParentCommand;
import me.lucko.luckperms.sponge.context.SpongeContextManager;
import me.lucko.luckperms.sponge.context.SpongePlayerCalculator;
import me.lucko.luckperms.sponge.listeners.SpongeConnectionListener;
import me.lucko.luckperms.sponge.listeners.SpongePlatformListener;
import me.lucko.luckperms.sponge.messaging.SpongeMessagingFactory;
import me.lucko.luckperms.sponge.model.manager.SpongeGroupManager;
import me.lucko.luckperms.sponge.model.manager.SpongeUserManager;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.ProxyFactory;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.model.ProxiedServiceObject;
import me.lucko.luckperms.sponge.service.model.persisted.PersistedCollection;
import me.lucko.luckperms.sponge.tasks.ServiceCacheHousekeepingTask;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.query.QueryOptions;

import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * LuckPerms implementation for the Sponge API.
 */
public class LPSpongePlugin extends AbstractLuckPermsPlugin {
    private final LPSpongeBootstrap bootstrap;

    private SpongeSenderFactory senderFactory;
    private SpongeConnectionListener connectionListener;
    private SpongeCommandExecutor commandManager;
    private SpongeUserManager userManager;
    private SpongeGroupManager groupManager;
    private StandardTrackManager trackManager;
    private SpongeContextManager contextManager;
    private LuckPermsService service;

    private boolean lateLoad = false;

    public LPSpongePlugin(LPSpongeBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public LPSpongeBootstrap getBootstrap() {
        return this.bootstrap;
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new SpongeSenderFactory(this);
    }

    @Override
    protected Set<Dependency> getGlobalDependencies() {
        Set<Dependency> dependencies = super.getGlobalDependencies();
        dependencies.add(Dependency.ADVENTURE_PLATFORM);
        //dependencies.add(Dependency.ADVENTURE_PLATFORM_SPONGEAPI);
        dependencies.add(Dependency.CONFIGURATE_CORE);
        dependencies.add(Dependency.CONFIGURATE_HOCON);
        dependencies.add(Dependency.HOCON_CONFIG);
        return dependencies;
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new SpongeConfigAdapter(this, resolveConfig("luckperms.conf"));
    }

    @Override
    protected void registerPlatformListeners() {
        this.connectionListener = new SpongeConnectionListener(this);
        this.bootstrap.getGame().getEventManager().registerListeners(this.bootstrap, this.connectionListener);
        this.bootstrap.getGame().getEventManager().registerListeners(this.bootstrap, new SpongePlatformListener(this));
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new SpongeMessagingFactory(this);
    }

    @Override
    protected void registerCommands() {
        this.commandManager = new SpongeCommandExecutor(this);
        this.bootstrap.getGame().getCommandManager().register(this.bootstrap, this.commandManager, "luckperms", "lp", "perm", "perms", "permission", "permissions");
    }

    @Override
    protected void setupManagers() {
        this.userManager = new SpongeUserManager(this);
        this.groupManager = new SpongeGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new SpongeCalculatorFactory(this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new SpongeContextManager(this);

        SpongePlayerCalculator playerCalculator = new SpongePlayerCalculator(this, getConfiguration().get(ConfigKeys.DISABLED_CONTEXTS));
        this.bootstrap.getGame().getEventManager().registerListeners(this.bootstrap, playerCalculator);
        this.contextManager.registerCalculator(playerCalculator);
    }

    @Override
    protected void setupPlatformHooks() {
        getLogger().info("Registering PermissionService...");
        this.service = new LuckPermsService(this);

        PermissionService oldService = this.bootstrap.getGame().getServiceManager().provide(PermissionService.class).orElse(null);
        if (oldService != null && !(oldService instanceof ProxiedServiceObject)) {

            // before registering our permission service, copy any existing permission descriptions
            Collection<PermissionDescription> permissionDescriptions = oldService.getDescriptions();
            for (PermissionDescription description : permissionDescriptions) {
                if (description instanceof ProxiedServiceObject) {
                    continue;
                }
                ProxyFactory.registerDescription(this.service, description);
            }
        }

        if (this.bootstrap.getGame().getPluginManager().getPlugin("permissionsex").isPresent()) {
            getLogger().warn("Detected PermissionsEx - assuming it's loaded for migration.");
            getLogger().warn("Delaying LuckPerms PermissionService registration.");
            this.lateLoad = true;
        } else {
            this.bootstrap.getGame().getServiceManager().setProvider(this.bootstrap, LPPermissionService.class, this.service);
            this.bootstrap.getGame().getServiceManager().setProvider(this.bootstrap, PermissionService.class, this.service.sponge());
        }
    }

    @Override
    protected AbstractEventBus<?> provideEventBus(LuckPermsApiProvider apiProvider) {
        return new SpongeEventBus(this, apiProvider);
    }

    @Override
    protected void registerApiOnPlatform(LuckPerms api) {
        this.bootstrap.getGame().getServiceManager().setProvider(this.bootstrap, LuckPerms.class, api);
    }

    @Override
    protected void registerHousekeepingTasks() {
        super.registerHousekeepingTasks();
        this.bootstrap.getScheduler().asyncRepeating(new ServiceCacheHousekeepingTask(this.service), 2, TimeUnit.MINUTES);
    }

    @Override
    protected void performFinalSetup() {
        // register permissions
        for (CommandPermission perm : CommandPermission.values()) {
            this.service.registerPermissionDescription(perm.getPermission(), null, this.bootstrap.getPluginContainer());
        }
    }

    public void lateEnable() {
        if (this.lateLoad) {
            getLogger().info("Providing late registration of PermissionService...");
            this.bootstrap.getGame().getServiceManager().setProvider(this.bootstrap, LPPermissionService.class, this.service);
            this.bootstrap.getGame().getServiceManager().setProvider(this.bootstrap, PermissionService.class, this.service.sponge());
        }
    }

    @Override
    public void performPlatformDataSync() {
        for (LPSubjectCollection collection : this.service.getLoadedCollections().values()) {
            if (collection instanceof PersistedCollection) {
                ((PersistedCollection) collection).loadAll();
            }
        }
        this.service.invalidateAllCaches();
    }

    @Override
    public Optional<QueryOptions> getQueryOptionsForUser(User user) {
        return this.bootstrap.getPlayer(user.getUniqueId()).map(player -> this.contextManager.getQueryOptions(player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        return Stream.concat(
                Stream.of(getConsoleSender()),
                this.bootstrap.getServer().map(server -> server.getOnlinePlayers().stream().map(s -> this.senderFactory.wrap(s))).orElseGet(Stream::empty)
        );
    }

    @Override
    public Sender getConsoleSender() {
        if (this.bootstrap.getGame().isServerAvailable()) {
            return this.senderFactory.wrap(this.bootstrap.getGame().getServer().getConsole());
        } else {
            return new DummyConsoleSender(this) {
                @Override
                public void sendMessage(Component message) {
                    LPSpongePlugin.this.getLogger().info(LegacyComponentSerializer.legacySection().serialize(TranslationManager.render(message)));
                }
            };
        }
    }

    @Override
    public List<Command<?>> getExtraCommands() {
        return Collections.singletonList(new SpongeParentCommand(this));
    }

    public SpongeSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    @Override
    public SpongeConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public SpongeCommandExecutor getCommandManager() {
        return this.commandManager;
    }

    @Override
    public SpongeUserManager getUserManager() {
        return this.userManager;
    }

    @Override
    public SpongeGroupManager getGroupManager() {
        return this.groupManager;
    }

    @Override
    public StandardTrackManager getTrackManager() {
        return this.trackManager;
    }

    @Override
    public SpongeContextManager getContextManager() {
        return this.contextManager;
    }

    public LuckPermsService getService() {
        return this.service;
    }

}
