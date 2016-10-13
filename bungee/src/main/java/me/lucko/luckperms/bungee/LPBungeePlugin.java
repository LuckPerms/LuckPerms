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
import me.lucko.luckperms.ApiHandler;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.PlatformType;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.api.ApiProvider;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.commands.ConsecutiveExecutor;
import me.lucko.luckperms.common.commands.Sender;
import me.lucko.luckperms.common.config.LPConfiguration;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.contexts.ContextManager;
import me.lucko.luckperms.common.contexts.ServerCalculator;
import me.lucko.luckperms.common.core.UuidCache;
import me.lucko.luckperms.common.data.Importer;
import me.lucko.luckperms.common.groups.GroupManager;
import me.lucko.luckperms.common.runnables.ExpireTemporaryTask;
import me.lucko.luckperms.common.runnables.UpdateTask;
import me.lucko.luckperms.common.storage.Datastore;
import me.lucko.luckperms.common.storage.StorageFactory;
import me.lucko.luckperms.common.tracks.TrackManager;
import me.lucko.luckperms.common.users.UserManager;
import me.lucko.luckperms.common.utils.LocaleManager;
import me.lucko.luckperms.common.utils.LogFactory;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class LPBungeePlugin extends Plugin implements LuckPermsPlugin {
    private final Set<UUID> ignoringLogs = ConcurrentHashMap.newKeySet();
    private LPConfiguration configuration;
    private UserManager userManager;
    private GroupManager groupManager;
    private TrackManager trackManager;
    private Datastore datastore;
    private UuidCache uuidCache;
    private ApiProvider apiProvider;
    private Logger log;
    private Importer importer;
    private ConsecutiveExecutor consecutiveExecutor;
    private LocaleManager localeManager;
    private ContextManager<ProxiedPlayer> contextManager;
    private CalculatorFactory calculatorFactory;

    @Override
    public void onEnable() {
        log = LogFactory.wrap(getLogger());

        getLog().info("Loading configuration...");
        configuration = new BungeeConfig(this);

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

        // register events
        getProxy().getPluginManager().registerListener(this, new BungeeListener(this));

        // register commands
        getLog().info("Registering commands...");
        CommandManager commandManager = new CommandManager(this);
        getProxy().getPluginManager().registerCommand(this, new BungeeCommand(commandManager));

        // disable the default Bungee /perms command so it gets handled by the Bukkit plugin
        getProxy().getDisabledCommands().add("perms");

        datastore = StorageFactory.getDatastore(this, "h2");

        getLog().info("Loading internal permission managers...");
        uuidCache = new UuidCache(getConfiguration().isOnlineMode());
        userManager = new UserManager(this);
        groupManager = new GroupManager(this);
        trackManager = new TrackManager();
        importer = new Importer(commandManager);
        consecutiveExecutor = new ConsecutiveExecutor(commandManager);
        calculatorFactory = new BungeeCalculatorFactory(this);

        contextManager = new ContextManager<>();
        BackendServerCalculator serverCalculator = new BackendServerCalculator();
        getProxy().getPluginManager().registerListener(this, serverCalculator);
        contextManager.registerCalculator(serverCalculator);
        contextManager.registerCalculator(new ServerCalculator<>(getConfiguration().getServer()));

        int mins = getConfiguration().getSyncTime();
        if (mins > 0) {
            getProxy().getScheduler().schedule(this, new UpdateTask(this), mins, mins, TimeUnit.MINUTES);
        }

        // 20 times per second (once per "tick")
        getProxy().getScheduler().schedule(this, BungeeSenderFactory.get(this), 50L, 50L, TimeUnit.MILLISECONDS);
        getProxy().getScheduler().schedule(this, new ExpireTemporaryTask(this), 3L, 3L, TimeUnit.SECONDS);
        getProxy().getScheduler().schedule(this, consecutiveExecutor, 1L, 1L, TimeUnit.SECONDS);

        getLog().info("Registering API...");
        apiProvider = new ApiProvider(this);
        ApiHandler.registerProvider(apiProvider);

        // Run update task to refresh any online users
        getLog().info("Scheduling Update Task to refresh any online users.");
        try {
            new UpdateTask(this).run();
        } catch (Exception e) {
            e.printStackTrace();
        }

        getLog().info("Successfully loaded.");
    }

    @Override
    public void onDisable() {
        getLog().info("Closing datastore...");
        datastore.shutdown();

        getLog().info("Unregistering API...");
        ApiHandler.unregisterProvider();
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public PlatformType getType() {
        return PlatformType.BUNGEE;
    }

    @Override
    public File getMainDir() {
        return getDataFolder();
    }

    @Override
    public Message getPlayerStatus(UUID uuid) {
        return getProxy().getPlayer(getUuidCache().getExternalUUID(uuid)) != null ? Message.PLAYER_ONLINE : Message.PLAYER_OFFLINE;
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
    public List<Sender> getNotifyListeners() {
        return getProxy().getPlayers().stream()
                .map(p -> BungeeSenderFactory.get(this).wrap(p, Collections.singleton(Permission.LOG_NOTIFY)))
                .filter(Permission.LOG_NOTIFY::isAuthorized)
                .collect(Collectors.toList());
    }

    @Override
    public Sender getConsoleSender() {
        return BungeeSenderFactory.get(this).wrap(getProxy().getConsole());
    }

    @Override
    public Set<Contexts> getPreProcessContexts(boolean op) {
        Set<Map<String, String>> c = new HashSet<>();
        c.add(Collections.emptyMap());
        c.add(Collections.singletonMap("server", getConfiguration().getServer()));
        c.addAll(getProxy().getServers().values().stream()
                .map(ServerInfo::getName)
                .map(s -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("server", getConfiguration().getServer());
                    map.put("world", s);
                    return map;
                })
                .collect(Collectors.toList())
        );

        return c.stream()
                .map(map -> new Contexts(
                        map,
                        getConfiguration().isIncludingGlobalPerms(),
                        getConfiguration().isIncludingGlobalWorldPerms(),
                        true,
                        getConfiguration().isApplyingGlobalGroups(),
                        getConfiguration().isApplyingGlobalWorldGroups()
                ))
                .collect(Collectors.toSet());
    }

    @Override
    public Object getPlugin(String name) {
        return getProxy().getPluginManager().getPlugin(name);
    }

    @Override
    public Object getService(Class clazz) {
        return null;
    }

    @Override
    public UUID getUUID(String playerName) {
        return null; // Not needed on Bungee
    }

    @Override
    public boolean isPluginLoaded(String name) {
        return getProxy().getPluginManager().getPlugins().stream()
                .filter(p -> p.getDescription().getName().equalsIgnoreCase(name))
                .findAny()
                .isPresent();
    }

    @Override
    public void runUpdateTask() {
        doAsync(new UpdateTask(this));
    }

    @Override
    public void doAsync(Runnable r) {
        getProxy().getScheduler().runAsync(this, r);
    }

    @Override
    public void doSync(Runnable r) {
        r.run();
    }
}
