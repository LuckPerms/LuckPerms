/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
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

package me.lucko.luckperms.nukkit;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.calculators.PlatformCalculatorFactory;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.contexts.ContextManager;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.listener.ConnectionListener;
import me.lucko.luckperms.common.managers.group.StandardGroupManager;
import me.lucko.luckperms.common.managers.track.StandardTrackManager;
import me.lucko.luckperms.common.managers.user.StandardUserManager;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.tasks.CacheHousekeepingTask;
import me.lucko.luckperms.common.tasks.ExpireTemporaryTask;
import me.lucko.luckperms.nukkit.calculators.NukkitCalculatorFactory;
import me.lucko.luckperms.nukkit.contexts.NukkitContextManager;
import me.lucko.luckperms.nukkit.contexts.WorldCalculator;
import me.lucko.luckperms.nukkit.listeners.NukkitConnectionListener;
import me.lucko.luckperms.nukkit.listeners.NukkitPlatformListener;
import me.lucko.luckperms.nukkit.model.PermissionDefault;
import me.lucko.luckperms.nukkit.model.permissible.LPPermissible;
import me.lucko.luckperms.nukkit.model.permissible.PermissibleInjector;
import me.lucko.luckperms.nukkit.model.permissible.PermissibleMonitoringInjector;
import me.lucko.luckperms.nukkit.model.server.InjectorDefaultsMap;
import me.lucko.luckperms.nukkit.model.server.InjectorPermissionMap;
import me.lucko.luckperms.nukkit.model.server.InjectorSubscriptionMap;
import me.lucko.luckperms.nukkit.model.server.LPDefaultsMap;
import me.lucko.luckperms.nukkit.model.server.LPPermissionMap;
import me.lucko.luckperms.nukkit.model.server.LPSubscriptionMap;

import cn.nukkit.Player;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.permission.Permission;
import cn.nukkit.plugin.PluginManager;
import cn.nukkit.plugin.service.ServicePriority;
import cn.nukkit.utils.Config;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * LuckPerms implementation for the Nukkit API.
 */
public class LPNukkitPlugin extends AbstractLuckPermsPlugin {
    private final LPNukkitBootstrap bootstrap;

    private NukkitSenderFactory senderFactory;
    private NukkitConnectionListener connectionListener;
    private NukkitCommandExecutor commandManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private ContextManager<Player> contextManager;
    private LPSubscriptionMap subscriptionMap;
    private LPPermissionMap permissionMap;
    private LPDefaultsMap defaultPermissionMap;

    public LPNukkitPlugin(LPNukkitBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public LPNukkitBootstrap getBootstrap() {
        return this.bootstrap;
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new NukkitSenderFactory(this);
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new NukkitConfigAdapter(this, resolveConfig());
    }

    @Override
    protected void registerPlatformListeners() {
        this.connectionListener = new NukkitConnectionListener(this);
        this.bootstrap.getServer().getPluginManager().registerEvents(this.connectionListener, this.bootstrap);
        this.bootstrap.getServer().getPluginManager().registerEvents(new NukkitPlatformListener(this), this.bootstrap);
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new MessagingFactory<>(this);
    }

    @Override
    protected void registerCommands() {
        this.commandManager = new NukkitCommandExecutor(this);
        PluginCommand main = (PluginCommand) this.bootstrap.getServer().getPluginCommand("luckperms");
        main.setExecutor(this.commandManager);
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected PlatformCalculatorFactory provideCalculatorFactory() {
        return new NukkitCalculatorFactory(this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new NukkitContextManager(this);
        this.contextManager.registerCalculator(new WorldCalculator(this));
    }

    @Override
    protected void setupPlatformHooks() {
        // inject our own custom permission maps
        Runnable[] injectors = new Runnable[]{
                new InjectorSubscriptionMap(this),
                new InjectorPermissionMap(this),
                new InjectorDefaultsMap(this),
                new PermissibleMonitoringInjector(this)
        };

        for (Runnable injector : injectors) {
            injector.run();

            // schedule another injection after all plugins have loaded
            // the entire pluginmanager instance is replaced by some plugins :(
            this.bootstrap.getScheduler().asyncLater(injector, 1L);
        }
    }

    @Override
    protected AbstractEventBus provideEventBus(LuckPermsApiProvider apiProvider) {
        return new NukkitEventBus(this, apiProvider);
    }

    @Override
    protected void registerApiOnPlatform(LuckPermsApi api) {
        this.bootstrap.getServer().getServiceManager().register(LuckPermsApi.class, api, this.bootstrap, ServicePriority.NORMAL);
    }

    @Override
    protected void registerHousekeepingTasks() {
        this.bootstrap.getScheduler().asyncRepeating(new ExpireTemporaryTask(this), 60L);
        this.bootstrap.getScheduler().asyncRepeating(new CacheHousekeepingTask(this), 2400L);
    }

    @Override
    protected void performFinalSetup() {
        // register permissions
        try {
            PluginManager pm = this.bootstrap.getServer().getPluginManager();
            PermissionDefault permDefault = getConfiguration().get(ConfigKeys.COMMANDS_ALLOW_OP) ? PermissionDefault.OP : PermissionDefault.FALSE;

            for (CommandPermission p : CommandPermission.values()) {
                pm.addPermission(new Permission(p.getPermission(), null, permDefault.toString()));
            }
        } catch (Exception e) {
            // this throws an exception if the plugin is /reloaded, grr
        }

        if (!getConfiguration().get(ConfigKeys.OPS_ENABLED)) {
            Config ops = this.bootstrap.getServer().getOps();
            ops.getKeys(false).forEach(ops::remove);
        }

        // replace the temporary executor when the Nukkit one starts
        this.bootstrap.getServer().getScheduler().scheduleTask(this.bootstrap, () -> this.bootstrap.getScheduler().setUseFallback(false), true);

        // Load any online users (in the case of a reload)
        for (Player player : this.bootstrap.getServer().getOnlinePlayers().values()) {
            this.bootstrap.getScheduler().doAsync(() -> {
                try {
                    User user = this.connectionListener.loadUser(player.getUniqueId(), player.getName());
                    if (user != null) {
                        this.bootstrap.getScheduler().doSync(() -> {
                            try {
                                LPPermissible lpPermissible = new LPPermissible(player, user, this);
                                PermissibleInjector.inject(player, lpPermissible);
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    protected void performEarlyDisableTasks() {
        // Switch back to the fallback executor, the nukkit one won't allow new tasks
        this.bootstrap.getScheduler().setUseFallback(true);
    }

    @Override
    protected void removePlatformHooks() {
        // uninject from players
        for (Player player : this.bootstrap.getServer().getOnlinePlayers().values()) {
            try {
                PermissibleInjector.unInject(player, false);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (getConfiguration().get(ConfigKeys.AUTO_OP)) {
                player.setOp(false);
            }

            final User user = getUserManager().getIfLoaded(player.getUniqueId());
            if (user != null) {
                user.getCachedData().invalidateCaches();
                getUserManager().unload(user);
            }
        }

        // uninject custom maps
        InjectorSubscriptionMap.uninject();
        InjectorPermissionMap.uninject();
        InjectorDefaultsMap.uninject();
    }

    public void refreshAutoOp(User user, Player player) {
        if (user == null) {
            return;
        }

        if (getConfiguration().get(ConfigKeys.AUTO_OP)) {
            Map<String, Boolean> backing = user.getCachedData().getPermissionData(this.contextManager.getApplicableContexts(player)).getImmutableBacking();
            boolean op = Optional.ofNullable(backing.get("luckperms.autoop")).orElse(false);
            player.setOp(op);
        }
    }

    private File resolveConfig() {
        File configFile = new File(this.bootstrap.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            this.bootstrap.getDataFolder().mkdirs();
            this.bootstrap.saveResource("config.yml", false);
        }
        return configFile;
    }

    @Override
    public Optional<Contexts> getContextForUser(User user) {
        Player player = this.bootstrap.getPlayer(user.getUuid());
        if (player == null) {
            return Optional.empty();
        }
        return Optional.of(this.contextManager.getApplicableContexts(player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        return Stream.concat(
                Stream.of(getConsoleSender()),
                this.bootstrap.getServer().getOnlinePlayers().values().stream().map(p -> getSenderFactory().wrap(p))
        );
    }

    @Override
    public Sender getConsoleSender() {
        return getSenderFactory().wrap(this.bootstrap.getServer().getConsoleSender());
    }

    public NukkitSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    @Override
    public ConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public NukkitCommandExecutor getCommandManager() {
        return this.commandManager;
    }

    @Override
    public StandardUserManager getUserManager() {
        return this.userManager;
    }

    @Override
    public StandardGroupManager getGroupManager() {
        return this.groupManager;
    }

    @Override
    public StandardTrackManager getTrackManager() {
        return this.trackManager;
    }

    @Override
    public ContextManager<Player> getContextManager() {
        return this.contextManager;
    }

    public LPSubscriptionMap getSubscriptionMap() {
        return this.subscriptionMap;
    }

    public void setSubscriptionMap(LPSubscriptionMap subscriptionMap) {
        this.subscriptionMap = subscriptionMap;
    }

    public LPPermissionMap getPermissionMap() {
        return this.permissionMap;
    }

    public void setPermissionMap(LPPermissionMap permissionMap) {
        this.permissionMap = permissionMap;
    }

    public LPDefaultsMap getDefaultPermissionMap() {
        return this.defaultPermissionMap;
    }

    public void setDefaultPermissionMap(LPDefaultsMap defaultPermissionMap) {
        this.defaultPermissionMap = defaultPermissionMap;
    }

}
