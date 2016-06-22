package me.lucko.luckperms;

import lombok.Getter;
import me.lucko.luckperms.commands.CommandManager;
import me.lucko.luckperms.data.Datastore;
import me.lucko.luckperms.data.MySQLConfiguration;
import me.lucko.luckperms.data.methods.MySQLDatastore;
import me.lucko.luckperms.data.methods.SQLiteDatastore;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.listeners.PlayerListener;
import me.lucko.luckperms.runnables.UpdateTask;
import me.lucko.luckperms.users.BungeeUserManager;
import me.lucko.luckperms.users.UserManager;
import me.lucko.luckperms.utils.LPConfiguration;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
public class LPBungeePlugin extends Plugin implements LuckPermsPlugin {
    public static final String VERSION = "v1.0";

    private LPConfiguration configuration;
    private UserManager userManager;
    private GroupManager groupManager;
    private Datastore datastore;

    @Override
    public void onEnable() {
        configuration = new BungeeConfig(this);

        // register events
        getProxy().getPluginManager().registerListener(this, new PlayerListener(this));

        // register commands
        getProxy().getPluginManager().registerCommand(this, new MainCommand(new CommandManager(this)));

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
        } else {
            getLogger().warning("Storage method '" + storageMethod + "' was not recognised. Using SQLite as fallback.");
            datastore = new SQLiteDatastore(this, new File(getDataFolder(), "luckperms.sqlite"));
        }

        datastore.init();

        userManager = new BungeeUserManager(this);
        groupManager = new GroupManager(this);

        int mins = getConfiguration().getSyncTime();
        if (mins > 0) {
            getProxy().getScheduler().schedule(this, new UpdateTask(this), mins, mins, TimeUnit.MINUTES);
        }
    }

    @Override
    public void onDisable() {
        datastore.shutdown();
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getPlayerStatus(UUID uuid) {
        if (getProxy().getPlayer(uuid) != null) return "&aOnline";
        return "&cOffline";
    }

    @Override
    public int getPlayerCount() {
        return getProxy().getOnlineCount();
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
