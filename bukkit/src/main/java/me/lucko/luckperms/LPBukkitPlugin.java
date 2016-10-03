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

package me.lucko.luckperms;

import lombok.Getter;
import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.PlatformType;
import me.lucko.luckperms.api.implementation.ApiProvider;
import me.lucko.luckperms.api.vault.VaultHook;
import me.lucko.luckperms.calculators.DefaultsProvider;
import me.lucko.luckperms.commands.ConsecutiveExecutor;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.config.LPConfiguration;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.contexts.ContextManager;
import me.lucko.luckperms.contexts.ServerCalculator;
import me.lucko.luckperms.contexts.WorldCalculator;
import me.lucko.luckperms.core.UuidCache;
import me.lucko.luckperms.data.Importer;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.runnables.ExpireTemporaryTask;
import me.lucko.luckperms.runnables.UpdateTask;
import me.lucko.luckperms.storage.Datastore;
import me.lucko.luckperms.storage.StorageFactory;
import me.lucko.luckperms.tracks.TrackManager;
import me.lucko.luckperms.users.BukkitUserManager;
import me.lucko.luckperms.utils.LocaleManager;
import me.lucko.luckperms.utils.LogFactory;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Getter
public class LPBukkitPlugin extends JavaPlugin implements LuckPermsPlugin {
    private VaultHook vaultHook = null;

    private final Set<UUID> ignoringLogs = ConcurrentHashMap.newKeySet();
    private LPConfiguration configuration;
    private BukkitUserManager userManager;
    private GroupManager groupManager;
    private TrackManager trackManager;
    private Datastore datastore;
    private UuidCache uuidCache;
    private ApiProvider apiProvider;
    private Logger log;
    private Importer importer;
    private ConsecutiveExecutor consecutiveExecutor;
    private DefaultsProvider defaultsProvider;
    private LocaleManager localeManager;
    private ContextManager<Player> contextManager;
    private WorldCalculator worldCalculator;

    @Override
    public void onEnable() {
        log = LogFactory.wrap(getLogger());

        getLog().info("Loading configuration...");
        configuration = new BukkitConfig(this);

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

        defaultsProvider = new DefaultsProvider();
        getServer().getScheduler().runTaskLater(this, () -> defaultsProvider.refresh(), 1L);

        // register events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new BukkitListener(this), this);

        // register commands
        getLog().info("Registering commands...");
        BukkitCommand commandManager = new BukkitCommand(this);
        PluginCommand main = getServer().getPluginCommand("luckperms");
        main.setExecutor(commandManager);
        main.setTabCompleter(commandManager);
        main.setAliases(Arrays.asList("perms", "lp", "permissions", "p", "perm"));

        datastore = StorageFactory.getDatastore(this, "h2");

        getLog().info("Loading internal permission managers...");
        uuidCache = new UuidCache(getConfiguration().isOnlineMode());
        userManager = new BukkitUserManager(this);
        groupManager = new GroupManager(this);
        trackManager = new TrackManager();
        importer = new Importer(commandManager);
        consecutiveExecutor = new ConsecutiveExecutor(commandManager);

        contextManager = new ContextManager<>();
        worldCalculator = new WorldCalculator(this);
        pm.registerEvents(worldCalculator, this);
        contextManager.registerCalculator(worldCalculator);
        contextManager.registerCalculator(new ServerCalculator<>(getConfiguration().getServer()));
        contextManager.registerListener(userManager);

        int mins = getConfiguration().getSyncTime();
        if (mins > 0) {
            long ticks = mins * 60 * 20;
            getServer().getScheduler().runTaskTimerAsynchronously(this, new UpdateTask(this), ticks, ticks);
        }

        getServer().getScheduler().runTaskTimer(this, BukkitSenderFactory.get(this), 1L, 1L);
        getServer().getScheduler().runTaskTimerAsynchronously(this, new ExpireTemporaryTask(this), 60L, 60L);
        getServer().getScheduler().runTaskTimerAsynchronously(this, consecutiveExecutor, 20L, 20L);

        // Provide vault support
        getLog().info("Attempting to hook into Vault...");
        try {
            if (getServer().getPluginManager().isPluginEnabled("Vault")) {
                vaultHook = new VaultHook();
                vaultHook.hook(this);
                getLog().info("Registered Vault permission & chat hook.");
            } else {
                getLog().info("Vault not found.");
            }
        } catch (Exception e) {
            getLog().severe("Error occurred whilst hooking into Vault.");
            e.printStackTrace();
        }

        getLog().info("Registering API...");
        apiProvider = new ApiProvider(this);
        LuckPerms.registerProvider(apiProvider);
        getServer().getServicesManager().register(LuckPermsApi.class, apiProvider, this, ServicePriority.Normal);

        // Run update task to refresh any online users
        getLog().info("Scheduling Update Task to refresh any online users.");
        try {
            new UpdateTask(this).run();
        } catch (Exception e) {
            e.printStackTrace();
        }

        registerPermissions(getConfiguration().isCommandsAllowOp() ? PermissionDefault.OP : PermissionDefault.FALSE);
        if (!getConfiguration().isOpsEnabled()) {
            getServer().getOperators().forEach(o -> o.setOp(false));
        }

        getLog().info("Successfully loaded.");
    }

    @Override
    public void onDisable() {
        getLog().info("Closing datastore...");
        datastore.shutdown();

        getLog().info("Unregistering API...");
        LuckPerms.unregisterProvider();
        getServer().getServicesManager().unregisterAll(this);

        if (vaultHook != null) {
            vaultHook.unhook(this);
        }
    }

    @Override
    public void doAsync(Runnable r) {
        getServer().getScheduler().runTaskAsynchronously(this, r);
    }

    @Override
    public void doSync(Runnable r) {
        getServer().getScheduler().runTask(this, r);
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
    public File getMainDir() {
        return getDataFolder();
    }

    @Override
    public Message getPlayerStatus(UUID uuid) {
        return getServer().getPlayer(getUuidCache().getExternalUUID(uuid)) != null ? Message.PLAYER_ONLINE : Message.PLAYER_OFFLINE;
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
    public List<Sender> getNotifyListeners() {
        return getServer().getOnlinePlayers().stream()
                .map(p -> BukkitSenderFactory.get(this).wrap(p, Collections.singleton(me.lucko.luckperms.constants.Permission.LOG_NOTIFY)))
                .filter(me.lucko.luckperms.constants.Permission.LOG_NOTIFY::isAuthorized)
                .collect(Collectors.toList());
    }

    @Override
    public Sender getConsoleSender() {
        return BukkitSenderFactory.get(this).wrap(getServer().getConsoleSender());
    }

    @Override
    public List<String> getPossiblePermissions() {
        final List<String> perms = new ArrayList<>();

        getServer().getPluginManager().getPermissions().forEach(p -> {
            perms.add(p.getName());
            p.getChildren().keySet().forEach(perms::add);
        });

        return perms;
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

    @Override
    public void runUpdateTask() {
        getServer().getScheduler().runTaskAsynchronously(this, new UpdateTask(this));
    }

    private void registerPermissions(PermissionDefault def) {
        PluginManager pm = getServer().getPluginManager();

        Map<String, List<String>> wildcards = new HashMap<>();
        List<String> all = new ArrayList<>();
        for (me.lucko.luckperms.constants.Permission p : me.lucko.luckperms.constants.Permission.values()) {
            pm.addPermission(new Permission(p.getNode(), def));
            if (p.getGroup() != null) {
                if (!wildcards.containsKey(p.getGroup())) {
                    wildcards.put(p.getGroup(), new ArrayList<>());
                }
                wildcards.get(p.getGroup()).add(p.getTag());
            }

            all.add(p.getNode());
        }

        for (Map.Entry<String, List<String>> e : wildcards.entrySet()) {
            pm.addPermission(new Permission(
                    "luckperms." + e.getKey() + ".*", def,
                    e.getValue().stream()
                            .map(tag -> "luckperms." + e.getKey() + "." + tag)
                            .collect(Collectors.toMap(s -> s, s -> true))
                    )
            );
        }

        pm.addPermission(new Permission("luckperms.*", def, all.stream().collect(Collectors.toMap(s -> s, s -> true))));
    }
}
