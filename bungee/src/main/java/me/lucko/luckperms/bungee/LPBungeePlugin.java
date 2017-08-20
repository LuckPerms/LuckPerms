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
import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.PlatformType;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.bungee.calculators.BungeeCalculatorFactory;
import me.lucko.luckperms.bungee.contexts.BackendServerCalculator;
import me.lucko.luckperms.bungee.messaging.BungeeMessagingService;
import me.lucko.luckperms.bungee.messaging.RedisBungeeMessagingService;
import me.lucko.luckperms.bungee.util.RedisBungeeUtil;
import me.lucko.luckperms.common.actionlog.LogDispatcher;
import me.lucko.luckperms.common.api.ApiHandler;
import me.lucko.luckperms.common.api.ApiProvider;
import me.lucko.luckperms.common.buffers.BufferedRequest;
import me.lucko.luckperms.common.caching.handlers.CachedStateManager;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.contexts.ContextManager;
import me.lucko.luckperms.common.contexts.LuckPermsCalculator;
import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.dependencies.DependencyManager;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.NoopLocaleManager;
import me.lucko.luckperms.common.locale.SimpleLocaleManager;
import me.lucko.luckperms.common.logging.SenderLogger;
import me.lucko.luckperms.common.managers.GenericGroupManager;
import me.lucko.luckperms.common.managers.GenericTrackManager;
import me.lucko.luckperms.common.managers.GenericUserManager;
import me.lucko.luckperms.common.managers.GroupManager;
import me.lucko.luckperms.common.managers.TrackManager;
import me.lucko.luckperms.common.managers.UserManager;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.messaging.NoopMessagingService;
import me.lucko.luckperms.common.messaging.RedisMessagingService;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.LuckPermsScheduler;
import me.lucko.luckperms.common.storage.Storage;
import me.lucko.luckperms.common.storage.StorageFactory;
import me.lucko.luckperms.common.storage.StorageType;
import me.lucko.luckperms.common.storage.backing.file.FileWatcher;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Getter
public class LPBungeePlugin extends Plugin implements LuckPermsPlugin {

    private long startTime;
    private LuckPermsScheduler scheduler;
    private CommandManager commandManager;
    private LuckPermsConfiguration configuration;
    private UserManager userManager;
    private GroupManager groupManager;
    private TrackManager trackManager;
    private Storage storage;
    private FileWatcher fileWatcher = null;
    private InternalMessagingService messagingService = null;
    private UuidCache uuidCache;
    private ApiProvider apiProvider;
    private Logger log;
    private LocaleManager localeManager;
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
        scheduler = new LPBungeeScheduler(this);
        localeManager = new NoopLocaleManager();
        senderFactory = new BungeeSenderFactory(this);
        log = new SenderLogger(this, getConsoleSender());

        DependencyManager.loadDependencies(this, Collections.singletonList(Dependency.CAFFEINE));
    }

    @Override
    public void onEnable() {
        startTime = System.currentTimeMillis();
        LuckPermsPlugin.sendStartupBanner(getConsoleSender(), this);
        verboseHandler = new VerboseHandler(scheduler.async(), getVersion());
        permissionVault = new PermissionVault(scheduler.async());
        logDispatcher = new LogDispatcher(this);

        getLog().info("Loading configuration...");
        configuration = new BungeeConfig(this);
        configuration.init();
        configuration.loadAll();

        Set<StorageType> storageTypes = StorageFactory.getRequiredTypes(this, StorageType.H2);
        DependencyManager.loadDependencies(this, storageTypes);

        // register events
        getProxy().getPluginManager().registerListener(this, new BungeeListener(this));

        if (getConfiguration().get(ConfigKeys.WATCH_FILES)) {
            fileWatcher = new FileWatcher(this);
            getScheduler().asyncRepeating(fileWatcher, 30L);
        }

        // initialise datastore
        storage = StorageFactory.getInstance(this, StorageType.H2);

        // initialise messaging
        String messagingType = getConfiguration().get(ConfigKeys.MESSAGING_SERVICE).toLowerCase();
        if (messagingType.equals("none") && getConfiguration().get(ConfigKeys.REDIS_ENABLED)) {
            messagingType = "redis";
        }

        if (!messagingType.equals("none")) {
            getLog().info("Loading messaging service... [" + messagingType.toUpperCase() + "]");
        }

        if (messagingType.equals("redis")) {
            if (getConfiguration().get(ConfigKeys.REDIS_ENABLED)) {
                RedisMessagingService redis = new RedisMessagingService(this);
                try {
                    redis.init(getConfiguration().get(ConfigKeys.REDIS_ADDRESS), getConfiguration().get(ConfigKeys.REDIS_PASSWORD));
                    messagingService = redis;
                } catch (Exception e) {
                    getLog().warn("Couldn't load redis...");
                    e.printStackTrace();
                }
            } else {
                getLog().warn("Messaging Service was set to redis, but redis is not enabled!");
            }
        } else if (messagingType.equals("bungee")) {
            BungeeMessagingService bungeeMessaging = new BungeeMessagingService(this);
            bungeeMessaging.init();
            messagingService = bungeeMessaging;
        } else if (messagingType.equals("redisbungee")) {
            if (getProxy().getPluginManager().getPlugin("RedisBungee") == null) {
                getLog().warn("RedisBungee plugin not present.");
            } else {
                RedisBungeeMessagingService redisBungeeMessaging = new RedisBungeeMessagingService(this);
                redisBungeeMessaging.init();
                messagingService = redisBungeeMessaging;
            }
        } else if (!messagingType.equals("none")) {
            getLog().warn("Messaging service '" + messagingType + "' not recognised.");
        }

        if (messagingService == null) {
            messagingService = new NoopMessagingService();
        }

        // setup the update task buffer
        updateTaskBuffer = new BufferedRequest<Void>(1000L, this::doAsync) {
            @Override
            protected Void perform() {
                new UpdateTask(LPBungeePlugin.this).run();
                return null;
            }
        };

        // load locale
        localeManager = new SimpleLocaleManager();
        File locale = new File(getDataFolder(), "lang.yml");
        if (locale.exists()) {
            getLog().info("Found lang.yml - loading messages...");
            try {
                localeManager.loadFromFile(locale);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // register commands
        commandManager = new CommandManager(this);
        getProxy().getPluginManager().registerCommand(this, new BungeeCommand(this, commandManager));

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

        contextManager = new ContextManager<ProxiedPlayer>() {
            @Override
            public Contexts formContexts(ProxiedPlayer player, ImmutableContextSet contextSet) {
                return new Contexts(
                        contextSet,
                        getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_PERMS),
                        getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_WORLD_PERMS),
                        true,
                        getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_GROUPS),
                        getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_WORLD_GROUPS),
                        false
                );
            }
        };

        BackendServerCalculator serverCalculator = new BackendServerCalculator(this);
        contextManager.registerCalculator(serverCalculator);

        LuckPermsCalculator<ProxiedPlayer> staticCalculator = new LuckPermsCalculator<>(getConfiguration());
        contextManager.registerCalculator(staticCalculator, true);

        // register with the LP API
        apiProvider = new ApiProvider(this);
        ApiHandler.registerProvider(apiProvider);

        // schedule update tasks
        int mins = getConfiguration().get(ConfigKeys.SYNC_TIME);
        if (mins > 0) {
            long ticks = mins * 60 * 20;
            scheduler.asyncRepeating(() -> updateTaskBuffer.request(), ticks);
        }
        scheduler.asyncLater(() -> updateTaskBuffer.request(), 40L);

        // run an update instantly.
        getLog().info("Performing initial data load...");
        updateTaskBuffer.requestDirectly();

        // register tasks
        scheduler.asyncRepeating(new ExpireTemporaryTask(this), 60L);
        scheduler.asyncRepeating(new CacheHousekeepingTask(this), 2400L);

        getLog().info("Successfully enabled. (took " + (System.currentTimeMillis() - startTime) + "ms)");
    }

    @Override
    public void onDisable() {
        getLog().info("Closing storage...");
        storage.shutdown();

        if (fileWatcher != null) {
            fileWatcher.close();
        }

        if (messagingService != null) {
            getLog().info("Closing messaging service...");
            messagingService.close();
        }

        ApiHandler.unregisterProvider();

        getLog().info("Shutting down internal scheduler...");
        scheduler.shutdown();
        getProxy().getScheduler().cancel(this);
        getProxy().getPluginManager().unregisterListeners(this);
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
    public String getServerName() {
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
    public List<String> getPlayerList() {
        return getProxy().getPlayers().stream().map(ProxiedPlayer::getName).collect(Collectors.toList());
    }

    @Override
    public Set<UUID> getOnlinePlayers() {
        return getProxy().getPlayers().stream().map(ProxiedPlayer::getUniqueId).collect(Collectors.toSet());
    }

    @Override
    public boolean isPlayerOnline(UUID external) {
        ProxiedPlayer player = getProxy().getPlayer(external);
        return player != null && player.isConnected();
    }

    @Override
    public List<Sender> getOnlineSenders() {
        return getProxy().getPlayers().stream()
                .map(p -> getSenderFactory().wrap(p))
                .collect(Collectors.toList());
    }

    @Override
    public Sender getConsoleSender() {
        return getSenderFactory().wrap(getProxy().getConsole());
    }

    @Override
    public Set<Contexts> getPreProcessContexts(boolean op) {
        Set<ImmutableContextSet> c = new HashSet<>();
        c.add(ContextSet.empty());
        c.add(ContextSet.singleton("server", getConfiguration().get(ConfigKeys.SERVER)));
        return c.stream()
                .map(set -> contextManager.formContexts(null, set))
                .collect(Collectors.toSet());
    }
}
