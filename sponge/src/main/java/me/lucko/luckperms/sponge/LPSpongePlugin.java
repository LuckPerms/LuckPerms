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

package me.lucko.luckperms.sponge;

import lombok.Getter;

import com.google.inject.Inject;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.PlatformType;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.api.ApiHandler;
import me.lucko.luckperms.common.api.ApiProvider;
import me.lucko.luckperms.common.caching.handlers.CachedStateManager;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.commands.BaseCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.config.LPConfiguration;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.contexts.ContextManager;
import me.lucko.luckperms.common.contexts.ServerCalculator;
import me.lucko.luckperms.common.core.UuidCache;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.Importer;
import me.lucko.luckperms.common.dependencies.DependencyManager;
import me.lucko.luckperms.common.managers.TrackManager;
import me.lucko.luckperms.common.managers.impl.GenericTrackManager;
import me.lucko.luckperms.common.messaging.RedisMessaging;
import me.lucko.luckperms.common.storage.Storage;
import me.lucko.luckperms.common.storage.StorageFactory;
import me.lucko.luckperms.common.storage.StorageType;
import me.lucko.luckperms.common.tasks.CacheHousekeepingTask;
import me.lucko.luckperms.common.tasks.ExpireTemporaryTask;
import me.lucko.luckperms.common.tasks.UpdateTask;
import me.lucko.luckperms.common.utils.BufferedRequest;
import me.lucko.luckperms.common.utils.DebugHandler;
import me.lucko.luckperms.common.utils.LocaleManager;
import me.lucko.luckperms.common.utils.LogFactory;
import me.lucko.luckperms.common.utils.PermissionCache;
import me.lucko.luckperms.sponge.commands.SpongeMainCommand;
import me.lucko.luckperms.sponge.contexts.WorldCalculator;
import me.lucko.luckperms.sponge.managers.SpongeGroupManager;
import me.lucko.luckperms.sponge.managers.SpongeUserManager;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.ServiceCacheHousekeepingTask;
import me.lucko.luckperms.sponge.service.base.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.persisted.PersistedCollection;
import me.lucko.luckperms.sponge.timings.LPTimings;
import me.lucko.luckperms.sponge.utils.VersionData;

import org.slf4j.Logger;
import org.spongepowered.api.Game;
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
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.AsynchronousExecutor;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.scheduler.SynchronousExecutor;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.text.Text;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Getter
@Plugin(id = "luckperms", name = "LuckPerms", version = VersionData.VERSION, authors = {"Luck"}, description = "A permissions plugin")
public class LPSpongePlugin implements LuckPermsPlugin {

    private final Set<UUID> ignoringLogs = ConcurrentHashMap.newKeySet();
    private final Set<Runnable> shutdownHooks = Collections.synchronizedSet(new HashSet<>());

    @Inject
    private Logger logger;

    @Inject
    private Game game;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;

    private Scheduler scheduler = Sponge.getScheduler();

    @Inject
    @SynchronousExecutor
    private SpongeExecutorService syncExecutor;

    @Inject
    @AsynchronousExecutor
    private SpongeExecutorService asyncExecutor;

    private LPTimings timings;
    private boolean lateLoad = false;

    private LPConfiguration configuration;
    private SpongeUserManager userManager;
    private SpongeGroupManager groupManager;
    private TrackManager trackManager;
    private Storage storage;
    private RedisMessaging redisMessaging = null;
    private UuidCache uuidCache;
    private ApiProvider apiProvider;
    private me.lucko.luckperms.api.Logger log;
    private Importer importer;
    private LuckPermsService service;
    private LocaleManager localeManager;
    private CachedStateManager cachedStateManager;
    private ContextManager<Subject> contextManager;
    private CalculatorFactory calculatorFactory;
    private BufferedRequest<Void> updateTaskBuffer;
    private DebugHandler debugHandler;
    private SpongeSenderFactory senderFactory;
    private PermissionCache permissionCache;

    @Listener(order = Order.FIRST)
    public void onEnable(GamePreInitializationEvent event) {
        log = LogFactory.wrap(logger);
        debugHandler = new DebugHandler(asyncExecutor, getVersion());
        senderFactory = new SpongeSenderFactory(this);
        permissionCache = new PermissionCache(asyncExecutor);
        timings = new LPTimings(this);

        getLog().info("Loading configuration...");
        configuration = new SpongeConfig(this);

        Set<StorageType> storageTypes = StorageFactory.getRequiredTypes(this, StorageType.H2);
        DependencyManager.loadDependencies(this, storageTypes);

        // register events
        game.getEventManager().registerListeners(this, new SpongeListener(this));

        // initialise datastore
        storage = StorageFactory.getInstance(this, StorageType.H2);

        // initialise redis
        if (getConfiguration().isRedisEnabled()) {
            getLog().info("Loading redis...");
            redisMessaging = new RedisMessaging(this);
            try {
                redisMessaging.init(getConfiguration().getRedisAddress(), getConfiguration().getRedisPassword());
                getLog().info("Loaded redis successfully...");
            } catch (Exception e) {
                getLog().info("Couldn't load redis...");
                e.printStackTrace();
                redisMessaging = null;
            }
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
        localeManager = new LocaleManager();
        File locale = new File(getMainDir(), "lang.yml");
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
        SpongeCommand commandManager = new SpongeCommand(this);
        cmdService.register(this, commandManager, "luckperms", "perms", "lp", "permissions", "p", "perm");

        // load internal managers
        getLog().info("Loading internal permission managers...");
        uuidCache = new UuidCache(getConfiguration().isOnlineMode());
        userManager = new SpongeUserManager(this);
        groupManager = new SpongeGroupManager(this);
        trackManager = new GenericTrackManager();
        importer = new Importer(commandManager);
        calculatorFactory = new SpongeCalculatorFactory(this);
        cachedStateManager = new CachedStateManager(this);

        contextManager = new ContextManager<>();
        contextManager.registerCalculator(new ServerCalculator<>(getConfiguration().getServer()));
        contextManager.registerCalculator(new WorldCalculator(this));

        // register the PermissionService with Sponge
        getLog().info("Registering PermissionService...");
        service = new LuckPermsService(this);

        if (game.getPluginManager().getPlugin("permissionsex").isPresent()) {
            getLog().warn("Detected PermissionsEx - assuming it's loaded for migration.");
            getLog().warn("Delaying LuckPerms PermissionService registration.");
            lateLoad = true;
        } else {
            game.getServiceManager().setProvider(this, PermissionService.class, service);
        }

        // register with the LP API
        getLog().info("Registering API...");
        apiProvider = new ApiProvider(this);
        ApiHandler.registerProvider(apiProvider);
        game.getServiceManager().setProvider(this, LuckPermsApi.class, apiProvider);

        // schedule update tasks
        int mins = getConfiguration().getSyncTime();
        if (mins > 0) {
            Task t = scheduler.createTaskBuilder().async().interval(mins, TimeUnit.MINUTES).execute(new UpdateTask(this))
                    .submit(LPSpongePlugin.this);
            addShutdownHook(t::cancel);
        }

        // run an update instantly.
        updateTaskBuffer.requestDirectly();

        // register tasks
        Task t2 = scheduler.createTaskBuilder().async().intervalTicks(60L).execute(new ExpireTemporaryTask(this)).submit(this);
        Task t3 = scheduler.createTaskBuilder().async().intervalTicks(2400L).execute(new CacheHousekeepingTask(this)).submit(this);
        Task t4 = scheduler.createTaskBuilder().async().intervalTicks(2400L).execute(new ServiceCacheHousekeepingTask(service)).submit(this);
        Task t5 = scheduler.createTaskBuilder().async().intervalTicks(2400L).execute(() -> userManager.performCleanup()).submit(this);
        addShutdownHook(t2::cancel);
        addShutdownHook(t3::cancel);
        addShutdownHook(t4::cancel);
        addShutdownHook(t5::cancel);

        getLog().info("Successfully loaded.");
    }

    @Listener(order = Order.LATE)
    public void onLateEnable(GamePreInitializationEvent event) {
        if (lateLoad) {
            getLog().info("Providing late registration of PermissionService...");
            game.getServiceManager().setProvider(this, PermissionService.class, service);
        }
    }

    @Listener
    public void onDisable(GameStoppingServerEvent event) {
        getLog().info("Closing datastore...");
        storage.shutdown();

        if (redisMessaging != null) {
            getLog().info("Closing redis...");
            redisMessaging.shutdown();
        }

        getLog().info("Unregistering API...");
        ApiHandler.unregisterProvider();
    }

    @Listener
    public void onPostInit(GamePostInitializationEvent event) {
        // register permissions
        Optional<PermissionService> ps = game.getServiceManager().provide(PermissionService.class);
        if (!ps.isPresent()) {
            getLog().warn("Unable to register all LuckPerms permissions. PermissionService not available.");
            return;
        }

        final PermissionService p = ps.get();

        Optional<PermissionDescription.Builder> builder = p.newDescriptionBuilder(this);
        if (!builder.isPresent()) {
            getLog().warn("Unable to register all LuckPerms permissions. Description Builder not available.");
            return;
        }

        for (Permission perm : Permission.values()) {
            for (String node : perm.getNodes()) {
                registerPermission(p, node);
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public File getMainDir() {
        File base = configDir.toFile().getParentFile().getParentFile();
        File luckPermsDir = new File(base, "luckperms");
        luckPermsDir.mkdirs();
        return luckPermsDir;
    }

    @Override
    public File getDataFolder() {
        return getMainDir();
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
                getConfiguration().isIncludingGlobalPerms(),
                getConfiguration().isIncludingGlobalWorldPerms(),
                true,
                getConfiguration().isApplyingGlobalGroups(),
                getConfiguration().isApplyingGlobalWorldGroups(),
                false
        );
    }

    @Override
    public String getVersion() {
        return VersionData.VERSION;
    }

    @Override
    public PlatformType getType() {
        return PlatformType.SPONGE;
    }

    @Override
    public String getServerName() {
        return getGame().getPlatform().getType().name();
    }

    @Override
    public String getServerVersion() {
        return getGame().getPlatform().getApi().getVersion().orElse("null") + " - " + getGame().getPlatform().getImplementation().getVersion().orElse("null");
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
    public boolean isOnline(UUID external) {
        return game.getServer().getPlayer(external).isPresent();
    }

    @Override
    public List<Sender> getSenders() {
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
    public Object getPlugin(String name) {
        return game.getPluginManager().getPlugin(name).map(PluginContainer::getInstance).orElse(null);
    }

    @Override
    public Object getService(Class clazz) {
        return game.getServiceManager().provideUnchecked(clazz);
    }

    @Override
    public UUID getUUID(String playerName) {
        return game.getServer().getPlayer(playerName).map(Player::getUniqueId).orElse(null);
    }

    @Override
    public boolean isPluginLoaded(String name) {
        return game.getPluginManager().isLoaded(name);
    }

    @Override
    public void addShutdownHook(Runnable r) {
        shutdownHooks.add(r);
    }

    @Override
    public void doAsync(Runnable r) {
        scheduler.createTaskBuilder().async().execute(r).submit(this);
    }

    @Override
    public void doSync(Runnable r) {
        scheduler.createTaskBuilder().execute(r).submit(this);
    }

    @Override
    public void doAsyncRepeating(Runnable r, long interval) {
        scheduler.createTaskBuilder().async().intervalTicks(interval).execute(r).submit(this);
    }

    @Override
    public List<BaseCommand> getExtraCommands() {
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

    private void registerPermission(PermissionService p, String node) {
        Optional<PermissionDescription.Builder> builder = p.newDescriptionBuilder(this);
        if (!builder.isPresent()) return;

        try {
            builder.get().assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of(node)).id(node).register();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
}
