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

package me.lucko.luckperms.nukkit;

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
import me.lucko.luckperms.common.logging.Logger;
import me.lucko.luckperms.common.logging.SenderLogger;
import me.lucko.luckperms.common.managers.group.StandardGroupManager;
import me.lucko.luckperms.common.managers.track.StandardTrackManager;
import me.lucko.luckperms.common.managers.user.StandardUserManager;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.Storage;
import me.lucko.luckperms.common.storage.StorageFactory;
import me.lucko.luckperms.common.storage.StorageType;
import me.lucko.luckperms.common.storage.dao.file.FileWatcher;
import me.lucko.luckperms.common.tasks.CacheHousekeepingTask;
import me.lucko.luckperms.common.tasks.ExpireTemporaryTask;
import me.lucko.luckperms.common.tasks.UpdateTask;
import me.lucko.luckperms.common.treeview.PermissionVault;
import me.lucko.luckperms.common.verbose.VerboseHandler;
import me.lucko.luckperms.nukkit.calculators.NukkitCalculatorFactory;
import me.lucko.luckperms.nukkit.contexts.NukkitContextManager;
import me.lucko.luckperms.nukkit.contexts.WorldCalculator;
import me.lucko.luckperms.nukkit.listeners.NukkitConnectionListener;
import me.lucko.luckperms.nukkit.listeners.NukkitPlatformListener;
import me.lucko.luckperms.nukkit.model.PermissionDefault;
import me.lucko.luckperms.nukkit.model.permissible.LPPermissible;
import me.lucko.luckperms.nukkit.model.permissible.PermissibleInjector;
import me.lucko.luckperms.nukkit.model.permissible.PermissibleMonitoringInjector;
import me.lucko.luckperms.nukkit.model.server.InjectorDefaultsMap;
import me.lucko.luckperms.nukkit.model.server.InjectorPermissionMap;
import me.lucko.luckperms.nukkit.model.server.InjectorSubscriptionMap;
import me.lucko.luckperms.nukkit.model.server.LPDefaultsMap;
import me.lucko.luckperms.nukkit.model.server.LPPermissionMap;
import me.lucko.luckperms.nukkit.model.server.LPSubscriptionMap;

import cn.nukkit.Player;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.event.HandlerList;
import cn.nukkit.permission.Permission;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginManager;
import cn.nukkit.plugin.service.ServicePriority;
import cn.nukkit.utils.Config;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * LuckPerms implementation for the Nukkit API.
 */
public class LPNukkitPlugin extends PluginBase implements LuckPermsPlugin {

    private long startTime;
    private NukkitSchedulerAdapter scheduler;
    private NukkitCommandExecutor commandManager;
    private LuckPermsConfiguration configuration;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private Storage storage;
    private FileWatcher fileWatcher = null;
    private InternalMessagingService messagingService = null;
    private LuckPermsApiProvider apiProvider;
    private EventFactory eventFactory;
    private Logger log;
    private LPSubscriptionMap subscriptionMap;
    private LPPermissionMap permissionMap;
    private LPDefaultsMap defaultPermissionMap;
    private LocaleManager localeManager;
    private PluginClassLoader pluginClassLoader;
    private DependencyManager dependencyManager;
    private InheritanceHandler inheritanceHandler;
    private CachedStateManager cachedStateManager;
    private ContextManager<Player> contextManager;
    private CalculatorFactory calculatorFactory;
    private BufferedRequest<Void> updateTaskBuffer;
    private VerboseHandler verboseHandler;
    private NukkitSenderFactory senderFactory;
    private PermissionVault permissionVault;
    private LogDispatcher logDispatcher;
    private Set<UUID> uniqueConnections = ConcurrentHashMap.newKeySet();

    @Override
    public void onLoad() {
        // setup minimal functionality in order to load initial dependencies
        this.scheduler = new NukkitSchedulerAdapter(this);
        this.localeManager = new NoopLocaleManager();
        this.senderFactory = new NukkitSenderFactory(this);
        this.log = new SenderLogger(this, getConsoleSender());

        this.pluginClassLoader = new ReflectionClassLoader(this);
        this.dependencyManager = new DependencyManager(this);
        this.dependencyManager.loadDependencies(DependencyRegistry.GLOBAL_DEPENDENCIES);
    }

    @Override
    public void onEnable() {
        this.startTime = System.currentTimeMillis();
        sendStartupBanner(getConsoleSender());
        this.verboseHandler = new VerboseHandler(this.scheduler.asyncNukkit());
        this.permissionVault = new PermissionVault(this.scheduler.asyncNukkit());
        this.logDispatcher = new LogDispatcher(this);

        getLog().info("Loading configuration...");
        this.configuration = new AbstractConfiguration(this, new NukkitConfigAdapter(this, resolveConfig("config.yml")));
        this.configuration.loadAll();

        StorageFactory storageFactory = new StorageFactory(this);
        Set<StorageType> storageTypes = storageFactory.getRequiredTypes(StorageType.H2);
        this.dependencyManager.loadStorageDependencies(storageTypes);

        // register events
        NukkitConnectionListener connectionListener = new NukkitConnectionListener(this);
        getServer().getPluginManager().registerEvents(connectionListener, this);
        getServer().getPluginManager().registerEvents(new NukkitPlatformListener(this), this);

        if (getConfiguration().get(ConfigKeys.WATCH_FILES)) {
            this.fileWatcher = new FileWatcher(this);
            getScheduler().asyncRepeating(this.fileWatcher, 30L);
        }

        // initialise datastore
        this.storage = storageFactory.getInstance(StorageType.H2);

        // initialise messaging
        this.messagingService = new MessagingFactory<>(this).getInstance();

        // setup the update task buffer
        this.updateTaskBuffer = new UpdateTaskBuffer(this);

        // load locale
        this.localeManager = new SimpleLocaleManager();
        this.localeManager.tryLoad(this, new File(getDataFolder(), "lang.yml"));

        // register commands
        this.commandManager = new NukkitCommandExecutor(this);
        PluginCommand main = (PluginCommand) getServer().getPluginCommand("luckperms");
        main.setExecutor(this.commandManager);

        // load internal managers
        getLog().info("Loading internal permission managers...");
        this.inheritanceHandler = new InheritanceHandler(this);
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
        this.calculatorFactory = new NukkitCalculatorFactory(this);
        this.cachedStateManager = new CachedStateManager();

        // setup context manager
        this.contextManager = new NukkitContextManager(this);
        this.contextManager.registerCalculator(new WorldCalculator(this));
        this.contextManager.registerStaticCalculator(new LuckPermsCalculator(getConfiguration()));

        // inject our own custom permission maps
        Runnable[] injectors = new Runnable[]{
                new InjectorSubscriptionMap(this),
                new InjectorPermissionMap(this),
                new InjectorDefaultsMap(this)
        };

        for (Runnable injector : injectors) {
            injector.run();

            // schedule another injection after all plugins have loaded
            // the entire pluginmanager instance is replaced by some plugins :(
            this.scheduler.asyncLater(injector, 1L);
        }

        // inject verbose handlers into internal nukkit objects
        new PermissibleMonitoringInjector(this).run();

        // register with the LP API
        this.apiProvider = new LuckPermsApiProvider(this);

        // setup event factory
        this.eventFactory = new EventFactory(this, this.apiProvider);

        ApiRegistrationUtil.registerProvider(this.apiProvider);
        getServer().getServiceManager().register(LuckPermsApi.class, this.apiProvider, this, ServicePriority.NORMAL);


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

        // register permissions
        try {
            PluginManager pm = getServer().getPluginManager();
            PermissionDefault permDefault = getConfiguration().get(ConfigKeys.COMMANDS_ALLOW_OP) ? PermissionDefault.OP : PermissionDefault.FALSE;

            for (CommandPermission p : CommandPermission.values()) {
                pm.addPermission(new Permission(p.getPermission(), null, permDefault.toString()));
            }
        } catch (Exception e) {
            // this throws an exception if the plugin is /reloaded, grr
        }

        if (!getConfiguration().get(ConfigKeys.OPS_ENABLED)) {
            Config ops = getServer().getOps();
            ops.getKeys(false).forEach(ops::remove);
        }

        // replace the temporary executor when the Nukkit one starts
        getServer().getScheduler().scheduleTask(this, () -> this.scheduler.setUseFallback(false), true);

        // Load any online users (in the case of a reload)
        for (Player player : getServer().getOnlinePlayers().values()) {
            this.scheduler.doAsync(() -> {
                try {
                    User user = connectionListener.loadUser(player.getUniqueId(), player.getName());
                    if (user != null) {
                        this.scheduler.doSync(() -> {
                            try {
                                LPPermissible lpPermissible = new LPPermissible(player, user, this);
                                PermissibleInjector.inject(player, lpPermissible);
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        getLog().info("Successfully enabled. (took " + (System.currentTimeMillis() - this.startTime) + "ms)");
    }

    @Override
    public void onDisable() {
        // Switch back to the fallback executor, the nukkit one won't allow new tasks
        this.scheduler.setUseFallback(true);

        this.permissionVault.shutdown();
        this.verboseHandler.shutdown();

        // uninject from players
        for (Player player : getServer().getOnlinePlayers().values()) {
            try {
                PermissibleInjector.unInject(player, false);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (getConfiguration().get(ConfigKeys.AUTO_OP)) {
                player.setOp(false);
            }

            final User user = getUserManager().getIfLoaded(player.getUniqueId());
            if (user != null) {
                user.getCachedData().invalidateCaches();
                getUserManager().unload(user);
            }
        }

        // uninject custom maps
        InjectorSubscriptionMap.uninject();
        InjectorPermissionMap.uninject();
        InjectorDefaultsMap.uninject();

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
        getServer().getServiceManager().cancel(this);

        getLog().info("Shutting down internal scheduler...");
        this.scheduler.shutdown();

        // Nukkit will do this again when #onDisable completes, but we do it early to prevent NPEs elsewhere.
        getServer().getScheduler().cancelTask(this);
        HandlerList.unregisterAll(this);
        getLog().info("Goodbye!");
    }

    public void refreshAutoOp(User user, Player player) {
        if (user == null) {
            return;
        }

        if (getConfiguration().get(ConfigKeys.AUTO_OP)) {
            Map<String, Boolean> backing = user.getCachedData().getPermissionData(this.contextManager.getApplicableContexts(player)).getImmutableBacking();
            boolean op = Optional.ofNullable(backing.get("luckperms.autoop")).orElse(false);
            player.setOp(op);
        }
    }

    private File resolveConfig(String file) {
        File configFile = new File(getDataFolder(), file);

        if (!configFile.exists()) {
            getDataFolder().mkdirs();
            saveResource("config.yml", false);
        }

        return configFile;
    }

    public LPSubscriptionMap getSubscriptionMap() {
        return this.subscriptionMap;
    }

    public void setSubscriptionMap(LPSubscriptionMap subscriptionMap) {
        this.subscriptionMap = subscriptionMap;
    }

    public LPPermissionMap getPermissionMap() {
        return this.permissionMap;
    }

    public void setPermissionMap(LPPermissionMap permissionMap) {
        this.permissionMap = permissionMap;
    }

    public LPDefaultsMap getDefaultPermissionMap() {
        return this.defaultPermissionMap;
    }

    public void setDefaultPermissionMap(LPDefaultsMap defaultPermissionMap) {
        this.defaultPermissionMap = defaultPermissionMap;
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
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public PlatformType getServerType() {
        return PlatformType.NUKKIT;
    }

    @Override
    public String getServerBrand() {
        return getServer().getName();
    }

    @Override
    public String getServerVersion() {
        return getServer().getVersion() + " - " + getServer().getNukkitVersion();
    }

    @Override
    public String getServerName() {
        return getServer().getServerUniqueId().toString();
    }

    @Override
    public File getDataDirectory() {
        return super.getDataFolder();
    }

    @Override
    public InputStream getResourceStream(String path) {
        return getResource(path);
    }

    @Override
    public Player getPlayer(User user) {
        return getServer().getOnlinePlayers().get(user.getUuid());
    }

    @Override
    public Optional<UUID> lookupUuid(String username) {
        return Optional.empty();
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
    public int getPlayerCount() {
        return getServer().getOnlinePlayers().size();
    }

    @Override
    public Stream<String> getPlayerList() {
        return getServer().getOnlinePlayers().values().stream().map(Player::getName);
    }

    @Override
    public Stream<UUID> getOnlinePlayers() {
        return getServer().getOnlinePlayers().values().stream().map(Player::getUniqueId);
    }

    @Override
    public boolean isPlayerOnline(UUID external) {
        Player player = getServer().getOnlinePlayers().get(external);
        return player != null && player.isOnline();
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        return Stream.concat(
                Stream.of(getConsoleSender()),
                getServer().getOnlinePlayers().values().stream().map(p -> getSenderFactory().wrap(p))
        );
    }

    @Override
    public Sender getConsoleSender() {
        return getSenderFactory().wrap(getServer().getConsoleSender());
    }

    @Override
    public long getStartTime() {
        return this.startTime;
    }

    @Override
    public NukkitSchedulerAdapter getScheduler() {
        return this.scheduler;
    }

    @Override
    public NukkitCommandExecutor getCommandManager() {
        return this.commandManager;
    }

    @Override
    public LuckPermsConfiguration getConfiguration() {
        return this.configuration;
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
    public Logger getLog() {
        return this.log;
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
    public ContextManager<Player> getContextManager() {
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

    public NukkitSenderFactory getSenderFactory() {
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
