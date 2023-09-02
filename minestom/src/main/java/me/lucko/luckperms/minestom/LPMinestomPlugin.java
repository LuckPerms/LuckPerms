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

package me.lucko.luckperms.minestom;

import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.context.manager.ContextManager;
import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager;
import me.lucko.luckperms.common.model.manager.track.StandardTrackManager;
import me.lucko.luckperms.common.model.manager.track.TrackManager;
import me.lucko.luckperms.common.model.manager.user.StandardUserManager;
import me.lucko.luckperms.common.model.manager.user.UserManager;
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.tasks.CacheHousekeepingTask;
import me.lucko.luckperms.common.tasks.ExpireTemporaryTask;
import me.lucko.luckperms.minestom.calculator.MinestomCalculatorFactory;
import me.lucko.luckperms.minestom.context.MinestomContextManager;
import me.lucko.luckperms.minestom.context.MinestomPlayerCalculator;
import me.lucko.luckperms.minestom.listener.MinestomConnectionListener;
import me.lucko.luckperms.minestom.listener.PlayerNodeChangeListener;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.query.QueryOptions;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class LPMinestomPlugin extends AbstractLuckPermsPlugin {
    private final LPMinestomBootstrap bootstrap;

    private MinestomSenderFactory senderFactory;
    private MinestomContextManager contextManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private MinestomConnectionListener connectionListener;
    private MinestomCommandExecutor commandExecutor;

    public LPMinestomPlugin(LPMinestomBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    protected Set<Dependency> getGlobalDependencies() {
        return EnumSet.of(
                Dependency.CAFFEINE,
                Dependency.OKIO,
                Dependency.OKHTTP,
                Dependency.BYTEBUDDY,
                Dependency.EVENT,
                Dependency.CONFIGURATE_CORE,
                Dependency.CONFIGURATE_YAML,
                Dependency.SNAKEYAML
        );
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new MinestomSenderFactory(this);
    }

    public MinestomSenderFactory getSenderFactory() {
        return senderFactory;
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new MinestomConfigAdapter(this, resolveConfig("config.yml"));
    }

    @Override
    protected void registerPlatformListeners() {
        this.connectionListener = new MinestomConnectionListener(this);
        this.connectionListener.registerListeners();
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new MessagingFactory<>(this);
    }

    @Override
    protected void registerCommands() {
        this.commandExecutor = new MinestomCommandExecutor(this);
        this.commandExecutor.register();
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new MinestomCalculatorFactory(this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new MinestomContextManager(this);

        MinestomPlayerCalculator playerCalculator = new MinestomPlayerCalculator();
        this.contextManager.registerCalculator(playerCalculator);
    }

    @Override
    protected void setupPlatformHooks() {
        // No platform hooks necessary
    }

    @Override
    protected AbstractEventBus<?> provideEventBus(LuckPermsApiProvider apiProvider) {
        return new MinestomEventBus(this, apiProvider);
    }

    @Override
    protected void registerApiOnPlatform(LuckPerms api) {
        // Minestom doesn't have a services manager
    }

    @Override
    protected void performFinalSetup() {
        // register minestom permission system compatability
        new PlayerNodeChangeListener(this, LuckPermsProvider.get()).register();
    }

    @Override
    protected void registerHousekeepingTasks() {
        this.bootstrap.getScheduler().asyncRepeating(new ExpireTemporaryTask(this), 3, TimeUnit.SECONDS);
        this.bootstrap.getScheduler().asyncRepeating(new CacheHousekeepingTask(this), 2, TimeUnit.MINUTES);
    }

    @Override
    public LPMinestomBootstrap getBootstrap() {
        return this.bootstrap;
    }

    @Override
    public UserManager<? extends User> getUserManager() {
        return this.userManager;
    }

    @Override
    public GroupManager<? extends Group> getGroupManager() {
        return this.groupManager;
    }

    @Override
    public TrackManager<? extends Track> getTrackManager() {
        return this.trackManager;
    }

    @Override
    public CommandManager getCommandManager() {
        return this.commandExecutor;
    }

    @Override
    public AbstractConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public ContextManager<Player, Player> getContextManager() {
        return this.contextManager;
    }

    @Override
    public Optional<QueryOptions> getQueryOptionsForUser(User user) {
        return this.bootstrap.getPlayer(user.getUniqueId()).map(player -> this.contextManager.getQueryOptions(player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        return MinecraftServer.getConnectionManager().getOnlinePlayers().stream().map(p -> getSenderFactory().wrap(p));
    }

    @Override
    public Sender getConsoleSender() {
        return getSenderFactory().wrap(MinecraftServer.getCommandManager().getConsoleSender());
    }
}
