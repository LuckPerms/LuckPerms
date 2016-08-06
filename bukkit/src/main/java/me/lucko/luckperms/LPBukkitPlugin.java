package me.lucko.luckperms;

import lombok.Getter;
import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.implementation.ApiProvider;
import me.lucko.luckperms.api.vault.VaultHook;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.data.Datastore;
import me.lucko.luckperms.data.methods.FlatfileDatastore;
import me.lucko.luckperms.data.methods.MySQLDatastore;
import me.lucko.luckperms.data.methods.SQLiteDatastore;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.runnables.UpdateTask;
import me.lucko.luckperms.tracks.TrackManager;
import me.lucko.luckperms.users.BukkitUserManager;
import me.lucko.luckperms.users.UserManager;
import me.lucko.luckperms.utils.LPConfiguration;
import me.lucko.luckperms.utils.LogUtil;
import me.lucko.luckperms.utils.UuidCache;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class LPBukkitPlugin extends JavaPlugin implements LuckPermsPlugin {
    private LPConfiguration configuration;
    private UserManager userManager;
    private GroupManager groupManager;
    private TrackManager trackManager;
    private Datastore datastore;
    private UuidCache uuidCache;
    private Logger log;

    @Override
    public void onEnable() {
        log = LogUtil.wrap(getLogger());

        getLog().info("Loading configuration...");
        configuration = new BukkitConfig(this);

        // register events
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new BukkitListener(this), this);

        // register commands
        getLog().info("Registering commands...");
        BukkitCommand commandManager = new BukkitCommand(this);
        PluginCommand main = getServer().getPluginCommand("luckperms");
        main.setExecutor(commandManager);
        main.setTabCompleter(commandManager);
        main.setAliases(Arrays.asList("perms", "lp", "permissions", "p", "perm"));

        getLog().info("Detecting storage method...");
        final String storageMethod = configuration.getStorageMethod();
        if (storageMethod.equalsIgnoreCase("mysql")) {
            getLog().info("Using MySQL as storage method.");
            datastore = new MySQLDatastore(this, configuration.getDatabaseValues());
        } else if (storageMethod.equalsIgnoreCase("sqlite")) {
            getLog().info("Using SQLite as storage method.");
            datastore = new SQLiteDatastore(this, new File(getDataFolder(), "luckperms.sqlite"));
        } else if (storageMethod.equalsIgnoreCase("flatfile")) {
            getLog().info("Using Flatfile (JSON) as storage method.");
            datastore = new FlatfileDatastore(this, getDataFolder());
        } else {
            getLog().severe("Storage method '" + storageMethod + "' was not recognised. Using SQLite as fallback.");
            datastore = new SQLiteDatastore(this, new File(getDataFolder(), "luckperms.sqlite"));
        }

        getLog().info("Initialising datastore...");
        datastore.init();

        getLog().info("Loading internal permission managers...");
        uuidCache = new UuidCache(getConfiguration().getOnlineMode());
        userManager = new BukkitUserManager(this);
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
            long ticks = mins * 60 * 20;
            getServer().getScheduler().runTaskTimerAsynchronously(this, new UpdateTask(this), ticks, ticks);
        }

        // Provide vault support
        getLog().info("Attempting to hook into Vault...");
        try {
            if (getServer().getPluginManager().isPluginEnabled("Vault")) {
                VaultHook.hook(this);
                getLog().info("Registered Vault permission & chat hook.");
            } else {
                getLog().info("Vault not found.");
            }
        } catch (Exception e) {
            getLog().severe("Error occurred whilst hooking into Vault.");
            e.printStackTrace();
        }

        getLog().info("Registering API...");
        final ApiProvider provider = new ApiProvider(this);
        LuckPerms.registerProvider(provider);
        getServer().getServicesManager().register(LuckPermsApi.class, provider, this, ServicePriority.Normal);

        getLog().info("Successfully loaded.");
    }

    @Override
    public void onDisable() {
        getLog().info("Closing datastore...");
        datastore.shutdown();

        getLog().info("Unregistering API...");
        LuckPerms.unregisterProvider();
        getServer().getServicesManager().unregisterAll(this);
    }

    @Override
    public void doAsync(Runnable r) {
        Bukkit.getScheduler().runTaskAsynchronously(this, r);
    }

    @Override
    public void doSync(Runnable r) {
        Bukkit.getScheduler().runTask(this, r);
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
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
    public void runUpdateTask() {
        getServer().getScheduler().runTaskAsynchronously(this, new UpdateTask(this));
    }
}
