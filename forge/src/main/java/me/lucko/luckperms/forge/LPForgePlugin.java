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

package me.lucko.luckperms.forge;

import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.minecraft.MinecraftLuckPermsPlugin;
import me.lucko.luckperms.common.minecraft.listeners.MinecraftAutoOpListener;
import me.lucko.luckperms.common.minecraft.listeners.MinecraftCommandListUpdater;
import me.lucko.luckperms.forge.context.ForgeContextManager;
import me.lucko.luckperms.forge.context.ForgePlayerCalculator;
import me.lucko.luckperms.forge.listeners.ForgeConnectionListener;
import me.lucko.luckperms.forge.listeners.ForgePlatformListener;
import me.lucko.luckperms.forge.messaging.ForgeMessagingFactory;
import me.lucko.luckperms.forge.messaging.PluginMessageMessenger;
import me.lucko.luckperms.forge.service.ForgePermissionHandlerListener;
import net.luckperms.api.LuckPerms;
import net.minecraftforge.fml.ModContainer;

import java.util.Set;

/**
 * LuckPerms implementation for Forge.
 */
public class LPForgePlugin extends MinecraftLuckPermsPlugin<LPForgePlugin, LPForgeBootstrap> {
    private ForgeSenderFactory senderFactory;
    private ForgeConnectionListener connectionListener;
    private ForgeCommandExecutor commandManager;
    private ForgeContextManager contextManager;

    public LPForgePlugin(LPForgeBootstrap bootstrap) {
        super(bootstrap);
    }

    protected void registerEarlyListeners() {
        this.connectionListener = new ForgeConnectionListener(this);
        this.bootstrap.registerListeners(this.connectionListener);

        ForgePlatformListener platformListener = new ForgePlatformListener(this);
        this.bootstrap.registerListeners(platformListener);

        ForgePermissionHandlerListener permissionHandlerListener = new ForgePermissionHandlerListener(this);
        this.bootstrap.registerListeners(permissionHandlerListener);

        if (!skipCommandRegistration()) {
            this.commandManager = new ForgeCommandExecutor(this);
            this.bootstrap.registerListeners(this.commandManager);
        }

        PluginMessageMessenger.registerChannel();
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new ForgeSenderFactory(this);
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
        return new ForgeConfigAdapter(this, resolveConfig("luckperms.conf"));
    }

    @Override
    protected void registerPlatformListeners() {
        // Too late for Forge, registered in #registerEarlyListeners
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new ForgeMessagingFactory(this);
    }

    @Override
    protected void registerCommands() {
        // Too late for Forge, registered in #registerEarlyListeners
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new ForgeContextManager(this);

        ForgePlayerCalculator playerCalculator = new ForgePlayerCalculator(this, getConfiguration().get(ConfigKeys.DISABLED_CONTEXTS));
        this.bootstrap.registerListeners(playerCalculator);
        this.contextManager.registerCalculator(playerCalculator);
    }

    @Override
    protected void setupPlatformHooks() {
    }

    @Override
    protected AbstractEventBus<ModContainer> provideEventBus(LuckPermsApiProvider provider) {
        return new ForgeEventBus(this, provider);
    }

    @Override
    protected void registerApiOnPlatform(LuckPerms api) {
    }

    @Override
    protected void performFinalSetup() {
        // register autoop listener
        if (getConfiguration().get(ConfigKeys.AUTO_OP)) {
            getApiProvider().getEventBus().subscribe(new MinecraftAutoOpListener(this));
        }

        // register forge command list updater
        if (getConfiguration().get(ConfigKeys.UPDATE_CLIENT_COMMAND_LIST)) {
            getApiProvider().getEventBus().subscribe(new MinecraftCommandListUpdater(this));
        }
    }

    public ForgeSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    @Override
    public ForgeConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public ForgeCommandExecutor getCommandManager() {
        return this.commandManager;
    }

    @Override
    public ForgeContextManager getContextManager() {
        return this.contextManager;
    }

}
