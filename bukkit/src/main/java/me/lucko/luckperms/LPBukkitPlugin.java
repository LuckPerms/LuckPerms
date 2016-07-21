package me.lucko.luckperms;

import lombok.Getter;
import me.lucko.luckperms.data.Datastore;
import me.lucko.luckperms.data.MySQLConfiguration;
import me.lucko.luckperms.data.methods.FlatfileDatastore;
import me.lucko.luckperms.data.methods.MySQLDatastore;
import me.lucko.luckperms.data.methods.SQLiteDatastore;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.listeners.PlayerListener;
import me.lucko.luckperms.runnables.UpdateTask;
import me.lucko.luckperms.tracks.TrackManager;
import me.lucko.luckperms.users.BukkitUserManager;
import me.lucko.luckperms.users.UserManager;
import me.lucko.luckperms.utils.LPConfiguration;
import me.lucko.luckperms.vaulthooks.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class LPBukkitPlugin extends JavaPlugin implements LuckPermsPlugin {
    public static final String VERSION = "v1.2";

    private LPConfiguration configuration;
    private UserManager userManager;
    private GroupManager groupManager;
    private TrackManager trackManager;
    private Datastore datastore;

    @Override
    public void onEnable() {
        getLogger().info("Loading configuration...");
        configuration = new BukkitConfig(this);

        // register events
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);

        // register commands
        getLogger().info("Registering commands...");
        CommandManagerBukkit commandManager = new CommandManagerBukkit(this);
        PluginCommand main = getServer().getPluginCommand("luckperms");
        main.setExecutor(commandManager);
        main.setTabCompleter(commandManager);
        main.setAliases(Arrays.asList("perms", "lp", "permissions", "p", "perm"));

        getLogger().info("Detecting storage method...");
        final String storageMethod = configuration.getStorageMethod();
        if (storageMethod.equalsIgnoreCase("mysql")) {
            getLogger().info("Using MySQL as storage method.");
            datastore = new MySQLDatastore(this, new MySQLConfiguration(
                    configuration.getDatabaseValue("address"),
                    configuration.getDatabaseValue("database"),
                    configuration.getDatabaseValue("username"),
                    configuration.getDatabaseValue("password")
            ));
        } else if (storageMethod.equalsIgnoreCase("sqlite")) {
            getLogger().info("Using SQLite as storage method.");
            datastore = new SQLiteDatastore(this, new File(getDataFolder(), "luckperms.sqlite"));
        } else if (storageMethod.equalsIgnoreCase("flatfile")) {
            getLogger().info("Using Flatfile (JSON) as storage method.");
            datastore = new FlatfileDatastore(this, getDataFolder());
        } else {
            getLogger().severe("Storage method '" + storageMethod + "' was not recognised. Using SQLite as fallback.");
            datastore = new SQLiteDatastore(this, new File(getDataFolder(), "luckperms.sqlite"));
        }

        getLogger().info("Initialising datastore...");
        datastore.init();

        getLogger().info("Loading internal permission managers...");
        userManager = new BukkitUserManager(this);
        groupManager = new GroupManager(this);
        trackManager = new TrackManager();

        // Run update task to refresh any online users
        getLogger().info("Scheduling Update Task to refresh any online users.");
        runUpdateTask();

        int mins = getConfiguration().getSyncTime();
        if (mins > 0) {
            long ticks = mins * 60 * 20;
            getServer().getScheduler().runTaskTimerAsynchronously(this, new UpdateTask(this), ticks, ticks);
        }

        // Provide vault support
        getLogger().info("Attempting to hook into Vault...");
        try {
            if (getServer().getPluginManager().isPluginEnabled("Vault")) {
                VaultHook.hook(this);
                getLogger().info("Registered Vault permission & chat hook.");
            } else {
                getLogger().info("Vault not found.");
            }
        } catch (Exception e) {
            getLogger().severe("Error occurred whilst hooking into Vault.");
            e.printStackTrace();
        }

        getLogger().info("Successfully loaded.");
    }

    @Override
    public void onDisable() {
        datastore.shutdown();
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
        return VERSION;
    }

    @Override
    public String getPlayerStatus(UUID uuid) {
        return getServer().getPlayer(uuid) != null ? "&aOnline" : "&cOffline";
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
