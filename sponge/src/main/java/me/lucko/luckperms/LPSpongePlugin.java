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
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.core.LPConfiguration;
import me.lucko.luckperms.core.UuidCache;
import me.lucko.luckperms.data.Importer;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.runnables.UpdateTask;
import me.lucko.luckperms.storage.Datastore;
import me.lucko.luckperms.storage.StorageFactory;
import me.lucko.luckperms.tracks.TrackManager;
import me.lucko.luckperms.users.SpongeUserManager;
import me.lucko.luckperms.users.UserManager;
import me.lucko.luckperms.utils.LogFactory;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.text.Text;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
@Plugin(id = "luckperms", name = "LuckPerms", version = "null", authors = {"Luck"}, description = "A permissions plugin")
public class LPSpongePlugin implements LuckPermsPlugin {

    @Inject
    private Logger logger;

    @Inject
    private Game game;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;

    private Scheduler scheduler = Sponge.getScheduler();

    private final Set<UUID> ignoringLogs = new HashSet<>();
    private LPConfiguration configuration;
    private UserManager userManager;
    private GroupManager groupManager;
    private TrackManager trackManager;
    private Datastore datastore;
    private UuidCache uuidCache;
    private me.lucko.luckperms.api.Logger log;
    private Importer importer;

    @Listener
    public void onEnable(GamePreInitializationEvent event) {
        log = LogFactory.wrap(logger);

        getLog().info("Loading configuration...");
        configuration = new SpongeConfig(this);

        // register events
        Sponge.getEventManager().registerListeners(this, new SpongeListener(this));

        // register commands
        getLog().info("Registering commands...");
        CommandManager cmdService = Sponge.getCommandManager();
        SpongeCommand commandManager = new SpongeCommand(this);
        cmdService.register(this, commandManager, "luckperms", "perms", "lp", "permissions", "p", "perm");

        datastore = StorageFactory.getDatastore(this, "h2");

        getLog().info("Loading internal permission managers...");
        uuidCache = new UuidCache(getConfiguration().getOnlineMode());
        userManager = new SpongeUserManager(this);
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
            scheduler.createTaskBuilder().async().interval(mins, TimeUnit.MINUTES).execute(new UpdateTask(this))
                    .submit(LPSpongePlugin.this);
        }

        scheduler.createTaskBuilder().intervalTicks(1L).execute(SpongeSenderFactory.get()).submit(this);

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

    @Listener
    public void onPostInit(GamePostInitializationEvent event) {
        // register permissions
        Optional<PermissionService> ps = game.getServiceManager().provide(PermissionService.class);
        if (!ps.isPresent()) {
            getLog().warn("Unable to register all LuckPerms permissions. PermissionService not available.");
            return;
        }

        final PermissionService p = ps.get();

        Optional<PermissionDescription.Builder> builder = p.newDescriptionBuilder(this);
        if (!builder.isPresent()) {
            getLog().warn("Unable to register all LuckPerms permissions. Description Builder not available.");
            return;
        }

        for (Permission perm : Permission.values()) {
            registerPermission(p, perm.getNode());
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public File getMainDir() {
        File base = configDir.toFile().getParentFile().getParentFile();
        File luckPermsDir = new File(base, "luckperms");
        luckPermsDir.mkdirs();
        return luckPermsDir;
    }

    @Override
    public File getDataFolder() {
        return getMainDir();
    }

    @Override
    public String getVersion() {
        return "null";
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
    public List<Sender> getSenders() {
        return game.getServer().getOnlinePlayers().stream().map(s -> SpongeSenderFactory.get().wrap(s)).collect(Collectors.toList());
    }

    @Override
    public Sender getConsoleSender() {
        return SpongeSenderFactory.get().wrap(game.getServer().getConsole());
    }

    @Override
    public List<String> getPossiblePermissions() {
        Optional<PermissionService> p = game.getServiceManager().provide(PermissionService.class);
        if (!p.isPresent()) {
            return Collections.emptyList();
        }
        return p.get().getDescriptions().stream().map(PermissionDescription::getId).collect(Collectors.toList());
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

    private void registerPermission(PermissionService p, String node) {
        Optional<PermissionDescription.Builder> builder = p.newDescriptionBuilder(this);
        if (!builder.isPresent()) return;

        try {
            builder.get().assign(PermissionDescription.ROLE_ADMIN, true).description(Text.of(node)).id(node).register();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
}
