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

import com.google.inject.Inject;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.platform.PlatformType;
import me.lucko.luckperms.common.actionlog.LogDispatcher;
import me.lucko.luckperms.common.api.ApiRegistrationUtil;
import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.buffers.BufferedRequest;
import me.lucko.luckperms.common.buffers.UpdateTaskBuffer;
import me.lucko.luckperms.common.caching.handlers.CachedStateManager;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.abstraction.Command;
import me.lucko.luckperms.common.commands.sender.DummySender;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.config.AbstractConfiguration;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.contexts.ContextManager;
import me.lucko.luckperms.common.contexts.LuckPermsCalculator;
import me.lucko.luckperms.common.dependencies.DependencyManager;
import me.lucko.luckperms.common.dependencies.DependencyRegistry;
import me.lucko.luckperms.common.dependencies.classloader.PluginClassLoader;
import me.lucko.luckperms.common.dependencies.classloader.ReflectionClassLoader;
import me.lucko.luckperms.common.event.EventFactory;
import me.lucko.luckperms.common.inheritance.InheritanceHandler;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.NoopLocaleManager;
import me.lucko.luckperms.common.locale.SimpleLocaleManager;
import me.lucko.luckperms.common.logging.SenderLogger;
import me.lucko.luckperms.common.managers.track.StandardTrackManager;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.SchedulerAdapter;
import me.lucko.luckperms.common.storage.Storage;
import me.lucko.luckperms.common.storage.StorageFactory;
import me.lucko.luckperms.common.storage.StorageType;
import me.lucko.luckperms.common.storage.dao.file.FileWatcher;
import me.lucko.luckperms.common.tasks.CacheHousekeepingTask;
import me.lucko.luckperms.common.tasks.ExpireTemporaryTask;
import me.lucko.luckperms.common.tasks.UpdateTask;
import me.lucko.luckperms.common.treeview.PermissionVault;
import me.lucko.luckperms.common.verbose.VerboseHandler;
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
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.model.LuckPermsSpongePlugin;
import me.lucko.luckperms.sponge.service.persisted.PersistedCollection;
import me.lucko.luckperms.sponge.tasks.ServiceCacheHousekeepingTask;
import me.lucko.luckperms.sponge.utils.VersionData;

import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.scheduler.AsynchronousExecutor;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.scheduler.SynchronousExecutor;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

/**
 * LuckPerms implementation for the Sponge API.
 */
@Plugin(
        id = "luckperms",
        name = "LuckPerms",
        version = VersionData.VERSION,
        authors = "Luck",
        description = "A permissions plugin",
        url = "https://github.com/lucko/LuckPerms"
)
public class LPSpongePlugin implements LuckPermsSpongePlugin {

    @Inject
    private Logger logger;

    @Inject
    private Game game;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDirectory;

    private Scheduler spongeScheduler = Sponge.getScheduler();

    @Inject
    @SynchronousExecutor
    private SpongeExecutorService syncExecutorService;

    @Inject
    @AsynchronousExecutor
    private SpongeExecutorService asyncExecutorService;

    @Inject
    private PluginContainer pluginContainer;

    private boolean lateLoad = false;
    private long startTime;

    private SchedulerAdapter scheduler;
    private SpongeCommandExecutor commandManager;
    private LuckPermsConfiguration configuration;
    private SpongeUserManager userManager;
    private SpongeGroupManager groupManager;
    private StandardTrackManager trackManager;
    private Storage storage;
    private FileWatcher fileWatcher = null;
    private InternalMessagingService messagingService = null;
    private LuckPermsApiProvider apiProvider;
    private EventFactory eventFactory;
    private me.lucko.luckperms.common.logging.Logger log;
    private LuckPermsService service;
    private LocaleManager localeManager;
    private PluginClassLoader pluginClassLoader;
    private DependencyManager dependencyManager;
    private InheritanceHandler inheritanceHandler;
    private CachedStateManager cachedStateManager;
    private ContextManager<Subject> contextManager;
    private CalculatorFactory calculatorFactory;
    private BufferedRequest<Void> updateTaskBuffer;
    private VerboseHandler verboseHandler;
    private SpongeSenderFactory senderFactory;
    private PermissionVault permissionVault;
    private LogDispatcher logDispatcher;
    private Set<UUID> uniqueConnections = ConcurrentHashMap.newKeySet();

    @Listener(order = Order.FIRST)
    public void onEnable(GamePreInitializationEvent event) {
        this.startTime = System.currentTimeMillis();
        this.scheduler = new SpongeSchedulerAdapter(this);
        this.localeManager = new NoopLocaleManager();
        this.senderFactory = new SpongeSenderFactory(this);
        this.log = new SenderLogger(this, getConsoleSender());
        this.pluginClassLoader = new ReflectionClassLoader(this);
        this.dependencyManager = new DependencyManager(this);
        this.dependencyManager.loadDependencies(DependencyRegistry.GLOBAL_DEPENDENCIES);

        sendStartupBanner(getConsoleSender());
        this.verboseHandler = new VerboseHandler(this.scheduler.async());
        this.permissionVault = new PermissionVault(this.scheduler.async());
        this.logDispatcher = new LogDispatcher(this);

        getLog().info("Loading configuration...");
        this.configuration = new AbstractConfiguration(this, new SpongeConfigAdapter(this, resolveConfig("luckperms.conf")));
        this.configuration.loadAll();

        StorageFactory storageFactory = new StorageFactory(this);
        Set<StorageType> storageTypes = storageFactory.getRequiredTypes(StorageType.H2);
        this.dependencyManager.loadStorageDependencies(storageTypes);

        // register events
        this.game.getEventManager().registerListeners(this, new SpongeConnectionListener(this));
        this.game.getEventManager().registerListeners(this, new SpongePlatformListener(this));

        if (getConfiguration().get(ConfigKeys.WATCH_FILES)) {
            this.fileWatcher = new FileWatcher(this);
            getScheduler().asyncRepeating(this.fileWatcher, 30L);
        }

        // initialise datastore
        this.storage = storageFactory.getInstance(StorageType.H2);

        // initialise messaging
        this.messagingService = new SpongeMessagingFactory(this).getInstance();

        // setup the update task buffer
        this.updateTaskBuffer = new UpdateTaskBuffer(this);

        // load locale
        this.localeManager = new SimpleLocaleManager();
        this.localeManager.tryLoad(this, new File(getDataDirectory(), "lang.yml"));

        // register commands
        CommandManager cmdService = this.game.getCommandManager();
        this.commandManager = new SpongeCommandExecutor(this);
        cmdService.register(this, this.commandManager, "luckperms", "lp", "perm", "perms", "permission", "permissions");

        // load internal managers
        getLog().info("Loading internal permission managers...");
        this.inheritanceHandler = new InheritanceHandler(this);
        this.userManager = new SpongeUserManager(this);
        this.groupManager = new SpongeGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
        this.calculatorFactory = new SpongeCalculatorFactory(this);
        this.cachedStateManager = new CachedStateManager();

        // setup context manager
        this.contextManager = new SpongeContextManager(this);
        this.contextManager.registerCalculator(new WorldCalculator(this));
        this.contextManager.registerStaticCalculator(new LuckPermsCalculator(getConfiguration()));

        // register the PermissionService with Sponge
        getLog().info("Registering PermissionService...");
        this.service = new LuckPermsService(this);

        if (this.game.getPluginManager().getPlugin("permissionsex").isPresent()) {
            getLog().warn("Detected PermissionsEx - assuming it's loaded for migration.");
            getLog().warn("Delaying LuckPerms PermissionService registration.");
            this.lateLoad = true;
        } else {
            this.game.getServiceManager().setProvider(this, LPPermissionService.class, this.service);
            this.game.getServiceManager().setProvider(this, PermissionService.class, this.service.sponge());
            this.game.getServiceManager().setProvider(this, LuckPermsService.class, this.service);
        }

        // register with the LP API
        this.apiProvider = new LuckPermsApiProvider(this);

        // setup event factory
        this.eventFactory = new EventFactory(this, this.apiProvider);

        ApiRegistrationUtil.registerProvider(this.apiProvider);
        this.game.getServiceManager().setProvider(this, LuckPermsApi.class, this.apiProvider);

        // schedule update tasks
        int mins = getConfiguration().get(ConfigKeys.SYNC_TIME);
        if (mins > 0) {
            long ticks = mins * 60 * 20;
            this.scheduler.asyncRepeating(() -> this.updateTaskBuffer.request(), ticks);
        }
        this.scheduler.asyncLater(() -> this.updateTaskBuffer.request(), 40L);

        // run an update instantly.
        getLog().info("Performing initial data load...");
        try {
            new UpdateTask(this, true).run();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // register tasks
        this.scheduler.asyncRepeating(new ExpireTemporaryTask(this), 60L);
        this.scheduler.asyncRepeating(new CacheHousekeepingTask(this), 2400L);
        this.scheduler.asyncRepeating(new ServiceCacheHousekeepingTask(this.service), 2400L);

        // register permissions
        for (CommandPermission perm : CommandPermission.values()) {
            this.service.registerPermissionDescription(perm.getPermission(), null, this.pluginContainer);
        }

        getLog().info("Successfully enabled. (took " + (System.currentTimeMillis() - this.startTime) + "ms)");
    }

    @Listener(order = Order.LATE)
    public void onLateEnable(GamePreInitializationEvent event) {
        if (this.lateLoad) {
            getLog().info("Providing late registration of PermissionService...");
            this.game.getServiceManager().setProvider(this, LPPermissionService.class, this.service);
            this.game.getServiceManager().setProvider(this, PermissionService.class, this.service.sponge());
            this.game.getServiceManager().setProvider(this, LuckPermsService.class, this.service);
        }
    }

    @Listener
    public void onDisable(GameStoppingServerEvent event) {
        this.permissionVault.shutdown();
        this.verboseHandler.shutdown();

        getLog().info("Closing storage...");
        this.storage.shutdown();

        if (this.fileWatcher != null) {
            this.fileWatcher.close();
        }

        if (this.messagingService != null) {
            getLog().info("Closing messaging service...");
            this.messagingService.close();
        }

        ApiRegistrationUtil.unregisterProvider();

        getLog().info("Shutting down internal scheduler...");
        this.scheduler.shutdown();

        getLog().info("Goodbye!");
    }

    @Override
    public void onPostUpdate() {
        for (LPSubjectCollection collection : this.service.getLoadedCollections().values()) {
            if (collection instanceof PersistedCollection) {
                ((PersistedCollection) collection).loadAll();
            }
        }
        this.service.invalidateAllCaches(LPSubject.CacheLevel.PARENT);
    }

    private Path resolveConfig(String file) {
        Path path = this.configDirectory.resolve(file);

        if (!Files.exists(path)) {
            try {
                Files.createDirectories(this.configDirectory);
                try (InputStream is = getClass().getClassLoader().getResourceAsStream(file)) {
                    Files.copy(is, path);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return path;
    }

    @Override
    public Optional<InternalMessagingService> getMessagingService() {
        return Optional.ofNullable(this.messagingService);
    }

    @Override
    public void setMessagingService(InternalMessagingService messagingService) {
        if (this.messagingService == null) {
            this.messagingService = messagingService;
        }
    }

    @Override
    public Optional<FileWatcher> getFileWatcher() {
        return Optional.ofNullable(this.fileWatcher);
    }

    @Override
    public File getDataDirectory() {
        File serverRoot = this.configDirectory.toFile().getParentFile().getParentFile();
        File dataDirectory = new File(serverRoot, "luckperms");
        dataDirectory.mkdirs();
        return dataDirectory;
    }

    @Override
    public File getConfigDirectory() {
        return this.configDirectory.toFile();
    }

    @Override
    public InputStream getResourceStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    @Override
    public Player getPlayer(User user) {
        if (!this.game.isServerAvailable()) {
            return null;
        }

        return this.game.getServer().getPlayer(user.getUuid()).orElse(null);
    }

    @Override
    public Optional<UUID> lookupUuid(String username) {
        if (!this.game.isServerAvailable()) {
            return Optional.empty();
        }

        CompletableFuture<GameProfile> fut = this.game.getServer().getGameProfileManager().get(username);
        try {
            return Optional.of(fut.get().getUniqueId());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public Optional<Contexts> getContextForUser(User user) {
        Player player = getPlayer(user);
        if (player == null) {
            return Optional.empty();
        }
        return Optional.of(this.contextManager.getApplicableContexts(player));
    }

    @Override
    public String getVersion() {
        return VersionData.VERSION;
    }

    @Override
    public PlatformType getServerType() {
        return PlatformType.SPONGE;
    }

    @Override
    public String getServerBrand() {
        return getGame().getPlatform().getContainer(Platform.Component.IMPLEMENTATION).getName();
    }

    @Override
    public String getServerVersion() {
        return getGame().getPlatform().getContainer(Platform.Component.API).getName() + ": " +
                getGame().getPlatform().getContainer(Platform.Component.API).getVersion().orElse("null") + " - " +
                getGame().getPlatform().getContainer(Platform.Component.IMPLEMENTATION).getName() + ": " +
                getGame().getPlatform().getContainer(Platform.Component.IMPLEMENTATION).getVersion().orElse("null");
    }

    @Override
    public int getPlayerCount() {
        return this.game.isServerAvailable() ? this.game.getServer().getOnlinePlayers().size() : 0;
    }

    @Override
    public Stream<String> getPlayerList() {
        return this.game.isServerAvailable() ? this.game.getServer().getOnlinePlayers().stream().map(Player::getName) : Stream.empty();
    }

    @Override
    public Stream<UUID> getOnlinePlayers() {
        return this.game.isServerAvailable() ? this.game.getServer().getOnlinePlayers().stream().map(Player::getUniqueId) : Stream.empty();
    }

    @Override
    public boolean isPlayerOnline(UUID external) {
        return this.game.isServerAvailable() ? this.game.getServer().getPlayer(external).map(Player::isOnline).orElse(false) : false;
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        if (!this.game.isServerAvailable()) {
            return Stream.empty();
        }

        return Stream.concat(
                Stream.of(getConsoleSender()),
                this.game.getServer().getOnlinePlayers().stream().map(s -> getSenderFactory().wrap(s))
        );
    }

    @Override
    public Sender getConsoleSender() {
        if (!this.game.isServerAvailable()) {
            return new DummySender(this, me.lucko.luckperms.common.commands.CommandManager.CONSOLE_UUID, me.lucko.luckperms.common.commands.CommandManager.CONSOLE_NAME) {
                @Override
                protected void consumeMessage(String s) {
                    LPSpongePlugin.this.logger.info(s);
                }
            };
        }
        return getSenderFactory().wrap(this.game.getServer().getConsole());
    }

    @Override
    public List<Command> getExtraCommands() {
        return Collections.singletonList(new SpongeMainCommand(this));
    }

    public Game getGame() {
        return this.game;
    }

    public Scheduler getSpongeScheduler() {
        return this.spongeScheduler;
    }

    public SpongeExecutorService getSyncExecutorService() {
        return this.syncExecutorService;
    }

    public SpongeExecutorService getAsyncExecutorService() {
        return this.asyncExecutorService;
    }

    @Override
    public long getStartTime() {
        return this.startTime;
    }

    @Override
    public SchedulerAdapter getScheduler() {
        return this.scheduler;
    }

    @Override
    public SpongeCommandExecutor getCommandManager() {
        return this.commandManager;
    }

    @Override
    public LuckPermsConfiguration getConfiguration() {
        return this.configuration;
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
    public Storage getStorage() {
        return this.storage;
    }

    @Override
    public LuckPermsApiProvider getApiProvider() {
        return this.apiProvider;
    }

    @Override
    public EventFactory getEventFactory() {
        return this.eventFactory;
    }

    @Override
    public me.lucko.luckperms.common.logging.Logger getLog() {
        return this.log;
    }

    public LuckPermsService getService() {
        return this.service;
    }

    @Override
    public LocaleManager getLocaleManager() {
        return this.localeManager;
    }

    @Override
    public PluginClassLoader getPluginClassLoader() {
        return this.pluginClassLoader;
    }

    @Override
    public DependencyManager getDependencyManager() {
        return this.dependencyManager;
    }

    @Override
    public CachedStateManager getCachedStateManager() {
        return this.cachedStateManager;
    }

    @Override
    public ContextManager<Subject> getContextManager() {
        return this.contextManager;
    }

    @Override
    public InheritanceHandler getInheritanceHandler() {
        return this.inheritanceHandler;
    }

    @Override
    public CalculatorFactory getCalculatorFactory() {
        return this.calculatorFactory;
    }

    @Override
    public BufferedRequest<Void> getUpdateTaskBuffer() {
        return this.updateTaskBuffer;
    }

    @Override
    public VerboseHandler getVerboseHandler() {
        return this.verboseHandler;
    }

    public SpongeSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    @Override
    public PermissionVault getPermissionVault() {
        return this.permissionVault;
    }

    @Override
    public LogDispatcher getLogDispatcher() {
        return this.logDispatcher;
    }

    @Override
    public Set<UUID> getUniqueConnections() {
        return this.uniqueConnections;
    }
}
