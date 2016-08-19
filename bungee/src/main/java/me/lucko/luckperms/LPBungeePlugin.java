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
import me.lucko.luckperms.api.implementation.ApiProvider;
import me.lucko.luckperms.commands.CommandManager;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.core.LPConfiguration;
import me.lucko.luckperms.core.UuidCache;
import me.lucko.luckperms.data.Importer;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.runnables.UpdateTask;
import me.lucko.luckperms.storage.Datastore;
import me.lucko.luckperms.storage.StorageFactory;
import me.lucko.luckperms.tracks.TrackManager;
import me.lucko.luckperms.users.BungeeUserManager;
import me.lucko.luckperms.users.UserManager;
import me.lucko.luckperms.utils.LogFactory;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class LPBungeePlugin extends Plugin implements LuckPermsPlugin {
    private final Set<UUID> ignoringLogs = new HashSet<>();
    private LPConfiguration configuration;
    private UserManager userManager;
    private GroupManager groupManager;
    private TrackManager trackManager;
    private Datastore datastore;
    private UuidCache uuidCache;
    private Logger log;
    private Importer importer;

    @Override
    public void onEnable() {
        log = LogFactory.wrap(getLogger());

        getLog().info("Loading configuration...");
        configuration = new BungeeConfig(this);

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
        uuidCache = new UuidCache(getConfiguration().getOnlineMode());
        userManager = new BungeeUserManager(this);
        groupManager = new GroupManager(this);
        trackManager = new TrackManager();
        importer = new Importer(commandManager);

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

        // 20 times per second (once per "tick")
        getProxy().getScheduler().schedule(this, BungeeSenderFactory.get(), 50L, 50L, TimeUnit.MILLISECONDS);

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
    public String getVersion() {
        return getDescription().getVersion();
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
    public List<Sender> getSenders() {
        return getProxy().getPlayers().stream().map(p -> BungeeSenderFactory.get().wrap(p)).collect(Collectors.toList());
    }

    @Override
    public Sender getConsoleSender() {
        return BungeeSenderFactory.get().wrap(getProxy().getConsole());
    }

    @Override
    public List<String> getPossiblePermissions() {
        // No such thing on Bungee. Wildcards are processed in the listener instead.
        return Collections.emptyList();
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
