package me.lucko.luckperms;

import lombok.Getter;
import me.lucko.luckperms.commands.CommandManager;
import me.lucko.luckperms.data.Datastore;
import me.lucko.luckperms.data.MySQLConfiguration;
import me.lucko.luckperms.data.methods.FlatfileDatastore;
import me.lucko.luckperms.data.methods.MySQLDatastore;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.listeners.PlayerListener;
import me.lucko.luckperms.runnables.UpdateTask;
import me.lucko.luckperms.tracks.TrackManager;
import me.lucko.luckperms.users.BungeeUserManager;
import me.lucko.luckperms.users.UserManager;
import me.lucko.luckperms.utils.LPConfiguration;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class LPBungeePlugin extends Plugin implements LuckPermsPlugin {
    public static final String VERSION = "v1.2";

    private LPConfiguration configuration;
    private UserManager userManager;
    private GroupManager groupManager;
    private TrackManager trackManager;
    private Datastore datastore;

    @Override
    public void onEnable() {
        configuration = new BungeeConfig(this);

        // register events
        getProxy().getPluginManager().registerListener(this, new PlayerListener(this));

        // register commands
        getProxy().getPluginManager().registerCommand(this, new MainCommand(new CommandManager(this)));

        // disable the default Bungee /perms command so it gets handled by the Bukkit plugin
        getProxy().getDisabledCommands().add("perms");

        final String storageMethod = configuration.getStorageMethod();

        if (storageMethod.equalsIgnoreCase("mysql")) {
            getLogger().info("Using MySQL as storage method.");
            datastore = new MySQLDatastore(this, new MySQLConfiguration(
                    configuration.getDatabaseValue("address"),
                    configuration.getDatabaseValue("database"),
                    configuration.getDatabaseValue("username"),
                    configuration.getDatabaseValue("password")
            ));
        } else if (storageMethod.equalsIgnoreCase("flatfile")) {
            getLogger().info("Using Flatfile (JSON) as storage method.");
            datastore = new FlatfileDatastore(this, getDataFolder());
        } else {
            getLogger().warning("Storage method '" + storageMethod + "' was not recognised. Using Flatfile as fallback.");
            datastore = new FlatfileDatastore(this, getDataFolder());
        }

        datastore.init();

        userManager = new BungeeUserManager(this);
        groupManager = new GroupManager(this);
        trackManager = new TrackManager(this);

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
        return getProxy().getPlayer(uuid) != null ? "&aOnline" : "&cOffline";
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
