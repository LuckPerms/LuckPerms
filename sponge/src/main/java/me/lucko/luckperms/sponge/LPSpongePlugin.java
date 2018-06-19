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

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.abstraction.Command;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.config.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.contexts.ContextManager;
import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.managers.track.StandardTrackManager;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin;
import me.lucko.luckperms.common.sender.DummySender;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.tasks.CacheHousekeepingTask;
import me.lucko.luckperms.common.tasks.ExpireTemporaryTask;
import me.lucko.luckperms.common.treeview.PermissionRegistry;
import me.lucko.luckperms.common.utils.MoreFiles;
import me.lucko.luckperms.sponge.calculators.SpongeCalculatorFactory;
import me.lucko.luckperms.sponge.commands.SpongeMainCommand;
import me.lucko.luckperms.sponge.contexts.SpongeContextManager;
import me.lucko.luckperms.sponge.contexts.WorldCalculator;
import me.lucko.luckperms.sponge.listeners.SpongeConnectionListener;
import me.lucko.luckperms.sponge.listeners.SpongePlatformListener;
import me.lucko.luckperms.sponge.managers.SpongeGroupManager;
import me.lucko.luckperms.sponge.managers.SpongeUserManager;
import me.lucko.luckperms.sponge.messaging.SpongeMessagingFactory;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.event.UpdateEventHandler;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.persisted.PersistedCollection;
import me.lucko.luckperms.sponge.tasks.ServiceCacheHousekeepingTask;

import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
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
    private ContextManager<Subject> contextManager;
    private LuckPermsService service;
    private UpdateEventHandler updateEventHandler;

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
        return EnumSet.of(Dependency.TEXT, Dependency.CAFFEINE, Dependency.OKIO, Dependency.OKHTTP,
                Dependency.CONFIGURATE_CORE, Dependency.CONFIGURATE_HOCON, Dependency.HOCON_CONFIG);
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new SpongeConfigAdapter(this, resolveConfig());
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
        this.contextManager.registerCalculator(new WorldCalculator(this));
    }

    @Override
    protected void setupPlatformHooks() {
        getLogger().info("Registering PermissionService...");
        this.updateEventHandler = UpdateEventHandler.obtain(this);
        this.service = new LuckPermsService(this);

        // before registering our permission service, copy any existing permission descriptions
        PermissionRegistry permissionRegistry = getPermissionRegistry();
        this.bootstrap.getGame().getServiceManager().provide(PermissionService.class)
                .ifPresent(ps -> ps.getDescriptions().stream().map(PermissionDescription::getId).forEach(permissionRegistry::insert));

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
    protected AbstractEventBus provideEventBus(LuckPermsApiProvider apiProvider) {
        return new SpongeEventBus(this, apiProvider);
    }

    @Override
    protected void registerApiOnPlatform(LuckPermsApi api) {
        this.bootstrap.getGame().getServiceManager().setProvider(this.bootstrap, LuckPermsApi.class, api);
    }

    @Override
    protected void registerHousekeepingTasks() {
        this.bootstrap.getScheduler().asyncRepeating(new ExpireTemporaryTask(this), 3, TimeUnit.SECONDS);
        this.bootstrap.getScheduler().asyncRepeating(new CacheHousekeepingTask(this), 2, TimeUnit.MINUTES);
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
    public void onPostUpdate() {
        for (LPSubjectCollection collection : this.service.getLoadedCollections().values()) {
            if (collection instanceof PersistedCollection) {
                ((PersistedCollection) collection).loadAll();
            }
        }
        this.service.invalidateAllCaches();
    }

    private Path resolveConfig() {
        Path path = this.bootstrap.getConfigPath().resolve("luckperms.conf");
        if (!Files.exists(path)) {
            try {
                MoreFiles.createDirectoriesIfNotExists(this.bootstrap.getConfigPath());
                try (InputStream is = getClass().getClassLoader().getResourceAsStream("luckperms.conf")) {
                    Files.copy(is, path);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return path;
    }

    @Override
    public Optional<Contexts> getContextForUser(User user) {
        return this.bootstrap.getPlayer(user.getUuid()).map(player -> this.contextManager.getApplicableContexts(player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        if (!this.bootstrap.getGame().isServerAvailable()) {
            return Stream.empty();
        }

        return Stream.concat(
                Stream.of(getConsoleSender()),
                this.bootstrap.getGame().getServer().getOnlinePlayers().stream().map(s -> this.senderFactory.wrap(s))
        );
    }

    @Override
    public Sender getConsoleSender() {
        if (!this.bootstrap.getGame().isServerAvailable()) {
            return new DummySender(this, CommandManager.CONSOLE_UUID, CommandManager.CONSOLE_NAME) {
                @Override
                protected void consumeMessage(String s) {
                    LPSpongePlugin.this.bootstrap.getPluginLogger().info(s);
                }
            };
        }
        return this.senderFactory.wrap(this.bootstrap.getGame().getServer().getConsole());
    }

    @Override
    public List<Command<?, ?>> getExtraCommands() {
        return Collections.singletonList(new SpongeMainCommand(this));
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
    public ContextManager<Subject> getContextManager() {
        return this.contextManager;
    }

    public LuckPermsService getService() {
        return this.service;
    }

    public UpdateEventHandler getUpdateEventHandler() {
        return this.updateEventHandler;
    }

}
