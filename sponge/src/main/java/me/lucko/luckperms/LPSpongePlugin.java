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

import com.google.inject.Inject;
import lombok.Getter;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.implementation.ApiProvider;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.data.Datastore;
import me.lucko.luckperms.data.methods.FlatfileDatastore;
import me.lucko.luckperms.data.methods.MySQLDatastore;
import me.lucko.luckperms.data.methods.SQLiteDatastore;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.runnables.UpdateTask;
import me.lucko.luckperms.tracks.TrackManager;
import me.lucko.luckperms.users.SpongeUserManager;
import me.lucko.luckperms.users.UserManager;
import me.lucko.luckperms.utils.LPConfiguration;
import me.lucko.luckperms.utils.LogUtil;
import me.lucko.luckperms.utils.UuidCache;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Scheduler;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
@Plugin(id = "luckperms", name = "LuckPerms", version = "MAGIC", authors = {"Luck"}, description = "A permissions plugin")
public class LPSpongePlugin implements LuckPermsPlugin {

    @Inject
    private Logger logger;

    @Inject
    private Game game;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;

    private Scheduler scheduler = Sponge.getScheduler();

    private LPConfiguration configuration;
    private UserManager userManager;
    private GroupManager groupManager;
    private TrackManager trackManager;
    private Datastore datastore;
    private UuidCache uuidCache;
    private me.lucko.luckperms.api.Logger log;

    @Listener
    public void onEnable(GamePreInitializationEvent event) {
        log = LogUtil.wrap(logger);

        getLog().info("Loading configuration...");
        configuration = new SpongeConfig(this);

        // register events
        Sponge.getEventManager().registerListeners(this, new SpongeListener(this));

        // register commands
        getLog().info("Registering commands...");
        CommandManager cmdService = Sponge.getCommandManager();
        cmdService.register(this, new SpongeCommand(this), "luckperms", "perms", "lp", "permissions", "p", "perm");

        getLog().info("Detecting storage method...");
        final String storageMethod = configuration.getStorageMethod();
        if (storageMethod.equalsIgnoreCase("mysql")) {
            getLog().info("Using MySQL as storage method.");
            datastore = new MySQLDatastore(this, configuration.getDatabaseValues());
        } else if (storageMethod.equalsIgnoreCase("sqlite")) {
            getLog().info("Using SQLite as storage method.");
            datastore = new SQLiteDatastore(this, new File(getStorageDir(), "luckperms.sqlite"));
        } else if (storageMethod.equalsIgnoreCase("flatfile")) {
            getLog().info("Using Flatfile (JSON) as storage method.");
            datastore = new FlatfileDatastore(this, getStorageDir());
        } else {
            getLog().severe("Storage method '" + storageMethod + "' was not recognised. Using SQLite as fallback.");
            datastore = new SQLiteDatastore(this, new File(getStorageDir(), "luckperms.sqlite"));
        }

        getLog().info("Initialising datastore...");
        datastore.init();

        getLog().info("Loading internal permission managers...");
        uuidCache = new UuidCache(getConfiguration().getOnlineMode());
        userManager = new SpongeUserManager(this);
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
            scheduler.createTaskBuilder().async().interval(mins, TimeUnit.MINUTES).execute(new UpdateTask(this))
                    .submit(LPSpongePlugin.this);
        }

        getLog().info("Registering API...");
        final ApiProvider provider = new ApiProvider(this);
        LuckPerms.registerProvider(provider);
        Sponge.getServiceManager().setProvider(this, LuckPermsApi.class, provider);

        getLog().info("Successfully loaded.");
    }

    @Listener
    public void onDisable(GameStoppingServerEvent event) {
        getLog().info("Closing datastore...");
        datastore.shutdown();

        getLog().info("Unregistering API...");
        LuckPerms.unregisterProvider();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private File getStorageDir() {
        File base = configDir.toFile().getParentFile().getParentFile();
        File luckPermsDir = new File(base, "luckperms");
        luckPermsDir.mkdirs();
        return luckPermsDir;
    }

    @Override
    public String getVersion() {
        return "MAGIC";
    }

    @Override
    public Message getPlayerStatus(UUID uuid) {
        return game.getServer().getPlayer(getUuidCache().getExternalUUID(uuid)).isPresent() ? Message.PLAYER_ONLINE : Message.PLAYER_OFFLINE;
    }

    @Override
    public int getPlayerCount() {
        return game.getServer().getOnlinePlayers().size();
    }

    @Override
    public List<String> getPlayerList() {
        return game.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    @Override
    public void runUpdateTask() {
        scheduler.createTaskBuilder().async().execute(new UpdateTask(this)).submit(LPSpongePlugin.this);
    }

    @Override
    public void doAsync(Runnable r) {
        scheduler.createTaskBuilder().async().execute(r).submit(LPSpongePlugin.this);
    }

    @Override
    public void doSync(Runnable r) {
        scheduler.createTaskBuilder().execute(r).submit(LPSpongePlugin.this);
    }
}
