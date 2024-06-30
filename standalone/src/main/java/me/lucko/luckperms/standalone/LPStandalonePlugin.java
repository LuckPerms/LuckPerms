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

package me.lucko.luckperms.standalone;

import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
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
import me.lucko.luckperms.standalone.app.LuckPermsApplication;
import me.lucko.luckperms.standalone.app.integration.StandaloneUser;
import me.lucko.luckperms.standalone.stub.StandaloneContextManager;
import me.lucko.luckperms.standalone.stub.StandaloneDummyConnectionListener;
import me.lucko.luckperms.standalone.stub.StandaloneEventBus;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.query.QueryOptions;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * LuckPerms implementation for the standalone app.
 */
public class LPStandalonePlugin extends AbstractLuckPermsPlugin {
    private final LPStandaloneBootstrap bootstrap;

    private StandaloneSenderFactory senderFactory;
    private StandaloneDummyConnectionListener connectionListener;
    private StandaloneCommandManager commandManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private StandaloneContextManager contextManager;
    
    public LPStandalonePlugin(LPStandaloneBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public LPStandaloneBootstrap getBootstrap() {
        return this.bootstrap;
    }

    public LuckPermsApplication getLoader() {
        return this.bootstrap.getLoader();
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new StandaloneSenderFactory(this);
    }

    @Override
    protected Set<Dependency> getGlobalDependencies() {
        Set<Dependency> dependencies = super.getGlobalDependencies();
        dependencies.remove(Dependency.ADVENTURE);
        dependencies.add(Dependency.CONFIGURATE_CORE);
        dependencies.add(Dependency.CONFIGURATE_YAML);
        dependencies.add(Dependency.SNAKEYAML);
        return dependencies;
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new StandaloneConfigAdapter(this, resolveConfig("config.yml"));
    }

    @Override
    protected void registerPlatformListeners() {
        this.connectionListener = new StandaloneDummyConnectionListener(this);
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new StandaloneMessagingFactory(this);
    }

    @Override
    protected void registerCommands() {
        this.commandManager = new StandaloneCommandManager(this);
        this.bootstrap.getLoader().setCommandExecutor(this.commandManager);
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new StandaloneCalculatorFactory(this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new StandaloneContextManager(this);
    }

    @Override
    protected void setupPlatformHooks() {

    }

    @Override
    protected AbstractEventBus<?> provideEventBus(LuckPermsApiProvider apiProvider) {
        return new StandaloneEventBus(this, apiProvider);
    }

    @Override
    protected void registerApiOnPlatform(LuckPerms api) {
        this.bootstrap.getLoader().setApi(api);
    }

    @Override
    protected void performFinalSetup() {

    }

    @Override
    public Optional<QueryOptions> getQueryOptionsForUser(User user) {
        return Optional.empty();
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        return Stream.of(getConsoleSender());
    }

    @Override
    public Sender getConsoleSender() {
        return getSenderFactory().wrap(StandaloneUser.INSTANCE);
    }

    public StandaloneSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    @Override
    public AbstractConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public StandaloneCommandManager getCommandManager() {
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
    public StandaloneContextManager getContextManager() {
        return this.contextManager;
    }

}
