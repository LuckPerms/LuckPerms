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

package me.lucko.luckperms.bukkit;

import lombok.Getter;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.PlatformType;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.bukkit.calculators.BukkitCalculatorFactory;
import me.lucko.luckperms.bukkit.contexts.BukkitContextManager;
import me.lucko.luckperms.bukkit.contexts.WorldCalculator;
import me.lucko.luckperms.bukkit.messaging.BukkitMessagingFactory;
import me.lucko.luckperms.bukkit.model.Injector;
import me.lucko.luckperms.bukkit.model.LPPermissible;
import me.lucko.luckperms.bukkit.processors.ChildPermissionProvider;
import me.lucko.luckperms.bukkit.processors.DefaultsProvider;
import me.lucko.luckperms.bukkit.vault.VaultHookManager;
import me.lucko.luckperms.common.actionlog.LogDispatcher;
import me.lucko.luckperms.common.api.ApiHandler;
import me.lucko.luckperms.common.api.ApiProvider;
import me.lucko.luckperms.common.buffers.BufferedRequest;
import me.lucko.luckperms.common.buffers.UpdateTaskBuffer;
import me.lucko.luckperms.common.caching.handlers.CachedStateManager;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.constants.CommandPermission;
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
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.Storage;
import me.lucko.luckperms.common.storage.StorageFactory;
import me.lucko.luckperms.common.storage.StorageType;
import me.lucko.luckperms.common.storage.backing.file.FileWatcher;
import me.lucko.luckperms.common.tasks.CacheHousekeepingTask;
import me.lucko.luckperms.common.tasks.ExpireTemporaryTask;
import me.lucko.luckperms.common.tasks.UpdateTask;
import me.lucko.luckperms.common.treeview.PermissionVault;
import me.lucko.luckperms.common.utils.LoginHelper;
import me.lucko.luckperms.common.utils.UuidCache;
import me.lucko.luckperms.common.verbose.VerboseHandler;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * LuckPerms implementation for the Bukkit API.
 */
@Getter
public class LPBukkitPlugin extends JavaPlugin implements LuckPermsPlugin {

    private long startTime;
    private LPBukkitScheduler scheduler;
    private BukkitCommand commandManager;
    private VaultHookManager vaultHookManager = null;
    private LuckPermsConfiguration configuration;
    private UserManager userManager;
    private GroupManager groupManager;
    private TrackManager trackManager;
    private Storage storage;
    private FileWatcher fileWatcher = null;
    private InternalMessagingService messagingService = null;
    private UuidCache uuidCache;
    private BukkitListener listener;
    private ApiProvider apiProvider;
    private Logger log;
    private DefaultsProvider defaultsProvider;
    private ChildPermissionProvider childPermissionProvider;
    private LocaleManager localeManager;
    private CachedStateManager cachedStateManager;
    private ContextManager<Player> contextManager;
    private CalculatorFactory calculatorFactory;
    private BufferedRequest<Void> updateTaskBuffer;
    private boolean started = false;
    private CountDownLatch enableLatch = new CountDownLatch(1);
    private VerboseHandler verboseHandler;
    private BukkitSenderFactory senderFactory;
    private PermissionVault permissionVault;
    private LogDispatcher logDispatcher;
    private Set<UUID> uniqueConnections = ConcurrentHashMap.newKeySet();

    @Override
    public void onLoad() {
        if (checkInvalidVersion()) {
            return;
        }

        // setup minimal functionality in order to load initial dependencies
        scheduler = new LPBukkitScheduler(this);
        localeManager = new NoopLocaleManager();
        senderFactory = new BukkitSenderFactory(this);
        log = new SenderLogger(this, getConsoleSender());

        DependencyManager.loadDependencies(this, Collections.singleton(Dependency.CAFFEINE));
    }

    @Override
    public void onEnable() {
        if (checkInvalidVersion()) {
            getLogger().severe("----------------------------------------------------------------------");
            getLogger().severe("Your server version is not compatible with this build of LuckPerms. :(");
            getLogger().severe("");
            getLogger().severe("If your server is running 1.8, please update to 1.8.8 or higher.");
            getLogger().severe("If your server is running 1.7.10, please download the Bukkit-Legacy version of LuckPerms from here:");
            getLogger().severe("==> https://ci.lucko.me/job/LuckPerms/");
            getLogger().severe("----------------------------------------------------------------------");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            enable();
            started = true;
        } finally {
            // count down the latch when onEnable has been called
            // we don't care about the result here
            enableLatch.countDown();
        }
    }

    private void enable() {
        startTime = System.currentTimeMillis();
        LuckPermsPlugin.sendStartupBanner(getConsoleSender(), this);
        verboseHandler = new VerboseHandler(scheduler.asyncBukkit(), getVersion());
        permissionVault = new PermissionVault(scheduler.asyncBukkit());
        logDispatcher = new LogDispatcher(this);

        getLog().info("Loading configuration...");
        configuration = new BukkitConfig(this);
        configuration.init();
        configuration.loadAll();

        Set<StorageType> storageTypes = StorageFactory.getRequiredTypes(this, StorageType.H2);
        DependencyManager.loadStorageDependencies(this, storageTypes);

        // setup the Bukkit defaults hook
        defaultsProvider = new DefaultsProvider();
        childPermissionProvider = new ChildPermissionProvider();

        // give all plugins a chance to load their permissions, then refresh.
        scheduler.syncLater(() -> {
            defaultsProvider.refresh();
            childPermissionProvider.setup();

            Set<String> perms = new HashSet<>();
            getServer().getPluginManager().getPermissions().forEach(p -> {
                perms.add(p.getName());
                perms.addAll(p.getChildren().keySet());
            });

            perms.forEach(p -> permissionVault.offer(p));
        }, 1L);

        // register events
        listener = new BukkitListener(this);
        getServer().getPluginManager().registerEvents(listener, this);

        if (getConfiguration().get(ConfigKeys.WATCH_FILES)) {
            fileWatcher = new FileWatcher(this);
            getScheduler().asyncRepeating(fileWatcher, 30L);
        }

        // initialise datastore
        storage = StorageFactory.getInstance(this, StorageType.H2);

        // initialise messaging
        messagingService = new BukkitMessagingFactory(this).getInstance();

        // setup the update task buffer
        updateTaskBuffer = new UpdateTaskBuffer(this);

        // load locale
        localeManager = new SimpleLocaleManager();
        localeManager.tryLoad(this, new File(getDataFolder(), "lang.yml"));

        // register commands
        commandManager = new BukkitCommand(this);
        PluginCommand main = getServer().getPluginCommand("luckperms");
        main.setExecutor(commandManager);
        main.setTabCompleter(commandManager);
        main.setDescription("Manage permissions");
        main.setAliases(Arrays.asList("lp", "perm", "perms", "permission", "permissions"));

        // load internal managers
        getLog().info("Loading internal permission managers...");
        uuidCache = new UuidCache(this);
        userManager = new GenericUserManager(this);
        groupManager = new GenericGroupManager(this);
        trackManager = new GenericTrackManager(this);
        calculatorFactory = new BukkitCalculatorFactory(this);
        cachedStateManager = new CachedStateManager();

        // setup context manager
        contextManager = new BukkitContextManager(this);
        contextManager.registerCalculator(new WorldCalculator(this));
        contextManager.registerCalculator(new LuckPermsCalculator<>(getConfiguration()), true);

        // Provide vault support
        tryVaultHook(false);

        // register with the LP API
        apiProvider = new ApiProvider(this);
        ApiHandler.registerProvider(apiProvider);
        getServer().getServicesManager().register(LuckPermsApi.class, apiProvider, this, ServicePriority.Normal);


        // schedule update tasks
        int mins = getConfiguration().get(ConfigKeys.SYNC_TIME);
        if (mins > 0) {
            long ticks = mins * 60 * 20;
            scheduler.asyncRepeating(() -> updateTaskBuffer.request(), ticks);
        }
        scheduler.asyncLater(() -> updateTaskBuffer.request(), 40L);

        // run an update instantly.
        getLog().info("Performing initial data load...");
        new UpdateTask(this, true).run();

        // register tasks
        scheduler.asyncRepeating(new ExpireTemporaryTask(this), 60L);
        scheduler.asyncRepeating(new CacheHousekeepingTask(this), 2400L);

        // register permissions
        try {
            registerPermissions(getConfiguration().get(ConfigKeys.COMMANDS_ALLOW_OP) ? PermissionDefault.OP : PermissionDefault.FALSE);
        } catch (Exception e) {
            // this throws an exception if the plugin is /reloaded, grr
        }

        if (!getConfiguration().get(ConfigKeys.OPS_ENABLED)) {
            scheduler.doSync(() -> getServer().getOperators().forEach(o -> o.setOp(false)));
        }

        // replace the temporary executor when the Bukkit one starts
        getServer().getScheduler().runTaskAsynchronously(this, () -> scheduler.setUseBukkitAsync(true));

        // Load any online users (in the case of a reload)
        for (Player player : getServer().getOnlinePlayers()) {
            scheduler.doAsync(() -> {
                LoginHelper.loadUser(this, player.getUniqueId(), player.getName(), false);
                User user = getUserManager().getIfLoaded(getUuidCache().getUUID(player.getUniqueId()));
                if (user != null) {
                    scheduler.doSync(() -> {
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

        getLog().info("Successfully enabled. (took " + (System.currentTimeMillis() - startTime) + "ms)");
    }

    @Override
    public void onDisable() {
        if (checkInvalidVersion()) {
            return;
        }

        // Switch back to the LP executor, the bukkit one won't allow new tasks
        scheduler.setUseBukkitAsync(false);

        started = false;

        defaultsProvider.close();
        permissionVault.setShutdown(true);
        verboseHandler.setShutdown(true);

        for (Player player : getServer().getOnlinePlayers()) {
            try {
                Injector.unInject(player, false, false);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (getConfiguration().get(ConfigKeys.AUTO_OP)) {
                player.setOp(false);
            }

            final User user = getUserManager().getIfLoaded(getUuidCache().getUUID(player.getUniqueId()));
            if (user != null) {
                user.unregisterData();
                getUserManager().unload(user);
            }
        }

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
        getServer().getServicesManager().unregisterAll(this);

        if (vaultHookManager != null) {
            vaultHookManager.unhook(this);
        }

        getLog().info("Shutting down internal scheduler...");
        scheduler.shutdown();

        // Bukkit will do this again when #onDisable completes, but we do it early to prevent NPEs elsewhere.
        getServer().getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);

        // Null everything
        vaultHookManager = null;
        configuration = null;
        userManager = null;
        groupManager = null;
        trackManager = null;
        storage = null;
        fileWatcher = null;
        messagingService = null;
        uuidCache = null;
        listener = null;
        apiProvider = null;
        log = null;
        defaultsProvider = null;
        childPermissionProvider = null;
        localeManager = null;
        cachedStateManager = null;
        contextManager = null;
        calculatorFactory = null;
        updateTaskBuffer = null;
        verboseHandler = null;
        senderFactory = null;
        permissionVault = null;
        logDispatcher = null;
    }

    public void tryVaultHook(boolean force) {
        if (vaultHookManager != null) {
            return; // already hooked
        }

        try {
            if (force || getServer().getPluginManager().isPluginEnabled("Vault")) {
                vaultHookManager = new VaultHookManager();
                vaultHookManager.hook(this);
                getLog().info("Registered Vault permission & chat hook.");
            }
        } catch (Exception e) {
            vaultHookManager = null;
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

    public void refreshAutoOp(Player player) {
        if (getConfiguration().get(ConfigKeys.AUTO_OP)) {
            try {
                LPPermissible permissible = Injector.getPermissible(player.getUniqueId());
                if (permissible == null || !permissible.getActive().get()) {
                    return;
                }

                User user = permissible.getUser();
                if (user == null) {
                    return;
                }

                Map<String, Boolean> backing = user.getUserData().getPermissionData(permissible.calculateContexts()).getImmutableBacking();
                boolean op = Optional.ofNullable(backing.get("luckperms.autoop")).orElse(false);
                player.setOp(op);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public PlatformType getServerType() {
        return PlatformType.BUKKIT;
    }

    @Override
    public String getServerName() {
        return getServer().getName();
    }

    @Override
    public String getServerVersion() {
        return getServer().getVersion() + " - " + getServer().getBukkitVersion();
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
        return getServer().getPlayer(uuidCache.getExternalUUID(user.getUuid()));
    }

    @Override
    public Optional<UUID> lookupUuid(String username) {
        try {
            //noinspection deprecation
            return Optional.ofNullable(getServer().getOfflinePlayer(username)).flatMap(p -> Optional.ofNullable(p.getUniqueId()));
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public Contexts getContextForUser(User user) {
        Player player = getPlayer(user);
        if (player == null) {
            return null;
        }
        return contextManager.getApplicableContexts(player);
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
    public boolean isPlayerOnline(UUID external) {
        Player player = getServer().getPlayer(external);
        return player != null && player.isOnline();
    }

    @Override
    public List<Sender> getOnlineSenders() {
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
        c.add(ContextSet.singleton("server", getConfiguration().get(ConfigKeys.SERVER)));

        // Pre process all worlds
        c.addAll(getServer().getWorlds().stream()
                .map(World::getName)
                .map(s -> {
                    MutableContextSet set = MutableContextSet.create();
                    set.add("server", getConfiguration().get(ConfigKeys.SERVER));
                    set.add("world", s);
                    set.addAll(configuration.getContextsFile().getStaticContexts());
                    return set.makeImmutable();
                })
                .collect(Collectors.toList())
        );

        // Pre process the separate Vault server, if any
        if (!getConfiguration().get(ConfigKeys.SERVER).equals(getConfiguration().get(ConfigKeys.VAULT_SERVER))) {
            c.add(ContextSet.singleton("server", getConfiguration().get(ConfigKeys.VAULT_SERVER)));
            c.addAll(getServer().getWorlds().stream()
                    .map(World::getName)
                    .map(s -> {
                        MutableContextSet set = MutableContextSet.create();
                        set.add("server", getConfiguration().get(ConfigKeys.VAULT_SERVER));
                        set.add("world", s);
                        set.addAll(configuration.getContextsFile().getStaticContexts());
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
                        getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_PERMS),
                        getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_WORLD_PERMS),
                        true,
                        getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_GROUPS),
                        getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_WORLD_GROUPS),
                        op
                ))
                .collect(Collectors.toSet())
        );

        // Check for and include varying Vault config options
        boolean vaultDiff = getConfiguration().get(ConfigKeys.VAULT_INCLUDING_GLOBAL) != getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_PERMS) ||
                !getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_WORLD_PERMS) ||
                !getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_GROUPS) ||
                !getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_WORLD_GROUPS);

        if (vaultDiff) {
            contexts.addAll(c.stream()
                    .map(map -> new Contexts(map, getConfiguration().get(ConfigKeys.VAULT_INCLUDING_GLOBAL), true, true, true, true, op))
                    .collect(Collectors.toSet())
            );
        }

        return contexts;
    }

    @Override
    public LinkedHashMap<String, Object> getExtraInfo() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("Vault Enabled", vaultHookManager != null);
        map.put("Bukkit Defaults count", defaultsProvider.size());
        map.put("Bukkit Child Permissions count", childPermissionProvider.getPermissions().size());
        return map;
    }

    private void registerPermissions(PermissionDefault def) {
        PluginManager pm = getServer().getPluginManager();

        for (CommandPermission p : CommandPermission.values()) {
            pm.addPermission(new org.bukkit.permissions.Permission(p.getPermission(), def));
        }
    }

    private static boolean checkInvalidVersion() {
        try {
            Class.forName("com.google.gson.JsonElement");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
