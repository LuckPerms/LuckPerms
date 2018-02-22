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

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.platform.PlatformType;
import me.lucko.luckperms.bungee.calculators.BungeeCalculatorFactory;
import me.lucko.luckperms.bungee.contexts.BackendServerCalculator;
import me.lucko.luckperms.bungee.contexts.BungeeContextManager;
import me.lucko.luckperms.bungee.contexts.RedisBungeeCalculator;
import me.lucko.luckperms.bungee.listeners.BungeeConnectionListener;
import me.lucko.luckperms.bungee.listeners.BungeePermissionCheckListener;
import me.lucko.luckperms.bungee.messaging.BungeeMessagingFactory;
import me.lucko.luckperms.bungee.util.RedisBungeeUtil;
import me.lucko.luckperms.common.actionlog.LogDispatcher;
import me.lucko.luckperms.common.api.ApiRegistrationUtil;
import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.buffers.BufferedRequest;
import me.lucko.luckperms.common.buffers.UpdateTaskBuffer;
import me.lucko.luckperms.common.caching.handlers.CachedStateManager;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.commands.CommandManager;
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
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
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

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * LuckPerms implementation for the BungeeCord API.
 */
public class LPBungeePlugin extends Plugin implements LuckPermsPlugin {

    private long startTime;
    private SchedulerAdapter scheduler;
    private CommandManager commandManager;
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
    private LocaleManager localeManager;
    private PluginClassLoader pluginClassLoader;
    private DependencyManager dependencyManager;
    private InheritanceHandler inheritanceHandler;
    private CachedStateManager cachedStateManager;
    private ContextManager<ProxiedPlayer> contextManager;
    private CalculatorFactory calculatorFactory;
    private BufferedRequest<Void> updateTaskBuffer;
    private VerboseHandler verboseHandler;
    private BungeeSenderFactory senderFactory;
    private PermissionVault permissionVault;
    private LogDispatcher logDispatcher;
    private Set<UUID> uniqueConnections = ConcurrentHashMap.newKeySet();

    @Override
    public void onLoad() {
        // setup minimal functionality in order to load initial dependencies
        this.scheduler = new BungeeSchedulerAdapter(this);
        this.localeManager = new NoopLocaleManager();
        this.senderFactory = new BungeeSenderFactory(this);
        this.log = new SenderLogger(this, getConsoleSender());

        this.pluginClassLoader = new ReflectionClassLoader(this);
        this.dependencyManager = new DependencyManager(this);
        this.dependencyManager.loadDependencies(DependencyRegistry.GLOBAL_DEPENDENCIES);
    }

    @Override
    public void onEnable() {
        this.startTime = System.currentTimeMillis();
        sendStartupBanner(getConsoleSender());
        this.verboseHandler = new VerboseHandler(this.scheduler.async());
        this.permissionVault = new PermissionVault(this.scheduler.async());
        this.logDispatcher = new LogDispatcher(this);

        getLog().info("Loading configuration...");
        this.configuration = new AbstractConfiguration(this, new BungeeConfigAdapter(this, resolveConfig("config.yml")));
        this.configuration.loadAll();

        StorageFactory storageFactory = new StorageFactory(this);
        Set<StorageType> storageTypes = storageFactory.getRequiredTypes(StorageType.H2);
        this.dependencyManager.loadStorageDependencies(storageTypes);

        // register events
        getProxy().getPluginManager().registerListener(this, new BungeeConnectionListener(this));
        getProxy().getPluginManager().registerListener(this, new BungeePermissionCheckListener(this));

        if (getConfiguration().get(ConfigKeys.WATCH_FILES)) {
            this.fileWatcher = new FileWatcher(this);
            getScheduler().asyncRepeating(this.fileWatcher, 30L);
        }

        // initialise datastore
        this.storage = storageFactory.getInstance(StorageType.H2);

        // initialise messaging
        this.messagingService = new BungeeMessagingFactory(this).getInstance();

        // setup the update task buffer
        this.updateTaskBuffer = new UpdateTaskBuffer(this);

        // load locale
        this.localeManager = new SimpleLocaleManager();
        this.localeManager.tryLoad(this, new File(getDataFolder(), "lang.yml"));

        // register commands
        this.commandManager = new CommandManager(this);
        getProxy().getPluginManager().registerCommand(this, new BungeeCommandExecutor(this, this.commandManager));

        // disable the default Bungee /perms command so it gets handled by the Bukkit plugin
        getProxy().getDisabledCommands().add("perms");

        // load internal managers
        getLog().info("Loading internal permission managers...");
        this.inheritanceHandler = new InheritanceHandler(this);
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
        this.calculatorFactory = new BungeeCalculatorFactory(this);
        this.cachedStateManager = new CachedStateManager();

        // setup context manager
        this.contextManager = new BungeeContextManager(this);
        this.contextManager.registerCalculator(new BackendServerCalculator(this));
        this.contextManager.registerStaticCalculator(new LuckPermsCalculator(getConfiguration()));

        if (getProxy().getPluginManager().getPlugin("RedisBungee") != null) {
            this.contextManager.registerStaticCalculator(new RedisBungeeCalculator());
        }

        // register with the LP API
        this.apiProvider = new LuckPermsApiProvider(this);

        // setup event factory
        this.eventFactory = new EventFactory(this, this.apiProvider);

        ApiRegistrationUtil.registerProvider(this.apiProvider);

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

        getLog().info("Successfully enabled. (took " + (System.currentTimeMillis() - this.startTime) + "ms)");
    }

    @Override
    public void onDisable() {
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

        getProxy().getScheduler().cancel(this);
        getProxy().getPluginManager().unregisterListeners(this);
        getLog().info("Goodbye!");
    }

    private File resolveConfig(String file) {
        File configFile = new File(getDataFolder(), file);

        if (!configFile.exists()) {
            getDataFolder().mkdirs();
            try (InputStream is = getResourceAsStream(file)) {
                Files.copy(is, configFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return configFile;
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
        return PlatformType.BUNGEE;
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
    public File getDataDirectory() {
        return super.getDataFolder();
    }

    @Override
    public InputStream getResourceStream(String path) {
        return getResourceAsStream(path);
    }

    @Override
    public ProxiedPlayer getPlayer(User user) {
        return getProxy().getPlayer(user.getUuid());
    }

    @Override
    public Optional<UUID> lookupUuid(String username) {
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
    public Optional<Contexts> getContextForUser(User user) {
        ProxiedPlayer player = getPlayer(user);
        if (player == null) {
            return Optional.empty();
        }
        return Optional.of(this.contextManager.getApplicableContexts(player));
    }

    @Override
    public int getPlayerCount() {
        return getProxy().getOnlineCount();
    }

    @Override
    public Stream<String> getPlayerList() {
        return getProxy().getPlayers().stream().map(ProxiedPlayer::getName);
    }

    @Override
    public Stream<UUID> getOnlinePlayers() {
        return getProxy().getPlayers().stream().map(ProxiedPlayer::getUniqueId);
    }

    @Override
    public boolean isPlayerOnline(UUID external) {
        ProxiedPlayer player = getProxy().getPlayer(external);
        return player != null && player.isConnected();
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        return Stream.concat(
                Stream.of(getConsoleSender()),
                getProxy().getPlayers().stream().map(p -> getSenderFactory().wrap(p))
        );
    }

    @Override
    public Sender getConsoleSender() {
        return getSenderFactory().wrap(getProxy().getConsole());
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
    public CommandManager getCommandManager() {
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
    public ContextManager<ProxiedPlayer> getContextManager() {
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

    public BungeeSenderFactory getSenderFactory() {
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
