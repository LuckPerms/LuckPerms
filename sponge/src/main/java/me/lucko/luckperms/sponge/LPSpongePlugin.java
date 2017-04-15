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

import lombok.Getter;

import com.google.inject.Inject;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.PlatformType;
import me.lucko.luckperms.common.api.ApiHandler;
import me.lucko.luckperms.common.api.ApiProvider;
import me.lucko.luckperms.common.caching.handlers.CachedStateManager;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.commands.abstraction.Command;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.contexts.ContextManager;
import me.lucko.luckperms.common.contexts.StaticCalculator;
import me.lucko.luckperms.common.core.UuidCache;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.dependencies.DependencyManager;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.NoopLocaleManager;
import me.lucko.luckperms.common.locale.SimpleLocaleManager;
import me.lucko.luckperms.common.managers.TrackManager;
import me.lucko.luckperms.common.managers.impl.GenericTrackManager;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.messaging.NoopMessagingService;
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
import me.lucko.luckperms.common.utils.FileWatcher;
import me.lucko.luckperms.common.utils.LoggerImpl;
import me.lucko.luckperms.common.verbose.VerboseHandler;
import me.lucko.luckperms.sponge.commands.SpongeMainCommand;
import me.lucko.luckperms.sponge.contexts.WorldCalculator;
import me.lucko.luckperms.sponge.managers.SpongeGroupManager;
import me.lucko.luckperms.sponge.managers.SpongeUserManager;
import me.lucko.luckperms.sponge.messaging.BungeeMessagingService;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.ServiceCacheHousekeepingTask;
import me.lucko.luckperms.sponge.service.persisted.PersistedCollection;
import me.lucko.luckperms.sponge.service.proxy.LPSubjectCollection;
import me.lucko.luckperms.sponge.timings.LPTimings;
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
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.AsynchronousExecutor;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.scheduler.SynchronousExecutor;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.text.Text;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Getter
@Plugin(id = "luckperms", name = "LuckPerms", version = VersionData.VERSION, authors = {"Luck"}, description = "A permissions plugin")
public class LPSpongePlugin implements LuckPermsPlugin {

    private final Set<UUID> ignoringLogs = ConcurrentHashMap.newKeySet();

    @Inject
    private Logger logger;

    @Inject
    private Game game;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;

    private Scheduler spongeScheduler = Sponge.getScheduler();

    @Inject
    @SynchronousExecutor
    private SpongeExecutorService syncExecutorService;

    @Inject
    @AsynchronousExecutor
    private SpongeExecutorService asyncExecutorService;

    private LPTimings timings;
    private boolean lateLoad = false;

    private LuckPermsScheduler scheduler;
    private SpongeCommand commandManager;
    private LuckPermsConfiguration configuration;
    private SpongeUserManager userManager;
    private SpongeGroupManager groupManager;
    private TrackManager trackManager;
    private Storage storage;
    private FileWatcher fileWatcher = null;
    private InternalMessagingService messagingService = null;
    private UuidCache uuidCache;
    private ApiProvider apiProvider;
    private me.lucko.luckperms.api.Logger log;
    private LuckPermsService service;
    private LocaleManager localeManager;
    private CachedStateManager cachedStateManager;
    private ContextManager<Subject> contextManager;
    private CalculatorFactory calculatorFactory;
    private BufferedRequest<Void> updateTaskBuffer;
    private VerboseHandler verboseHandler;
    private SpongeSenderFactory senderFactory;
    private PermissionVault permissionVault;

    @Listener(order = Order.FIRST)
    public void onEnable(GamePreInitializationEvent event) {
        scheduler = new LPSpongeScheduler(this);
        localeManager = new NoopLocaleManager();
        senderFactory = new SpongeSenderFactory(this);
        log = new LoggerImpl(getConsoleSender());

        LuckPermsPlugin.sendStartupBanner(getConsoleSender(), this);
        verboseHandler = new VerboseHandler(scheduler.getAsyncExecutor(), getVersion());
        permissionVault = new PermissionVault(scheduler.getAsyncExecutor());
        timings = new LPTimings(this);

        getLog().info("Loading configuration...");
        configuration = new SpongeConfig(this);
        configuration.init();
        configuration.loadAll();

        Set<StorageType> storageTypes = StorageFactory.getRequiredTypes(this, StorageType.H2);
        DependencyManager.loadDependencies(this, storageTypes);

        // register events
        game.getEventManager().registerListeners(this, new SpongeListener(this));

        if (getConfiguration().get(ConfigKeys.WATCH_FILES)) {
            fileWatcher = new FileWatcher(this);
            getScheduler().doAsyncRepeating(fileWatcher, 30L);
        }

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

        if (messagingService == null) {
            messagingService = new NoopMessagingService();
        }

        // setup the update task buffer
        updateTaskBuffer = new BufferedRequest<Void>(1000L, this::doAsync) {
            @Override
            protected Void perform() {
                new UpdateTask(LPSpongePlugin.this).run();
                return null;
            }
        };

        // load locale
        localeManager = new SimpleLocaleManager();
        File locale = new File(getDataDirectory(), "lang.yml");
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
        CommandManager cmdService = game.getCommandManager();
        commandManager = new SpongeCommand(this);
        cmdService.register(this, commandManager, "luckperms", "lp", "perm", "perms", "permission", "permissions");

        // load internal managers
        getLog().info("Loading internal permission managers...");
        uuidCache = new UuidCache(this);
        userManager = new SpongeUserManager(this);
        groupManager = new SpongeGroupManager(this);
        trackManager = new GenericTrackManager(this);
        calculatorFactory = new SpongeCalculatorFactory(this);
        cachedStateManager = new CachedStateManager(this);

        contextManager = new ContextManager<>();
        contextManager.registerCalculator(new StaticCalculator<>(configuration));
        contextManager.registerCalculator(new WorldCalculator());

        // register the PermissionService with Sponge
        getLog().info("Registering PermissionService...");
        service = new LuckPermsService(this);

        if (game.getPluginManager().getPlugin("permissionsex").isPresent()) {
            getLog().warn("Detected PermissionsEx - assuming it's loaded for migration.");
            getLog().warn("Delaying LuckPerms PermissionService registration.");
            lateLoad = true;
        } else {
            game.getServiceManager().setProvider(this, PermissionService.class, service);
            game.getServiceManager().setProvider(this, LuckPermsService.class, service);
        }

        // register with the LP API
        getLog().info("Registering API...");
        apiProvider = new ApiProvider(this);
        ApiHandler.registerProvider(apiProvider);
        game.getServiceManager().setProvider(this, LuckPermsApi.class, apiProvider);

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
        scheduler.doAsyncRepeating(new ServiceCacheHousekeepingTask(service), 2400L);
        scheduler.doAsyncRepeating(() -> userManager.performCleanup(), 2400L);

        getLog().info("Successfully loaded.");
    }

    @Listener(order = Order.LATE)
    public void onLateEnable(GamePreInitializationEvent event) {
        if (lateLoad) {
            getLog().info("Providing late registration of PermissionService...");
            game.getServiceManager().setProvider(this, PermissionService.class, service);
            game.getServiceManager().setProvider(this, LuckPermsService.class, service);
        }
    }

    @Listener
    public void onDisable(GameStoppingServerEvent event) {
        getLog().info("Closing datastore...");
        storage.shutdown();

        if (fileWatcher != null) {
            fileWatcher.close();
        }

        if (messagingService != null) {
            getLog().info("Closing messaging service...");
            messagingService.close();
        }

        getLog().info("Unregistering API...");
        ApiHandler.unregisterProvider();

        scheduler.shutdown();
    }

    @Listener
    public void onPostInit(GamePostInitializationEvent event) {
        // register permissions
        LuckPermsService service = this.service;
        if (service == null) {
            return;
        }

        for (Permission perm : Permission.values()) {
            for (String node : perm.getNodes()) {
                registerPermission(service, node);
            }
        }
    }

    @Override
    public void onPostUpdate() {
        for (LPSubjectCollection collection : service.getCollections().values()) {
            if (collection instanceof PersistedCollection) {
                ((PersistedCollection) collection).loadAll();
            }
        }
        service.invalidateParentCaches();
    }

    @Override
    public File getDataDirectory() {
        File base = configDir.toFile().getParentFile().getParentFile();
        File luckPermsDir = new File(base, "luckperms");
        luckPermsDir.mkdirs();
        return luckPermsDir;
    }

    @Override
    public File getConfigDirectory() {
        return configDir.toFile();
    }

    @Override
    public InputStream getResourceStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    @Override
    public Player getPlayer(User user) {
        return game.getServer().getPlayer(uuidCache.getExternalUUID(user.getUuid())).orElse(null);
    }

    @Override
    public Contexts getContextForUser(User user) {
        Player player = getPlayer(user);
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
    public String getVersion() {
        return VersionData.VERSION;
    }

    @Override
    public PlatformType getServerType() {
        return PlatformType.SPONGE;
    }

    @Override
    public String getServerName() {
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
        return game.getServer().getOnlinePlayers().size();
    }

    @Override
    public List<String> getPlayerList() {
        return game.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    @Override
    public Set<UUID> getOnlinePlayers() {
        return game.getServer().getOnlinePlayers().stream().map(Player::getUniqueId).collect(Collectors.toSet());
    }

    @Override
    public boolean isPlayerOnline(UUID external) {
        return game.getServer().getPlayer(external).map(Player::isOnline).orElse(false);
    }

    @Override
    public List<Sender> getOnlineSenders() {
        return game.getServer().getOnlinePlayers().stream()
                .map(s -> getSenderFactory().wrap(s))
                .collect(Collectors.toList());
    }

    @Override
    public Sender getConsoleSender() {
        return getSenderFactory().wrap(game.getServer().getConsole());
    }

    @Override
    public Set<Contexts> getPreProcessContexts(boolean op) {
        return Collections.emptySet();
    }

    @Override
    public List<Command> getExtraCommands() {
        return Collections.singletonList(new SpongeMainCommand(this));
    }

    @Override
    public LinkedHashMap<String, Object> getExtraInfo() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("SubjectCollection count", service.getCollections().size());
        map.put("Subject count",
                service.getCollections().values().stream()
                        .map(SubjectCollection::getAllSubjects)
                        .flatMap(subjects -> StreamSupport.stream(subjects.spliterator(), false))
                        .count()
        );
        map.put("PermissionDescription count", service.getDescriptions().size());
        return map;
    }

    private void registerPermission(LuckPermsService p, String node) {
        Optional<PermissionDescription.Builder> builder = p.newDescriptionBuilder(this);
        if (!builder.isPresent()) return;

        try {
            builder.get().assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of(node)).id(node).register();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
}
