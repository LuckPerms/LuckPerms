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
import me.lucko.luckperms.users.BukkitUserManager;
import me.lucko.luckperms.users.UserManager;
import me.lucko.luckperms.utils.LPConfiguration;
import me.lucko.luckperms.vaulthooks.VaultChatHook;
import me.lucko.luckperms.vaulthooks.VaultPermissionHook;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

@Getter
public class LPBukkitPlugin extends JavaPlugin implements LuckPermsPlugin {
    public static final String VERSION = "v1.0";

    private LPConfiguration configuration;
    private UserManager userManager;
    private GroupManager groupManager;
    private Datastore datastore;

    @Override
    public void onEnable() {
        configuration = new BukkitConfig(this);

        // register events
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);

        // register commands
        CommandManagerBukkit commandManager = new CommandManagerBukkit(this);
        PluginCommand main = getServer().getPluginCommand("luckperms");
        main.setExecutor(commandManager);
        main.setAliases(Arrays.asList("perms", "lp", "permissions", "p", "perm"));

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
            getLogger().warning("Storage method '" + storageMethod + "' was not recognised. Using SQLite as fallback.");
            datastore = new SQLiteDatastore(this, new File(getDataFolder(), "luckperms.sqlite"));
        }

        datastore.init();

        userManager = new BukkitUserManager(this);
        groupManager = new GroupManager(this);

        // Run update task to refresh any online users
        runUpdateTask();

        int mins = getConfiguration().getSyncTime();
        if (mins > 0) {
            long ticks = mins * 60 * 20;
            getServer().getScheduler().runTaskTimerAsynchronously(this, new UpdateTask(this), ticks, ticks);
        }

        // Provide vault support
        try {
            if (getServer().getPluginManager().isPluginEnabled("Vault")) {
                final VaultPermissionHook permsHook = new VaultPermissionHook(this);
                getServer().getServicesManager().register(Permission.class, permsHook, this, ServicePriority.High);
                getServer().getServicesManager().register(Chat.class, new VaultChatHook(permsHook), this, ServicePriority.Lowest);
                getLogger().info("Registered Vault permission & chat hook.");
            } else {
                getLogger().info("Vault not found.");
            }
        } catch (Exception e) {
            getLogger().warning("Error whilst hooking into Vault.");
            e.printStackTrace();
        }

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
    public void runUpdateTask() {
        getServer().getScheduler().runTaskAsynchronously(this, new UpdateTask(this));
    }
}
