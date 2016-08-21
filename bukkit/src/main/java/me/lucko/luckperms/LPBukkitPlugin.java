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
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.implementation.ApiProvider;
import me.lucko.luckperms.api.vault.VaultHook;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.core.LPConfiguration;
import me.lucko.luckperms.core.UuidCache;
import me.lucko.luckperms.data.Importer;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.runnables.ExpireTemporaryTask;
import me.lucko.luckperms.runnables.UpdateTask;
import me.lucko.luckperms.storage.Datastore;
import me.lucko.luckperms.storage.StorageFactory;
import me.lucko.luckperms.tracks.TrackManager;
import me.lucko.luckperms.users.BukkitUserManager;
import me.lucko.luckperms.users.UserManager;
import me.lucko.luckperms.utils.LogFactory;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class LPBukkitPlugin extends JavaPlugin implements LuckPermsPlugin {
    private final Set<UUID> ignoringLogs = new HashSet<>();
    private LPConfiguration configuration;
    private UserManager userManager;
    private GroupManager groupManager;
    private TrackManager trackManager;
    private Datastore datastore;
    private UuidCache uuidCache;
    private ApiProvider apiProvider;
    private Logger log;
    private Importer importer;

    @Override
    public void onEnable() {
        log = LogFactory.wrap(getLogger());

        getLog().info("Loading configuration...");
        configuration = new BukkitConfig(this);

        // register events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new BukkitListener(this), this);

        // register commands
        getLog().info("Registering commands...");
        BukkitCommand commandManager = new BukkitCommand(this);
        PluginCommand main = getServer().getPluginCommand("luckperms");
        main.setExecutor(commandManager);
        main.setTabCompleter(commandManager);
        main.setAliases(Arrays.asList("perms", "lp", "permissions", "p", "perm"));

        datastore = StorageFactory.getDatastore(this, "h2");

        getLog().info("Loading internal permission managers...");
        uuidCache = new UuidCache(getConfiguration().getOnlineMode());
        userManager = new BukkitUserManager(this);
        groupManager = new GroupManager(this);
        trackManager = new TrackManager();
        importer = new Importer(commandManager);

        int mins = getConfiguration().getSyncTime();
        if (mins > 0) {
            long ticks = mins * 60 * 20;
            getServer().getScheduler().runTaskTimerAsynchronously(this, new UpdateTask(this), ticks, ticks);
        }

        getServer().getScheduler().runTaskTimer(this, BukkitSenderFactory.get(), 1L, 1L);
        getServer().getScheduler().runTaskTimerAsynchronously(this, new ExpireTemporaryTask(this), 60L, 60L);

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
        apiProvider = new ApiProvider(this);
        LuckPerms.registerProvider(apiProvider);
        getServer().getServicesManager().register(LuckPermsApi.class, apiProvider, this, ServicePriority.Normal);

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
        LuckPerms.unregisterProvider();
        getServer().getServicesManager().unregisterAll(this);
    }

    @Override
    public void doAsync(Runnable r) {
        getServer().getScheduler().runTaskAsynchronously(this, r);
    }

    @Override
    public void doSync(Runnable r) {
        getServer().getScheduler().runTask(this, r);
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public Type getType() {
        return Type.BUKKIT;
    }

    @Override
    public File getMainDir() {
        return getDataFolder();
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
    public List<Sender> getSenders() {
        return getServer().getOnlinePlayers().stream().map(p -> BukkitSenderFactory.get().wrap(p)).collect(Collectors.toList());
    }

    @Override
    public Sender getConsoleSender() {
        return BukkitSenderFactory.get().wrap(getServer().getConsoleSender());
    }

    @Override
    public List<String> getPossiblePermissions() {
        final List<String> perms = new ArrayList<>();

        getServer().getPluginManager().getPermissions().forEach(p -> {
            perms.add(p.getName());
            p.getChildren().keySet().forEach(perms::add);
        });

        return perms;
    }

    @Override
    public Object getPlugin(String name) {
        return getServer().getPluginManager().getPlugin(name);
    }

    @Override
    public Object getService(Class clazz) {
        return getServer().getServicesManager().load(clazz);
    }

    @Override
    public boolean isPluginLoaded(String name) {
        return getServer().getPluginManager().isPluginEnabled(name);
    }

    @Override
    public void runUpdateTask() {
        getServer().getScheduler().runTaskAsynchronously(this, new UpdateTask(this));
    }
}
