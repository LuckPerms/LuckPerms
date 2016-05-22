package me.lucko.luckperms;

import lombok.Getter;
import me.lucko.luckperms.commands.CommandManager;
import me.lucko.luckperms.data.Datastore;
import me.lucko.luckperms.data.DatastoreConfiguration;
import me.lucko.luckperms.data.HikariDatastore;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.listeners.PlayerListener;
import me.lucko.luckperms.runnables.UpdateTask;
import me.lucko.luckperms.users.BungeeUserManager;
import me.lucko.luckperms.users.UserManager;
import me.lucko.luckperms.utils.LPConfiguration;
import net.md_5.bungee.api.plugin.Plugin;

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

        datastore = new HikariDatastore(this);
        datastore.init(new DatastoreConfiguration(
                configuration.getDatabaseValue("address"),
                configuration.getDatabaseValue("database"),
                configuration.getDatabaseValue("username"),
                configuration.getDatabaseValue("password")
        ));

        userManager = new BungeeUserManager(this);
        groupManager = new GroupManager(this);

        int mins = getConfiguration().getSyncTime();
        if (mins > 0) {
            getProxy().getScheduler().schedule(this, new UpdateTask(this), mins, mins, TimeUnit.MINUTES);
        }
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
        new UpdateTask(this).run();
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
