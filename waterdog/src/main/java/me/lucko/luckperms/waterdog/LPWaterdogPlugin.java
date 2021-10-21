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

package me.lucko.luckperms.waterdog;

import dev.waterdog.waterdogpe.plugin.Plugin;
import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager;
import me.lucko.luckperms.common.model.manager.track.StandardTrackManager;
import me.lucko.luckperms.common.model.manager.user.StandardUserManager;
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.waterdog.calculator.WaterdogCalculatorFactory;
import me.lucko.luckperms.waterdog.context.WaterdogContextManager;
import me.lucko.luckperms.waterdog.context.WaterdogPlayerCalculator;
import me.lucko.luckperms.waterdog.listeners.WaterdogConnectionListener;
import me.lucko.luckperms.waterdog.listeners.WaterdogPermissionCheckListener;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.query.QueryOptions;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * LuckPerms implementation for the Waterdog API.
 */
public class LPWaterdogPlugin extends AbstractLuckPermsPlugin {
    private final LPWaterdogBootstrap bootstrap;

    private WaterdogSenderFactory senderFactory;
    private WaterdogConnectionListener connectionListener;
    private CommandManager commandManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private WaterdogContextManager contextManager;

    public LPWaterdogPlugin(LPWaterdogBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public LPWaterdogBootstrap getBootstrap() {
        return this.bootstrap;
    }

    public Plugin getLoader() {
        return this.bootstrap.getLoader();
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new WaterdogSenderFactory(this);
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new WaterdogConfigAdapter(this, resolveConfig("config.yml").toFile());
    }

    @Override
    protected void registerPlatformListeners() {
        new WaterdogPermissionCheckListener(this).register(this.bootstrap);
        this.connectionListener = new WaterdogConnectionListener(this);
        this.connectionListener.register(this.bootstrap.getProxy().getEventManager());
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new MessagingFactory<>(this);
    }

    @Override
    protected void registerCommands() {
        this.commandManager = new CommandManager(this);
        WaterdogCommandExecutor command = new WaterdogCommandExecutor(this, this.commandManager);
        command.register();
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new WaterdogCalculatorFactory(this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new WaterdogContextManager(this);

        Set<String> disabledContexts = getConfiguration().get(ConfigKeys.DISABLED_CONTEXTS);
        if (!disabledContexts.contains(DefaultContextKeys.WORLD_KEY)) {
            WaterdogPlayerCalculator playerCalculator = new WaterdogPlayerCalculator(this);
            playerCalculator.register(this.bootstrap.getProxy().getEventManager());
            this.contextManager.registerCalculator(playerCalculator);
        }
    }

    @Override
    protected void setupPlatformHooks() {

    }

    @Override
    protected AbstractEventBus<?> provideEventBus(LuckPermsApiProvider apiProvider) {
        return new WaterdogEventBus(this, apiProvider);
    }

    @Override
    protected void registerApiOnPlatform(LuckPerms api) {
        // Waterdog doesn't have a services manager
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
                this.bootstrap.getProxy().getPlayers().values().stream().map(p -> this.senderFactory.wrap(p))
        );
    }

    @Override
    public Sender getConsoleSender() {
        return this.senderFactory.wrap(this.bootstrap.getProxy().getConsoleSender());
    }

    public WaterdogSenderFactory getSenderFactory() {
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
    public WaterdogContextManager getContextManager() {
        return this.contextManager;
    }

}
