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

package me.lucko.luckperms.bukkit;

import me.lucko.luckperms.bukkit.brigadier.LuckPermsBrigadier;
import me.lucko.luckperms.bukkit.calculator.BukkitCalculatorFactory;
import me.lucko.luckperms.bukkit.context.BukkitContextManager;
import me.lucko.luckperms.bukkit.context.BukkitPlayerCalculator;
import me.lucko.luckperms.bukkit.inject.permissible.LuckPermsPermissible;
import me.lucko.luckperms.bukkit.inject.permissible.PermissibleInjector;
import me.lucko.luckperms.bukkit.inject.permissible.PermissibleMonitoringInjector;
import me.lucko.luckperms.bukkit.inject.server.InjectorDefaultsMap;
import me.lucko.luckperms.bukkit.inject.server.InjectorPermissionMap;
import me.lucko.luckperms.bukkit.inject.server.InjectorSubscriptionMap;
import me.lucko.luckperms.bukkit.inject.server.LuckPermsDefaultsMap;
import me.lucko.luckperms.bukkit.inject.server.LuckPermsPermissionMap;
import me.lucko.luckperms.bukkit.inject.server.LuckPermsSubscriptionMap;
import me.lucko.luckperms.bukkit.listeners.BukkitAutoOpListener;
import me.lucko.luckperms.bukkit.listeners.BukkitCommandListUpdater;
import me.lucko.luckperms.bukkit.listeners.BukkitConnectionListener;
import me.lucko.luckperms.bukkit.listeners.BukkitPlatformListener;
import me.lucko.luckperms.bukkit.messaging.BukkitMessagingFactory;
import me.lucko.luckperms.bukkit.util.PluginManagerUtil;
import me.lucko.luckperms.bukkit.vault.VaultHookManager;
import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager;
import me.lucko.luckperms.common.model.manager.track.StandardTrackManager;
import me.lucko.luckperms.common.model.manager.user.StandardUserManager;
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.common.sender.Sender;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * LuckPerms implementation for the Bukkit API.
 */
public class LPBukkitPlugin extends AbstractLuckPermsPlugin {
    private final LPBukkitBootstrap bootstrap;

    private BukkitSenderFactory senderFactory;
    private BukkitConnectionListener connectionListener;
    private BukkitCommandExecutor commandManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private BukkitContextManager contextManager;
    private LuckPermsSubscriptionMap subscriptionMap;
    private LuckPermsPermissionMap permissionMap;
    private LuckPermsDefaultsMap defaultPermissionMap;
    private VaultHookManager vaultHookManager = null;
    
    public LPBukkitPlugin(LPBukkitBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public LPBukkitBootstrap getBootstrap() {
        return this.bootstrap;
    }

    public JavaPlugin getLoader() {
        return this.bootstrap.getLoader();
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new BukkitSenderFactory(this);
    }

    @Override
    protected Set<Dependency> getGlobalDependencies() {
        Set<Dependency> dependencies = super.getGlobalDependencies();
        dependencies.add(Dependency.ADVENTURE_PLATFORM);
        dependencies.add(Dependency.ADVENTURE_PLATFORM_BUKKIT);
        if (isBrigadierSupported()) {
            dependencies.add(Dependency.COMMODORE);
            dependencies.add(Dependency.COMMODORE_FILE);
        }
        return dependencies;
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new BukkitConfigAdapter(this, resolveConfig("config.yml").toFile());
    }

    @Override
    protected void registerPlatformListeners() {
        this.connectionListener = new BukkitConnectionListener(this);
        this.bootstrap.getServer().getPluginManager().registerEvents(this.connectionListener, this.bootstrap.getLoader());
        this.bootstrap.getServer().getPluginManager().registerEvents(new BukkitPlatformListener(this), this.bootstrap.getLoader());
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new BukkitMessagingFactory(this);
    }

    @Override
    protected void registerCommands() {
        PluginCommand command = this.bootstrap.getLoader().getCommand("luckperms");
        if (command == null) {
            getLogger().severe("Unable to register /luckperms command with the server");
            return;
        }

        if (isAsyncTabCompleteSupported()) {
            this.commandManager = new BukkitAsyncCommandExecutor(this, command);
        } else {
            this.commandManager = new BukkitCommandExecutor(this, command);
        }

        this.commandManager.register();

        // setup brigadier
        if (isBrigadierSupported() && getConfiguration().get(ConfigKeys.REGISTER_COMMAND_LIST_DATA)) {
            try {
                LuckPermsBrigadier.register(this, command);
            } catch (Exception e) {
                if (!(e instanceof RuntimeException && e.getMessage().contains("not supported by the server"))) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new BukkitCalculatorFactory(this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new BukkitContextManager(this);

        BukkitPlayerCalculator playerCalculator = new BukkitPlayerCalculator(this, getConfiguration().get(ConfigKeys.DISABLED_CONTEXTS));
        this.bootstrap.getServer().getPluginManager().registerEvents(playerCalculator, this.bootstrap.getLoader());
        this.contextManager.registerCalculator(playerCalculator);
    }

    @Override
    protected void setupPlatformHooks() {
        // inject our own custom permission maps
        Runnable[] injectors = new Runnable[]{
                new InjectorSubscriptionMap(this)::inject,
                new InjectorPermissionMap(this)::inject,
                new InjectorDefaultsMap(this)::inject,
                new PermissibleMonitoringInjector(this, PermissibleMonitoringInjector.Mode.INJECT)
        };

        for (Runnable injector : injectors) {
            injector.run();

            // schedule another injection after all plugins have loaded
            // the entire pluginmanager instance is replaced by some plugins :(
            this.bootstrap.getScheduler().sync(injector);
        }

        /*
         * This is an unfortunate solution to a problem which shouldn't even exist. As of Spigot 1.15,
         * the way LP establishes it's load order relative to Vault triggers a dependency warning.
         * This is a workaround to prevent that from showing, since at the moment, there is nothing I
         * can reasonably do to improve this handling in LP without breaking plugins which use/obtain
         * Vault in their onEnable without depending on us.
         *
         * Noteworthy discussion here:
         * - https://github.com/LuckPerms/LuckPerms/issues/1959
         * - https://hub.spigotmc.org/jira/browse/SPIGOT-5546
         * - https://github.com/PaperMC/Paper/pull/3509
         */
        PluginManagerUtil.injectDependency(this.bootstrap.getServer().getPluginManager(), this.bootstrap.getLoader().getName(), "Vault");

        // Provide vault support
        tryVaultHook(false);
    }

    @Override
    protected AbstractEventBus<?> provideEventBus(LuckPermsApiProvider apiProvider) {
        return new BukkitEventBus(this, apiProvider);
    }

    public void tryVaultHook(boolean force) {
        if (this.vaultHookManager != null) {
            return; // already hooked
        }

        try {
            if (force || this.bootstrap.getServer().getPluginManager().isPluginEnabled("Vault")) {
                this.vaultHookManager = new VaultHookManager(this);
                this.vaultHookManager.hook();
                getLogger().info("Registered Vault permission & chat hook.");
            }
        } catch (Exception e) {
            this.vaultHookManager = null;
            getLogger().severe("Error occurred whilst hooking into Vault.", e);
        }
    }

    @Override
    protected void registerApiOnPlatform(LuckPerms api) {
        this.bootstrap.getServer().getServicesManager().register(LuckPerms.class, api, this.bootstrap.getLoader(), ServicePriority.Normal);
    }

    @Override
    protected void performFinalSetup() {
        // register permissions
        PluginManager pluginManager = this.bootstrap.getServer().getPluginManager();
        PermissionDefault permDefault = getConfiguration().get(ConfigKeys.COMMANDS_ALLOW_OP) ? PermissionDefault.OP : PermissionDefault.FALSE;

        for (CommandPermission permission : CommandPermission.values()) {
            Permission bukkitPermission = new Permission(permission.getPermission(), permDefault);
            pluginManager.removePermission(bukkitPermission);
            pluginManager.addPermission(bukkitPermission);
        }

        // remove all operators on startup if they're disabled
        if (!getConfiguration().get(ConfigKeys.OPS_ENABLED)) {
            this.bootstrap.getScheduler().sync(() -> {
                for (OfflinePlayer player : this.bootstrap.getServer().getOperators()) {
                    player.setOp(false);
                }
            });
        }

        // register autoop listener
        if (getConfiguration().get(ConfigKeys.AUTO_OP)) {
            getApiProvider().getEventBus().subscribe(new BukkitAutoOpListener(this));
        }

        // register bukkit command list updater
        if (getConfiguration().get(ConfigKeys.UPDATE_CLIENT_COMMAND_LIST) && BukkitCommandListUpdater.isSupported()) {
            getApiProvider().getEventBus().subscribe(new BukkitCommandListUpdater(this));
        }

        // Load any online users (in the case of a reload)
        for (Player player : this.bootstrap.getServer().getOnlinePlayers()) {
            this.bootstrap.getScheduler().async(() -> {
                try {
                    User user = this.connectionListener.loadUser(player.getUniqueId(), player.getName());
                    if (user != null) {
                        this.bootstrap.getScheduler().sync(player, () -> {
                            try {
                                LuckPermsPermissible lpPermissible = new LuckPermsPermissible(player, user, this);
                                PermissibleInjector.inject(player, lpPermissible, getLogger());
                            } catch (Throwable t) {
                                getLogger().severe("Exception thrown when setting up permissions for " +
                                        player.getUniqueId() + " - " + player.getName(), t);
                            }
                        });
                    }
                } catch (Exception e) {
                    getLogger().severe("Exception occurred whilst loading data for " +
                            player.getUniqueId() + " - " + player.getName(), e);
                }
            });
        }
    }

    @Override
    protected void removePlatformHooks() {
        // uninject from players
        for (Player player : this.bootstrap.getServer().getOnlinePlayers()) {
            try {
                PermissibleInjector.uninject(player, false);
            } catch (Exception e) {
                getLogger().severe("Exception thrown when unloading permissions from " +
                        player.getUniqueId() + " - " + player.getName(), e);
            }

            if (getConfiguration().get(ConfigKeys.AUTO_OP)) {
                player.setOp(false);
            }

            final User user = getUserManager().getIfLoaded(player.getUniqueId());
            if (user != null) {
                user.getCachedData().invalidate();
                getUserManager().unload(user.getUniqueId());
            }
        }

        // uninject custom maps
        new InjectorSubscriptionMap(this).uninject();
        new InjectorPermissionMap(this).uninject();
        new InjectorDefaultsMap(this).uninject();
        new PermissibleMonitoringInjector(this, PermissibleMonitoringInjector.Mode.UNINJECT).run();

        // unhook vault
        if (this.vaultHookManager != null) {
            this.vaultHookManager.unhook();
        }
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean isBrigadierSupported() {
        return classExists("com.mojang.brigadier.CommandDispatcher");
    }

    private static boolean isAsyncTabCompleteSupported() {
        return classExists("com.destroystokyo.paper.event.server.AsyncTabCompleteEvent");
    }

    @Override
    public Optional<QueryOptions> getQueryOptionsForUser(User user) {
        return this.bootstrap.getPlayer(user.getUniqueId()).map(player -> this.contextManager.getQueryOptions(player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        List<Player> players = new ArrayList<>(this.bootstrap.getServer().getOnlinePlayers());
        return Stream.concat(
                Stream.of(getConsoleSender()),
                players.stream().map(p -> getSenderFactory().wrap(p))
        );
    }

    @Override
    public Sender getConsoleSender() {
        return getSenderFactory().wrap(this.bootstrap.getConsole());
    }

    public BukkitSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    @Override
    public AbstractConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public BukkitCommandExecutor getCommandManager() {
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
    public BukkitContextManager getContextManager() {
        return this.contextManager;
    }

    public LuckPermsSubscriptionMap getSubscriptionMap() {
        return this.subscriptionMap;
    }

    public void setSubscriptionMap(LuckPermsSubscriptionMap subscriptionMap) {
        this.subscriptionMap = subscriptionMap;
    }

    public LuckPermsPermissionMap getPermissionMap() {
        return this.permissionMap;
    }

    public void setPermissionMap(LuckPermsPermissionMap permissionMap) {
        this.permissionMap = permissionMap;
    }

    public LuckPermsDefaultsMap getDefaultPermissionMap() {
        return this.defaultPermissionMap;
    }

    public void setDefaultPermissionMap(LuckPermsDefaultsMap defaultPermissionMap) {
        this.defaultPermissionMap = defaultPermissionMap;
    }

}
