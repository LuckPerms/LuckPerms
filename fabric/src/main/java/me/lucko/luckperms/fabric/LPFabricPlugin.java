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

package me.lucko.luckperms.fabric;

import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager;
import me.lucko.luckperms.common.model.manager.track.StandardTrackManager;
import me.lucko.luckperms.common.model.manager.user.StandardUserManager;
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin;
import me.lucko.luckperms.common.sender.DummySender;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.tasks.CacheHousekeepingTask;
import me.lucko.luckperms.common.tasks.ExpireTemporaryTask;
import me.lucko.luckperms.common.util.MoreFiles;
import me.lucko.luckperms.fabric.context.FabricContextManager;
import me.lucko.luckperms.fabric.context.FabricPlayerCalculator;
import me.lucko.luckperms.fabric.listeners.FabricConnectionListener;
import me.lucko.luckperms.fabric.listeners.PermissionCheckListener;
import me.lucko.luckperms.fabric.messaging.FabricMessagingFactory;

import net.fabricmc.loader.api.ModContainer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class LPFabricPlugin extends AbstractLuckPermsPlugin {
    private final LPFabricBootstrap bootstrap;

    private FabricConnectionListener connectionListener;
    private FabricCommandExecutor commandManager;
    private FabricSenderFactory senderFactory;
    private FabricContextManager contextManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;

    public LPFabricPlugin(LPFabricBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public LPFabricBootstrap getBootstrap() {
        return this.bootstrap;
    }

    protected void registerFabricListeners() {
        // Events are registered very early on, and persist between game states
        this.connectionListener = new FabricConnectionListener(this);
        this.connectionListener.registerListeners();

        new PermissionCheckListener(this).registerListeners();

        // Command registration also need to occur early, and will persist across game states as well.
        this.commandManager = new FabricCommandExecutor(this);
        this.commandManager.register();
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new FabricSenderFactory(this);
    }

    @Override
    protected Set<Dependency> getGlobalDependencies() {
        Set<Dependency> dependencies = super.getGlobalDependencies();
        dependencies.add(Dependency.CONFIGURATE_CORE);
        dependencies.add(Dependency.CONFIGURATE_HOCON);
        dependencies.add(Dependency.HOCON_CONFIG);
        return dependencies;
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        Path configPath = this.getBootstrap().getConfigDirectory().resolve("luckperms.conf");

        if (!Files.exists(configPath)) {
            try {
                MoreFiles.createDirectoriesIfNotExists(this.bootstrap.getConfigDirectory());
                try (InputStream is = this.getBootstrap().getResourceStream("luckperms.conf")) {
                    Files.copy(is, configPath);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return new FabricConfigAdapter(this, configPath);
    }

    @Override
    protected void registerPlatformListeners() {
        // Too late for Fabric, registered in #registerFabricListeners
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new FabricMessagingFactory(this);
    }

    @Override
    protected void registerCommands() {
        // Too late for Fabric, registered in #registerFabricListeners
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new FabricCalculatorFactory(this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new FabricContextManager(this);

        FabricPlayerCalculator playerCalculator = new FabricPlayerCalculator(this);
        playerCalculator.registerListeners();
        this.contextManager.registerCalculator(playerCalculator);
    }

    @Override
    protected void setupPlatformHooks() {
    }

    @Override
    protected AbstractEventBus<ModContainer> provideEventBus(LuckPermsApiProvider provider) {
        return new FabricEventBus(this, provider);
    }

    @Override
    protected void registerApiOnPlatform(LuckPerms api) {
    }

    @Override
    protected void registerHousekeepingTasks() {
        this.bootstrap.getScheduler().asyncRepeating(new ExpireTemporaryTask(this), 3, TimeUnit.SECONDS);
        this.bootstrap.getScheduler().asyncRepeating(new CacheHousekeepingTask(this), 2, TimeUnit.MINUTES);
    }

    @Override
    protected void performFinalSetup() {
    }

    public FabricSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    @Override
    public FabricConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public FabricCommandExecutor getCommandManager() {
        return this.commandManager;
    }

    @Override
    public FabricContextManager getContextManager() {
        return this.contextManager;
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
    public Optional<QueryOptions> getQueryOptionsForUser(User user) {
        return this.bootstrap.getPlayer(user.getUniqueId()).map(player -> this.contextManager.getQueryOptions(player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        return Stream.concat(
                Stream.of(getConsoleSender()),
                this.bootstrap.getServer().map(MinecraftServer::getPlayerManager).map(s -> s.getPlayerList().stream().map(p -> this.senderFactory.wrap(p.getCommandSource()))).orElseGet(Stream::empty)
        );
    }

    @Override
    public Sender getConsoleSender() {
        return this.bootstrap.getServer()
                .map(s -> this.senderFactory.wrap(s.getCommandSource()))
                .orElseGet(() -> new DummySender(this, Sender.CONSOLE_UUID, Sender.CONSOLE_NAME) {
                    @Override
                    public void sendMessage(Component message) {
                        LPFabricPlugin.this.bootstrap.getPluginLogger().info(PlainComponentSerializer.plain().serialize(TranslationManager.render(message)));
                    }
                });
    }

}
