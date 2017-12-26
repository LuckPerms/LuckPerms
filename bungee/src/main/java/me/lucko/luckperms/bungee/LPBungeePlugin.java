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

import lombok.Getter;

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
import me.lucko.luckperms.common.api.ApiProvider;
import me.lucko.luckperms.common.api.ApiSingletonUtils;
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
import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.dependencies.DependencyManager;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.NoopLocaleManager;
import me.lucko.luckperms.common.locale.SimpleLocaleManager;
import me.lucko.luckperms.common.logging.Logger;
import me.lucko.luckperms.common.logging.SenderLogger;
import me.lucko.luckperms.common.managers.GenericGroupManager;
import me.lucko.luckperms.common.managers.GenericTrackManager;
import me.lucko.luckperms.common.managers.GenericUserManager;
import me.lucko.luckperms.common.managers.GroupManager;
import me.lucko.luckperms.common.managers.TrackManager;
import me.lucko.luckperms.common.managers.UserManager;
import me.lucko.luckperms.common.messaging.ExtendedMessagingService;
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
import me.lucko.luckperms.common.utils.UuidCache;
import me.lucko.luckperms.common.verbose.VerboseHandler;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * LuckPerms implementation for the BungeeCord API.
 */
@Getter
public class LPBungeePlugin extends Plugin implements LuckPermsPlugin {

    private long startTime;
    private SchedulerAdapter scheduler;
    private CommandManager commandManager;
    private LuckPermsConfiguration configuration;
    private UserManager userManager;
    private GroupManager groupManager;
    private TrackManager trackManager;
    private Storage storage;
    private FileWatcher fileWatcher = null;
    private ExtendedMessagingService messagingService = null;
    private UuidCache uuidCache;
    private ApiProvider apiProvider;
    private Logger log;
    private LocaleManager localeManager;
    private DependencyManager dependencyManager;
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
        scheduler = new BungeeSchedulerAdapter(this);
        localeManager = new NoopLocaleManager();
        senderFactory = new BungeeSenderFactory(this);
        log = new SenderLogger(this, getConsoleSender());

        dependencyManager = new DependencyManager(this);
        dependencyManager.loadDependencies(Collections.singleton(Dependency.CAFFEINE));
    }

    @Override
    public void onEnable() {
        startTime = System.currentTimeMillis();
        LuckPermsPlugin.sendStartupBanner(getConsoleSender(), this);
        verboseHandler = new VerboseHandler(scheduler.async(), getVersion());
        permissionVault = new PermissionVault(scheduler.async());
        logDispatcher = new LogDispatcher(this);

        getLog().info("Loading configuration...");
        configuration = new AbstractConfiguration(this, new BungeeConfigAdapter(this));
        configuration.init();

        Set<StorageType> storageTypes = StorageFactory.getRequiredTypes(this, StorageType.H2);
        dependencyManager.loadStorageDependencies(storageTypes);

        // register events
        getProxy().getPluginManager().registerListener(this, new BungeeConnectionListener(this));
        getProxy().getPluginManager().registerListener(this, new BungeePermissionCheckListener(this));

        if (getConfiguration().get(ConfigKeys.WATCH_FILES)) {
            fileWatcher = new FileWatcher(this);
            getScheduler().asyncRepeating(fileWatcher, 30L);
        }

        // initialise datastore
        storage = StorageFactory.getInstance(this, StorageType.H2);

        // initialise messaging
        messagingService = new BungeeMessagingFactory(this).getInstance();

        // setup the update task buffer
        updateTaskBuffer = new UpdateTaskBuffer(this);

        // load locale
        localeManager = new SimpleLocaleManager();
        localeManager.tryLoad(this, new File(getDataFolder(), "lang.yml"));

        // register commands
        commandManager = new CommandManager(this);
        getProxy().getPluginManager().registerCommand(this, new BungeeCommandExecutor(this, commandManager));

        // disable the default Bungee /perms command so it gets handled by the Bukkit plugin
        getProxy().getDisabledCommands().add("perms");

        // load internal managers
        getLog().info("Loading internal permission managers...");
        uuidCache = new UuidCache(this);
        userManager = new GenericUserManager(this);
        groupManager = new GenericGroupManager(this);
        trackManager = new GenericTrackManager(this);
        calculatorFactory = new BungeeCalculatorFactory(this);
        cachedStateManager = new CachedStateManager();

        // setup context manager
        contextManager = new BungeeContextManager(this);
        contextManager.registerCalculator(new BackendServerCalculator(this));
        contextManager.registerStaticCalculator(new LuckPermsCalculator(getConfiguration()));

        if (getProxy().getPluginManager().getPlugin("RedisBungee") != null) {
            contextManager.registerStaticCalculator(new RedisBungeeCalculator());
        }

        // register with the LP API
        apiProvider = new ApiProvider(this);
        ApiSingletonUtils.registerProvider(apiProvider);

        // schedule update tasks
        int mins = getConfiguration().get(ConfigKeys.SYNC_TIME);
        if (mins > 0) {
            long ticks = mins * 60 * 20;
            scheduler.asyncRepeating(() -> updateTaskBuffer.request(), ticks);
        }
        scheduler.asyncLater(() -> updateTaskBuffer.request(), 40L);

        // run an update instantly.
        getLog().info("Performing initial data load...");
        try {
            new UpdateTask(this, true).run();
        } catch (Exception e) {
            e.printStackTrace();
        }


        // register tasks
        scheduler.asyncRepeating(new ExpireTemporaryTask(this), 60L);
        scheduler.asyncRepeating(new CacheHousekeepingTask(this), 2400L);

        getLog().info("Successfully enabled. (took " + (System.currentTimeMillis() - startTime) + "ms)");
    }

    @Override
    public void onDisable() {
        permissionVault.shutdown();
        verboseHandler.shutdown();

        getLog().info("Closing storage...");
        storage.shutdown();

        if (fileWatcher != null) {
            fileWatcher.close();
        }

        if (messagingService != null) {
            getLog().info("Closing messaging service...");
            messagingService.close();
        }

        ApiSingletonUtils.unregisterProvider();

        getLog().info("Shutting down internal scheduler...");
        scheduler.shutdown();

        getProxy().getScheduler().cancel(this);
        getProxy().getPluginManager().unregisterListeners(this);
        getLog().info("Goodbye!");
    }

    @Override
    public Optional<ExtendedMessagingService> getMessagingService() {
        return Optional.ofNullable(messagingService);
    }

    public Optional<FileWatcher> getFileWatcher() {
        return Optional.ofNullable(fileWatcher);
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
        return getProxy().getPlayer(uuidCache.getExternalUUID(user.getUuid()));
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
    public Contexts getContextForUser(User user) {
        ProxiedPlayer player = getPlayer(user);
        if (player == null) {
            return null;
        }
        return contextManager.getApplicableContexts(player);
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
}
