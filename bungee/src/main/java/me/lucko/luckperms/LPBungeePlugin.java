package me.lucko.luckperms;

import lombok.Getter;
import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.implementation.ApiProvider;
import me.lucko.luckperms.commands.CommandManager;
import me.lucko.luckperms.data.Datastore;
import me.lucko.luckperms.data.methods.FlatfileDatastore;
import me.lucko.luckperms.data.methods.MySQLDatastore;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.listeners.PlayerListener;
import me.lucko.luckperms.runnables.UpdateTask;
import me.lucko.luckperms.tracks.TrackManager;
import me.lucko.luckperms.users.BungeeUserManager;
import me.lucko.luckperms.users.UserManager;
import me.lucko.luckperms.utils.LPConfiguration;
import me.lucko.luckperms.utils.LogUtil;
import me.lucko.luckperms.utils.UuidCache;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class LPBungeePlugin extends Plugin implements LuckPermsPlugin {
    private LPConfiguration configuration;
    private UserManager userManager;
    private GroupManager groupManager;
    private TrackManager trackManager;
    private Datastore datastore;
    private UuidCache uuidCache;

    @Override
    public void onEnable() {
        getLog().info("Loading configuration...");
        configuration = new BungeeConfig(this);

        // register events
        getProxy().getPluginManager().registerListener(this, new PlayerListener(this));

        // register commands
        getLog().info("Registering commands...");
        getProxy().getPluginManager().registerCommand(this, new BungeeCommand(new CommandManager(this)));

        // disable the default Bungee /perms command so it gets handled by the Bukkit plugin
        getProxy().getDisabledCommands().add("perms");

        getLog().info("Detecting storage method...");
        final String storageMethod = configuration.getStorageMethod();
        if (storageMethod.equalsIgnoreCase("mysql")) {
            getLog().info("Using MySQL as storage method.");
            datastore = new MySQLDatastore(this, configuration.getDatabaseValues());
        } else if (storageMethod.equalsIgnoreCase("flatfile")) {
            getLog().info("Using Flatfile (JSON) as storage method.");
            datastore = new FlatfileDatastore(this, getDataFolder());
        } else {
            getLog().severe("Storage method '" + storageMethod + "' was not recognised. Using Flatfile as fallback.");
            datastore = new FlatfileDatastore(this, getDataFolder());
        }

        getLog().info("Initialising datastore...");
        datastore.init();

        getLog().info("Loading internal permission managers...");
        uuidCache = new UuidCache(getConfiguration().getOnlineMode());
        userManager = new BungeeUserManager(this);
        groupManager = new GroupManager(this);
        trackManager = new TrackManager();

        // Run update task to refresh any online users
        getLog().info("Scheduling Update Task to refresh any online users.");
        try {
            new UpdateTask(this).run();
        } catch (Exception e) {
            e.printStackTrace();
        }

        int mins = getConfiguration().getSyncTime();
        if (mins > 0) {
            getProxy().getScheduler().schedule(this, new UpdateTask(this), mins, mins, TimeUnit.MINUTES);
        }

        getLog().info("Registering API...");
        LuckPerms.registerProvider(new ApiProvider(this));

        getLog().info("Successfully loaded.");
    }

    @Override
    public void onDisable() {
        getLog().info("Closing datastore...");
        datastore.shutdown();

        getLog().info("Unregistering API...");
        LuckPerms.unregisterProvider();
    }

    @Override
    public Logger getLog() {
        return LogUtil.wrap(getLogger());
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public String getPlayerStatus(UUID uuid) {
        return getProxy().getPlayer(getUuidCache().getExternalUUID(uuid)) != null ? "&aOnline" : "&cOffline";
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
