/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.bungee.messaging.BungeeMessagingService;
import me.lucko.luckperms.common.api.ApiHandler;
import me.lucko.luckperms.common.api.ApiProvider;
import me.lucko.luckperms.common.caching.handlers.CachedStateManager;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.contexts.ContextManager;
import me.lucko.luckperms.common.contexts.ServerCalculator;
import me.lucko.luckperms.common.core.UuidCache;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.Importer;
import me.lucko.luckperms.common.debug.DebugHandler;
import me.lucko.luckperms.common.dependencies.DependencyManager;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.NoopLocaleManager;
import me.lucko.luckperms.common.locale.SimpleLocaleManager;
import me.lucko.luckperms.common.managers.GroupManager;
import me.lucko.luckperms.common.managers.TrackManager;
import me.lucko.luckperms.common.managers.UserManager;
import me.lucko.luckperms.common.managers.impl.GenericGroupManager;
import me.lucko.luckperms.common.managers.impl.GenericTrackManager;
import me.lucko.luckperms.common.managers.impl.GenericUserManager;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.messaging.RedisMessaging;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.LuckPermsScheduler;
import me.lucko.luckperms.common.storage.Storage;
import me.lucko.luckperms.common.storage.StorageFactory;
import me.lucko.luckperms.common.storage.StorageType;
import me.lucko.luckperms.common.tasks.CacheHousekeepingTask;
import me.lucko.luckperms.common.tasks.ExpireTemporaryTask;
import me.lucko.luckperms.common.tasks.UpdateTask;
import me.lucko.luckperms.common.treeview.PermissionVault;
import me.lucko.luckperms.common.utils.BufferedRequest;
import me.lucko.luckperms.common.utils.LoggerImpl;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Getter
public class LPBungeePlugin extends Plugin implements LuckPermsPlugin {
    private final Set<UUID> ignoringLogs = ConcurrentHashMap.newKeySet();
    private LuckPermsScheduler scheduler;
    private LuckPermsConfiguration configuration;
    private UserManager userManager;
    private GroupManager groupManager;
    private TrackManager trackManager;
    private Storage storage;
    private InternalMessagingService messagingService = null;
    private UuidCache uuidCache;
    private ApiProvider apiProvider;
    private Logger log;
    private Importer importer;
    private LocaleManager localeManager;
    private CachedStateManager cachedStateManager;
    private ContextManager<ProxiedPlayer> contextManager;
    private CalculatorFactory calculatorFactory;
    private BufferedRequest<Void> updateTaskBuffer;
    private DebugHandler debugHandler;
    private BungeeSenderFactory senderFactory;
    private PermissionVault permissionVault;

    @Override
    public void onEnable() {
        scheduler = new LPBungeeScheduler(this);
        localeManager = new NoopLocaleManager();
        senderFactory = new BungeeSenderFactory(this);
        log = new LoggerImpl(getConsoleSender());
        LuckPermsPlugin.sendStartupBanner(getConsoleSender(), this);
        debugHandler = new DebugHandler(scheduler.getAsyncExecutor(), getVersion());
        permissionVault = new PermissionVault(scheduler.getAsyncExecutor());

        getLog().info("Loading configuration...");
        configuration = new BungeeConfig(this);
        configuration.init();
        configuration.loadAll();

        Set<StorageType> storageTypes = StorageFactory.getRequiredTypes(this, StorageType.H2);
        DependencyManager.loadDependencies(this, storageTypes);

        // register events
        getProxy().getPluginManager().registerListener(this, new BungeeListener(this));

        // initialise datastore
        storage = StorageFactory.getInstance(this, StorageType.H2);

        // initialise messaging
        String messagingType = getConfiguration().get(ConfigKeys.MESSAGING_SERVICE).toLowerCase();
        if (messagingType.equals("none") && getConfiguration().get(ConfigKeys.REDIS_ENABLED)) {
            messagingType = "redis";
        }
        if (messagingType.equals("redis")) {
            getLog().info("Loading redis...");
            if (getConfiguration().get(ConfigKeys.REDIS_ENABLED)) {
                RedisMessaging redis = new RedisMessaging(this);
                try {
                    redis.init(getConfiguration().get(ConfigKeys.REDIS_ADDRESS), getConfiguration().get(ConfigKeys.REDIS_PASSWORD));
                    getLog().info("Loaded redis successfully...");

                    messagingService = redis;
                } catch (Exception e) {
                    getLog().warn("Couldn't load redis...");
                    e.printStackTrace();
                }
            } else {
                getLog().warn("Messaging Service was set to redis, but redis is not enabled!");
            }
        } else if (messagingType.equals("bungee")) {
            getLog().info("Loading bungee messaging service...");
            BungeeMessagingService bungeeMessaging = new BungeeMessagingService(this);
            bungeeMessaging.init();
            messagingService = bungeeMessaging;
        } else if (!messagingType.equals("none")) {
            getLog().warn("Messaging service '" + messagingType + "' not recognised.");
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
            getLog().info("Found locale file. Attempting to load from it.");
            try {
                localeManager.loadFromFile(locale);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // register commands
        getLog().info("Registering commands...");
        CommandManager commandManager = new CommandManager(this);
        getProxy().getPluginManager().registerCommand(this, new BungeeCommand(this, commandManager));

        // disable the default Bungee /perms command so it gets handled by the Bukkit plugin
        getProxy().getDisabledCommands().add("perms");

        // load internal managers
        getLog().info("Loading internal permission managers...");
        uuidCache = new UuidCache(this);
        userManager = new GenericUserManager(this);
        groupManager = new GenericGroupManager(this);
        trackManager = new GenericTrackManager();
        importer = new Importer(commandManager);
        calculatorFactory = new BungeeCalculatorFactory(this);
        cachedStateManager = new CachedStateManager(this);

        contextManager = new ContextManager<>();
        BackendServerCalculator serverCalculator = new BackendServerCalculator();
        getProxy().getPluginManager().registerListener(this, serverCalculator);
        contextManager.registerCalculator(serverCalculator);
        contextManager.registerCalculator(new ServerCalculator<>(configuration));

        // register with the LP API
        getLog().info("Registering API...");
        apiProvider = new ApiProvider(this);
        ApiHandler.registerProvider(apiProvider);

        // schedule update tasks
        int mins = getConfiguration().get(ConfigKeys.SYNC_TIME);
        if (mins > 0) {
            long ticks = mins * 60 * 20;
            scheduler.doAsyncRepeating(() -> updateTaskBuffer.request(), ticks);
        }
        scheduler.doAsyncLater(() -> updateTaskBuffer.request(), 40L);

        // run an update instantly.
        updateTaskBuffer.requestDirectly();

        // register tasks
        scheduler.doAsyncRepeating(new ExpireTemporaryTask(this), 60L);
        scheduler.doAsyncRepeating(new CacheHousekeepingTask(this), 2400L);

        getLog().info("Successfully loaded.");
    }

    @Override
    public void onDisable() {
        getLog().info("Closing datastore...");
        storage.shutdown();

        if (messagingService != null) {
            getLog().info("Closing messaging service...");
            messagingService.close();
        }

        getLog().info("Unregistering API...");
        ApiHandler.unregisterProvider();

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
    public Contexts getContextForUser(User user) {
        ProxiedPlayer player = getPlayer(user);
        if (player == null) {
            return null;
        }
        return new Contexts(
                getContextManager().getApplicableContext(player),
                getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_PERMS),
                getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_WORLD_PERMS),
                true,
                getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_GROUPS),
                getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_WORLD_GROUPS),
                false
        );
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
        return getProxy().getPlayer(external) != null;
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
        Set<ContextSet> c = new HashSet<>();
        c.add(ContextSet.empty());
        c.add(ContextSet.singleton("server", getConfiguration().get(ConfigKeys.SERVER)));
        c.addAll(getProxy().getServers().values().stream()
                .map(ServerInfo::getName)
                .map(s -> {
                    MutableContextSet set = MutableContextSet.create();
                    set.add("server", getConfiguration().get(ConfigKeys.SERVER));
                    set.add("world", s);
                    return set.makeImmutable();
                })
                .collect(Collectors.toList())
        );

        return c.stream()
                .map(set -> new Contexts(
                        set,
                        getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_PERMS),
                        getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_WORLD_PERMS),
                        true,
                        getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_GROUPS),
                        getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_WORLD_GROUPS),
                        false
                ))
                .collect(Collectors.toSet());
    }

    @Override
    public UUID getUuidFromUsername(String playerName) {
        return null; // Not needed on Bungee
    }
}
