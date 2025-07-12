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

package me.lucko.luckperms.library;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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
import me.lucko.luckperms.library.stub.LibraryCommandManager;
import me.lucko.luckperms.library.stub.LibraryConfigAdapter;
import me.lucko.luckperms.library.stub.LibraryConnectionListener;
import me.lucko.luckperms.library.stub.LibraryContextManager;
import me.lucko.luckperms.library.stub.LibraryEventBus;
import me.lucko.luckperms.library.stub.LibraryMessagingFactory;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.query.QueryOptions;

public class LPLibraryPlugin extends AbstractLuckPermsPlugin {

    private final LuckPermsLibraryManager manager;
    private final LuckPermsLibrary library;
    private final LPLibraryBootstrap bootstrap;

    private LibrarySenderFactory senderFactory;
    private LibraryConnectionListener connectionListener;
    private LibraryCommandManager commandManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private LibraryContextManager contextManager;

    public LPLibraryPlugin(LuckPermsLibraryManager manager, LuckPermsLibrary library, LPLibraryBootstrap bootstrap) {
        this.manager = manager;
        this.library = library;
        this.bootstrap = bootstrap;
    }

    @Override
    public LPLibraryBootstrap getBootstrap() {
        return this.bootstrap;
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new LibrarySenderFactory(this);
    }

    @Override
    protected Set<Dependency> getGlobalDependencies() {
        Set<Dependency> dependencies;
        if (manager.shouldLoadDefaultDependencies()) {
            dependencies = super.getGlobalDependencies();
            dependencies.remove(Dependency.ADVENTURE);
            dependencies.add(Dependency.CONFIGURATE_CORE);
            dependencies.add(Dependency.CONFIGURATE_YAML);
            dependencies.add(Dependency.SNAKEYAML);
        } else {
            dependencies = EnumSet.noneOf(Dependency.class);
        }
        manager.modifyDependencies(dependencies);
        return dependencies;
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new LibraryConfigAdapter(this, () -> manager.createConfigLoader(this::resolveConfig));
    }

    @Override
    protected void registerPlatformListeners() {
        this.connectionListener = new LibraryConnectionListener(this);
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new LibraryMessagingFactory(this);
    }

    @Override
    protected void registerCommands() {
        this.commandManager = new LibraryCommandManager(this);
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new LibraryCalculatorFactory(manager, this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new LibraryContextManager(this);
    }

    @Override
    protected void setupPlatformHooks() {
    }

    @Override
    protected AbstractEventBus<?> provideEventBus(LuckPermsApiProvider apiProvider) {
        return new LibraryEventBus(this, apiProvider);
    }

    @Override
    protected void registerApiOnPlatform(LuckPerms api) {
        library.setLuckPerms(api);
    }

    @Override
    protected void performFinalSetup() {
    }

    @Override
    public Optional<QueryOptions> getQueryOptionsForUser(User user) {
        return bootstrap.getPlayer(user.getUniqueId()).map(player -> contextManager.getQueryOptions(player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        return Stream.concat(Stream.of(getConsoleSender()), library.getOnlineSenders().stream().map(senderFactory::wrap));
    }

    @Override
    public Sender getConsoleSender() {
        return senderFactory.wrap(library.getConsoleSender());
    }

    public LibrarySenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    @Override
    public AbstractConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public LibraryCommandManager getCommandManager() {
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
    public LibraryContextManager getContextManager() {
        return this.contextManager;
    }

}
