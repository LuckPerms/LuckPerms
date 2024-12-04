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

package me.lucko.luckperms.neoforge;

import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.config.ConfigKeys;
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
import me.lucko.luckperms.common.sender.DummyConsoleSender;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.neoforge.calculator.NeoForgeCalculatorFactory;
import me.lucko.luckperms.neoforge.context.NeoForgeContextManager;
import me.lucko.luckperms.neoforge.context.NeoForgePlayerCalculator;
import me.lucko.luckperms.neoforge.listeners.NeoForgeAutoOpListener;
import me.lucko.luckperms.neoforge.listeners.NeoForgeCommandListUpdater;
import me.lucko.luckperms.neoforge.listeners.NeoForgeConnectionListener;
import me.lucko.luckperms.neoforge.listeners.NeoForgePlatformListener;
import me.lucko.luckperms.neoforge.messaging.NeoForgeMessagingFactory;
import me.lucko.luckperms.neoforge.messaging.PluginMessageMessenger;
import me.lucko.luckperms.neoforge.service.NeoForgePermissionHandlerListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.neoforged.fml.ModContainer;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * LuckPerms implementation for Forge.
 */
public class LPNeoForgePlugin extends AbstractLuckPermsPlugin {
    private final LPNeoForgeBootstrap bootstrap;

    private NeoForgeSenderFactory senderFactory;
    private NeoForgeConnectionListener connectionListener;
    private NeoForgeCommandExecutor commandManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private NeoForgeContextManager contextManager;

    public LPNeoForgePlugin(LPNeoForgeBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public LPNeoForgeBootstrap getBootstrap() {
        return this.bootstrap;
    }

    protected void registerEarlyListeners() {
        this.connectionListener = new NeoForgeConnectionListener(this);
        this.bootstrap.registerListeners(this.connectionListener);

        NeoForgePlatformListener platformListener = new NeoForgePlatformListener(this);
        this.bootstrap.registerListeners(platformListener);

        NeoForgePermissionHandlerListener permissionHandlerListener = new NeoForgePermissionHandlerListener(this);
        this.bootstrap.registerListeners(permissionHandlerListener);

        this.commandManager = new NeoForgeCommandExecutor(this);
        this.bootstrap.registerListeners(this.commandManager);

        PluginMessageMessenger.registerChannel();
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new NeoForgeSenderFactory(this);
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
        return new NeoForgeConfigAdapter(this, resolveConfig("luckperms.conf"));
    }

    @Override
    protected void registerPlatformListeners() {
        // Too late for Forge, registered in #registerEarlyListeners
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new NeoForgeMessagingFactory(this);
    }

    @Override
    protected void registerCommands() {
        // Too late for Forge, registered in #registerEarlyListeners
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new NeoForgeCalculatorFactory(this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new NeoForgeContextManager(this);

        NeoForgePlayerCalculator playerCalculator = new NeoForgePlayerCalculator(this, getConfiguration().get(ConfigKeys.DISABLED_CONTEXTS));
        this.bootstrap.registerListeners(playerCalculator);
        this.contextManager.registerCalculator(playerCalculator);
    }

    @Override
    protected void setupPlatformHooks() {
    }

    @Override
    protected AbstractEventBus<ModContainer> provideEventBus(LuckPermsApiProvider provider) {
        return new NeoForgeEventBus(this, provider);
    }

    @Override
    protected void registerApiOnPlatform(LuckPerms api) {
    }

    @Override
    protected void performFinalSetup() {
        // register autoop listener
        if (getConfiguration().get(ConfigKeys.AUTO_OP)) {
            getApiProvider().getEventBus().subscribe(new NeoForgeAutoOpListener(this));
        }

        // register forge command list updater
        if (getConfiguration().get(ConfigKeys.UPDATE_CLIENT_COMMAND_LIST)) {
            getApiProvider().getEventBus().subscribe(new NeoForgeCommandListUpdater(this));
        }
    }

    @Override
    public Optional<QueryOptions> getQueryOptionsForUser(User user) {
        return this.bootstrap.getPlayer(user.getUniqueId()).map(player -> this.contextManager.getQueryOptions(player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        return Stream.concat(
                Stream.of(getConsoleSender()),
                this.bootstrap.getServer()
                        .map(MinecraftServer::getPlayerList)
                        .map(PlayerList::getPlayers)
                        .map(players -> players.stream().map(player -> this.senderFactory.wrap(player.createCommandSourceStack()))).orElseGet(Stream::empty)
        );
    }

    @Override
    public Sender getConsoleSender() {
        return this.bootstrap.getServer()
                .map(server -> this.senderFactory.wrap(server.createCommandSourceStack()))
                .orElseGet(() -> new DummyConsoleSender(this) {
                    @Override
                    public void sendMessage(Component message) {
                        LPNeoForgePlugin.this.bootstrap.getPluginLogger().info(PlainTextComponentSerializer.plainText().serialize(TranslationManager.render(message)));
                    }
                });
    }

    public NeoForgeSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    @Override
    public NeoForgeConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public NeoForgeCommandExecutor getCommandManager() {
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
    public NeoForgeContextManager getContextManager() {
        return this.contextManager;
    }

}
