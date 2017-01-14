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

package me.lucko.luckperms.bukkit;

import lombok.Getter;

import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.PlatformType;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.bukkit.calculators.AutoOPListener;
import me.lucko.luckperms.bukkit.inject.Injector;
import me.lucko.luckperms.bukkit.model.ChildPermissionProvider;
import me.lucko.luckperms.bukkit.model.DefaultsProvider;
import me.lucko.luckperms.bukkit.model.LPPermissible;
import me.lucko.luckperms.bukkit.vault.VaultHook;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.api.ApiHandler;
import me.lucko.luckperms.common.api.ApiProvider;
import me.lucko.luckperms.common.caching.handlers.CachedStateManager;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.config.LPConfiguration;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.contexts.ContextManager;
import me.lucko.luckperms.common.contexts.ServerCalculator;
import me.lucko.luckperms.common.core.UuidCache;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.Importer;
import me.lucko.luckperms.common.dependencies.DependencyManager;
import me.lucko.luckperms.common.managers.GroupManager;
import me.lucko.luckperms.common.managers.TrackManager;
import me.lucko.luckperms.common.managers.UserManager;
import me.lucko.luckperms.common.managers.impl.GenericGroupManager;
import me.lucko.luckperms.common.managers.impl.GenericTrackManager;
import me.lucko.luckperms.common.managers.impl.GenericUserManager;
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

import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Getter
public class LPBukkitPlugin extends JavaPlugin implements LuckPermsPlugin {
    private Set<UUID> ignoringLogs;
    private Executor syncExecutor;
    private Executor asyncExecutor;
    private VaultHook vaultHook = null;
    private LPConfiguration configuration;
    private UserManager userManager;
    private GroupManager groupManager;
    private TrackManager trackManager;
    private Storage storage;
    private RedisMessaging redisMessaging = null;
    private UuidCache uuidCache;
    private BukkitListener listener;
    private ApiProvider apiProvider;
    private Logger log;
    private Importer importer;
    private DefaultsProvider defaultsProvider;
    private ChildPermissionProvider childPermissionProvider;
    private LocaleManager localeManager;
    private CachedStateManager cachedStateManager;
    private ContextManager<Player> contextManager;
    private WorldCalculator worldCalculator;
    private CalculatorFactory calculatorFactory;
    private BufferedRequest<Void> updateTaskBuffer;
    private boolean started = false;
    private DebugHandler debugHandler;
    private BukkitSenderFactory senderFactory;
    private PermissionCache permissionCache;

    @Override
    public void onEnable() {
        // Used whilst the server is still starting
        asyncExecutor = Executors.newCachedThreadPool();
        syncExecutor = r -> getServer().getScheduler().runTask(this, r);
        Executor bukkitAsyncExecutor = r -> getServer().getScheduler().runTaskAsynchronously(this, r);

        log = LogFactory.wrap(getLogger());
        ignoringLogs = ConcurrentHashMap.newKeySet();
        debugHandler = new DebugHandler(bukkitAsyncExecutor, getVersion());
        senderFactory = new BukkitSenderFactory(this);
        permissionCache = new PermissionCache(bukkitAsyncExecutor);

        getLog().info("Loading configuration...");
        configuration = new BukkitConfig(this);

        Set<StorageType> storageTypes = StorageFactory.getRequiredTypes(this, StorageType.H2);
        DependencyManager.loadDependencies(this, storageTypes);

        // setup the Bukkit defaults hook
        defaultsProvider = new DefaultsProvider();
        childPermissionProvider = new ChildPermissionProvider();

        // give all plugins a chance to load their defaults, then refresh.
        getServer().getScheduler().runTaskLater(this, () -> {
            defaultsProvider.refresh();
            childPermissionProvider.setup();

            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                for (Map.Entry<String, Boolean> e : defaultsProvider.getOpDefaults().entrySet()) {
                    permissionCache.offer(e.getKey());
                }

                for (Map.Entry<String, Boolean> e : defaultsProvider.getNonOpDefaults().entrySet()) {
                    permissionCache.offer(e.getKey());
                }

                ImmutableMap<Map.Entry<String, Boolean>, ImmutableMap<String, Boolean>> permissions = childPermissionProvider.getPermissions();
                for (Map.Entry<Map.Entry<String, Boolean>, ImmutableMap<String, Boolean>> e : permissions.entrySet()) {
                    permissionCache.offer(e.getKey().getKey());
                    for (Map.Entry<String, Boolean> e1 : e.getValue().entrySet()) {
                        permissionCache.offer(e1.getKey());
                    }
                }
            });

        }, 1L);

        // register events
        PluginManager pm = getServer().getPluginManager();
        listener = new BukkitListener(this);
        pm.registerEvents(listener, this);

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
            }
        }

        // setup the update task buffer
        updateTaskBuffer = new BufferedRequest<Void>(1000L, this::doAsync) {
            @Override
            protected Void perform() {
                new UpdateTask(LPBukkitPlugin.this).run();
                return null;
            }
        };

        // load locale
        localeManager = new LocaleManager();
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
        BukkitCommand commandManager = new BukkitCommand(this);
        PluginCommand main = getServer().getPluginCommand("luckperms");
        main.setExecutor(commandManager);
        main.setTabCompleter(commandManager);
        main.setAliases(Arrays.asList("perms", "lp", "permissions", "perm"));

        // load internal managers
        getLog().info("Loading internal permission managers...");
        uuidCache = new UuidCache(getConfiguration().isOnlineMode());
        userManager = new GenericUserManager(this);
        groupManager = new GenericGroupManager(this);
        trackManager = new GenericTrackManager();
        importer = new Importer(commandManager);
        calculatorFactory = new BukkitCalculatorFactory(this);
        cachedStateManager = new CachedStateManager(this);

        contextManager = new ContextManager<>();
        worldCalculator = new WorldCalculator(this);
        pm.registerEvents(worldCalculator, this);
        contextManager.registerCalculator(worldCalculator);
        contextManager.registerCalculator(new ServerCalculator<>(getConfiguration().getServer()));

        // handle server operators
        if (getConfiguration().isAutoOp()) {
            contextManager.registerListener(new AutoOPListener());
        }

        // Provide vault support
        tryVaultHook(false);

        // register with the LP API
        getLog().info("Registering API...");
        apiProvider = new ApiProvider(this);
        ApiHandler.registerProvider(apiProvider);
        getServer().getServicesManager().register(LuckPermsApi.class, apiProvider, this, ServicePriority.Normal);


        // schedule update tasks
        int mins = getConfiguration().getSyncTime();
        if (mins > 0) {
            long ticks = mins * 60 * 20;
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> updateTaskBuffer.request(), 40L, ticks);
        }

        // run an update instantly.
        updateTaskBuffer.requestDirectly();

        // register tasks
        getServer().getScheduler().runTaskTimerAsynchronously(this, new ExpireTemporaryTask(this), 60L, 60L);
        getServer().getScheduler().runTaskTimerAsynchronously(this, new CacheHousekeepingTask(this), 2400L, 2400L);

        // register permissions
        registerPermissions(getConfiguration().isCommandsAllowOp() ? PermissionDefault.OP : PermissionDefault.FALSE);
        if (!getConfiguration().isOpsEnabled()) {
            getServer().getScheduler().runTask(this, () -> getServer().getOperators().forEach(o -> o.setOp(false)));
        }

        // replace the temporary executor when the Bukkit one starts
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            asyncExecutor = bukkitAsyncExecutor;
        });

        // Load any online users (in the case of a reload)
        for (Player player : getServer().getOnlinePlayers()) {
            doAsync(() -> {
                listener.onAsyncLogin(player.getUniqueId(), player.getName());
                User user = getUserManager().get(getUuidCache().getUUID(player.getUniqueId()));
                if (user != null) {
                    doSync(() -> {
                        try {
                            LPPermissible lpPermissible = new LPPermissible(player, user, this);
                            Injector.inject(player, lpPermissible);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    });
                }
            });
        }

        started = true;
        getLog().info("Successfully loaded.");
    }

    @Override
    public void onDisable() {
        started = false;

        defaultsProvider.close();
        permissionCache.setShutdown(true);
        debugHandler.setShutdown(true);

        for (Player player : getServer().getOnlinePlayers()) {
            Injector.unInject(player, false);
            if (getConfiguration().isAutoOp()) {
                player.setOp(false);
            }

            final User user = getUserManager().get(getUuidCache().getUUID(player.getUniqueId()));
            if (user != null) {
                user.unregisterData();
                getUserManager().unload(user);
            }
        }

        getLog().info("Closing datastore...");
        storage.shutdown();

        if (redisMessaging != null) {
            getLog().info("Closing redis...");
            redisMessaging.shutdown();
        }

        getLog().info("Unregistering API...");
        ApiHandler.unregisterProvider();
        getServer().getServicesManager().unregisterAll(this);

        if (vaultHook != null) {
            vaultHook.unhook(this);
        }

        // Bukkit will do this again when #onDisable completes, but we do it early to prevent NPEs elsewhere.
        getServer().getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);

        // Null everything
        ignoringLogs = null;
        syncExecutor = null;
        asyncExecutor = null;
        vaultHook = null;
        configuration = null;
        userManager = null;
        groupManager = null;
        trackManager = null;
        storage = null;
        redisMessaging = null;
        uuidCache = null;
        listener = null;
        apiProvider = null;
        log = null;
        importer = null;
        defaultsProvider = null;
        childPermissionProvider = null;
        localeManager = null;
        cachedStateManager = null;
        contextManager = null;
        worldCalculator = null;
        calculatorFactory = null;
        updateTaskBuffer = null;
        debugHandler = null;
        senderFactory = null;
        permissionCache = null;
    }

    public void tryVaultHook(boolean force) {
        if (vaultHook != null) {
            return;
        }

        getLog().info("Attempting to hook with Vault...");
        try {
            if (force || getServer().getPluginManager().isPluginEnabled("Vault")) {
                vaultHook = new VaultHook();
                vaultHook.hook(this);
                getLog().info("Registered Vault permission & chat hook.");
            } else {
                getLog().info("Vault not found.");
            }
        } catch (Exception e) {
            vaultHook = null;
            getLog().severe("Error occurred whilst hooking into Vault.");
            e.printStackTrace();
        }
    }

    @Override
    public void onUserRefresh(User user) {
        LPPermissible lpp = Injector.getPermissible(uuidCache.getExternalUUID(user.getUuid()));
        if (lpp != null) {
            lpp.updateSubscriptions();
        }
    }

    @Override
    public void doAsync(Runnable r) {
        asyncExecutor.execute(r);
    }

    @Override
    public void doSync(Runnable r) {
        syncExecutor.execute(r);
    }

    @Override
    public void doAsyncRepeating(Runnable r, long interval) {
        getServer().getScheduler().runTaskTimerAsynchronously(this, r, interval, interval);
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public PlatformType getType() {
        return PlatformType.BUKKIT;
    }

    @Override
    public String getServerName() {
        return getServer().getName();
    }

    @Override
    public String getServerVersion() {
        return getServer().getVersion();
    }

    @Override
    public File getMainDir() {
        return getDataFolder();
    }

    @Override
    public InputStream getResourceStream(String path) {
        return getResource(path);
    }

    @Override
    public Player getPlayer(User user) {
        return getServer().getPlayer(uuidCache.getExternalUUID(user.getUuid()));
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
                player.isOp()
        );
    }

    @Override
    public int getPlayerCount() {
        return getServer().getOnlinePlayers().size();
    }

    @Override
    public List<String> getPlayerList() {
        return getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    @Override
    public Set<UUID> getOnlinePlayers() {
        return getServer().getOnlinePlayers().stream().map(Player::getUniqueId).collect(Collectors.toSet());
    }

    @Override
    public boolean isOnline(UUID external) {
        return getServer().getPlayer(external) != null;
    }

    @Override
    public List<Sender> getSenders() {
        return getServer().getOnlinePlayers().stream()
                .map(p -> getSenderFactory().wrap(p))
                .collect(Collectors.toList());
    }

    @Override
    public Sender getConsoleSender() {
        return getSenderFactory().wrap(getServer().getConsoleSender());
    }

    @Override
    public Set<Contexts> getPreProcessContexts(boolean op) {
        Set<ContextSet> c = new HashSet<>();
        c.add(ContextSet.empty());
        c.add(ContextSet.singleton("server", getConfiguration().getServer()));

        // Pre process all worlds
        c.addAll(getServer().getWorlds().stream()
                .map(World::getName)
                .map(s -> {
                    MutableContextSet set = MutableContextSet.create();
                    set.add("server", getConfiguration().getServer());
                    set.add("world", s);
                    return set.makeImmutable();
                })
                .collect(Collectors.toList())
        );

        // Pre process the separate Vault server, if any
        if (!getConfiguration().getServer().equals(getConfiguration().getVaultServer())) {
            c.add(ContextSet.singleton("server", getConfiguration().getVaultServer()));
            c.addAll(getServer().getWorlds().stream()
                    .map(World::getName)
                    .map(s -> {
                        MutableContextSet set = MutableContextSet.create();
                        set.add("server", getConfiguration().getVaultServer());
                        set.add("world", s);
                        return set.makeImmutable();
                    })
                    .collect(Collectors.toList())
            );
        }

        Set<Contexts> contexts = new HashSet<>();

        // Convert to full Contexts
        contexts.addAll(c.stream()
                .map(set -> new Contexts(
                        set,
                        getConfiguration().isIncludingGlobalPerms(),
                        getConfiguration().isIncludingGlobalWorldPerms(),
                        true,
                        getConfiguration().isApplyingGlobalGroups(),
                        getConfiguration().isApplyingGlobalWorldGroups(),
                        op
                ))
                .collect(Collectors.toSet())
        );

        // Check for and include varying Vault config options
        boolean vaultDiff = getConfiguration().isVaultIncludingGlobal() != getConfiguration().isIncludingGlobalPerms() ||
                !getConfiguration().isIncludingGlobalWorldPerms() ||
                !getConfiguration().isApplyingGlobalGroups() ||
                !getConfiguration().isApplyingGlobalWorldGroups();

        if (vaultDiff) {
            contexts.addAll(c.stream()
                    .map(map -> new Contexts(map, getConfiguration().isVaultIncludingGlobal(), true, true, true, true, op))
                    .collect(Collectors.toSet())
            );
        }

        return contexts;
    }

    @Override
    public LinkedHashMap<String, Object> getExtraInfo() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("Vault Enabled", vaultHook != null);
        map.put("Vault Server", configuration.getVaultServer());
        map.put("Bukkit Defaults count", defaultsProvider.size());
        map.put("Bukkit Child Permissions count", childPermissionProvider.getPermissions().size());
        map.put("World Cache size", worldCalculator.getWorldCache().size());
        map.put("Vault Including Global", configuration.isVaultIncludingGlobal());
        map.put("Vault Ignoring World", configuration.isVaultIgnoreWorld());
        map.put("Vault Primary Group Overrides", configuration.isVaultPrimaryGroupOverrides());
        map.put("Vault Debug", configuration.isVaultDebug());
        map.put("OPs Enabled", configuration.isOpsEnabled());
        map.put("Auto OP", configuration.isAutoOp());
        map.put("Commands Allow OPs", configuration.isCommandsAllowOp());
        return map;
    }

    @Override
    public Object getPlugin(String name) {
        return getServer().getPluginManager().getPlugin(name);
    }

    @Override
    public Object getService(Class clazz) {
        return getServer().getServicesManager().load(clazz);
    }

    @SuppressWarnings("deprecation")
    @Override
    public UUID getUUID(String playerName) {
        try {
            return getServer().getOfflinePlayer(playerName).getUniqueId();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isPluginLoaded(String name) {
        return getServer().getPluginManager().isPluginEnabled(name);
    }

    private void registerPermissions(PermissionDefault def) {
        PluginManager pm = getServer().getPluginManager();

        for (Permission p : Permission.values()) {
            for (String node : p.getNodes()) {
                pm.addPermission(new org.bukkit.permissions.Permission(node, def));
            }
        }
    }
}
