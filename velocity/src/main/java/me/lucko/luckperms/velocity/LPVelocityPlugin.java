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

package me.lucko.luckperms.velocity;

import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.command.CommandManager;
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
import me.lucko.luckperms.velocity.calculator.VelocityCalculatorFactory;
import me.lucko.luckperms.velocity.context.VelocityContextManager;
import me.lucko.luckperms.velocity.context.VelocityPlayerCalculator;
import me.lucko.luckperms.velocity.listeners.MonitoringPermissionCheckListener;
import me.lucko.luckperms.velocity.listeners.VelocityConnectionListener;
import me.lucko.luckperms.velocity.messaging.VelocityMessagingFactory;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.query.QueryOptions;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * LuckPerms implementation for the Velocity API.
 */
public class LPVelocityPlugin extends AbstractLuckPermsPlugin {
    private final LPVelocityBootstrap bootstrap;

    private VelocitySenderFactory senderFactory;
    private VelocityConnectionListener connectionListener;
    private VelocityCommandExecutor commandManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private VelocityContextManager contextManager;

    public LPVelocityPlugin(LPVelocityBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public LPVelocityBootstrap getBootstrap() {
        return this.bootstrap;
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new VelocitySenderFactory(this);
    }

    @Override
    protected Set<Dependency> getGlobalDependencies() {
        Set<Dependency> dependencies = super.getGlobalDependencies();
        // required for loading the LP config
        dependencies.add(Dependency.CONFIGURATE_CORE);
        dependencies.add(Dependency.CONFIGURATE_YAML);
        dependencies.add(Dependency.SNAKEYAML);
        return dependencies;
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new VelocityConfigAdapter(this, resolveConfig("config.yml"));
    }

    @Override
    protected void registerPlatformListeners() {
        this.connectionListener = new VelocityConnectionListener(this);
        this.bootstrap.getProxy().getEventManager().register(this.bootstrap, this.connectionListener);
        this.bootstrap.getProxy().getEventManager().register(this.bootstrap, new MonitoringPermissionCheckListener(this));
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new VelocityMessagingFactory(this);
    }

    @Override
    protected void registerCommands() {
        this.commandManager = new VelocityCommandExecutor(this);
        this.commandManager.register();
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new VelocityCalculatorFactory(this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new VelocityContextManager(this);

        Set<String> disabledContexts = getConfiguration().get(ConfigKeys.DISABLED_CONTEXTS);
        if (!disabledContexts.contains(DefaultContextKeys.WORLD_KEY)) {
            VelocityPlayerCalculator playerCalculator = new VelocityPlayerCalculator(this);
            this.bootstrap.getProxy().getEventManager().register(this.bootstrap, playerCalculator);
            this.contextManager.registerCalculator(playerCalculator);
        }
    }

    @Override
    protected void setupPlatformHooks() {

    }

    @Override
    protected AbstractEventBus<?> provideEventBus(LuckPermsApiProvider apiProvider) {
        return new VelocityEventBus(this, apiProvider);
    }

    @Override
    protected void registerApiOnPlatform(LuckPerms api) {
        // Velocity doesn't have a services manager
    }

    @Override
    protected void performFinalSetup() {

    }

    @Override
    public Optional<QueryOptions> getQueryOptionsForUser(User user) {
        return this.bootstrap.getPlayer(user.getUniqueId()).map(player -> this.contextManager.getQueryOptions(player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        return Stream.concat(
                Stream.of(getConsoleSender()),
                this.bootstrap.getProxy().getAllPlayers().stream().map(p -> this.senderFactory.wrap(p))
        );
    }

    @Override
    public Sender getConsoleSender() {
        return this.senderFactory.wrap(this.bootstrap.getProxy().getConsoleCommandSource());
    }

    public VelocitySenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    @Override
    public AbstractConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public CommandManager getCommandManager() {
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
    public VelocityContextManager getContextManager() {
        return this.contextManager;
    }

}
