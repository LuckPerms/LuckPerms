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
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.minecraft.MinecraftLuckPermsPlugin;
import me.lucko.luckperms.common.minecraft.listeners.MinecraftAutoOpListener;
import me.lucko.luckperms.common.minecraft.listeners.MinecraftCommandListUpdater;
import me.lucko.luckperms.fabric.context.FabricContextManager;
import me.lucko.luckperms.fabric.context.FabricPlayerCalculator;
import me.lucko.luckperms.fabric.listeners.FabricConnectionListener;
import me.lucko.luckperms.fabric.listeners.FabricOtherListeners;
import me.lucko.luckperms.fabric.listeners.FabricPermissionsApiListener;
import me.lucko.luckperms.fabric.listeners.FabricPermissionsListener;
import me.lucko.luckperms.fabric.messaging.FabricMessagingFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.ModContainer;
import net.luckperms.api.LuckPerms;
import net.minecraft.server.players.ServerOpList;

import java.io.IOException;
import java.util.Set;

public class LPFabricPlugin extends MinecraftLuckPermsPlugin<LPFabricPlugin, LPFabricBootstrap> {
    private FabricConnectionListener connectionListener;
    private FabricCommandExecutor commandManager;
    private FabricSenderFactory senderFactory;
    private FabricContextManager contextManager;

    public LPFabricPlugin(LPFabricBootstrap bootstrap) {
        super(bootstrap);
    }

    protected void registerFabricListeners() {
        // Events are registered very early on, and persist between game states
        this.connectionListener = new FabricConnectionListener(this);
        this.connectionListener.registerListeners();

        new FabricPermissionsApiListener(this).registerListeners();
        new FabricPermissionsListener().registerListeners();

        // Command registration also need to occur early, and will persist across game states as well.
        if (!skipCommandRegistration()) {
            this.commandManager = new FabricCommandExecutor(this);
            this.commandManager.register();
        }

        new FabricOtherListeners(this).registerListeners();
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
        return new FabricConfigAdapter(this, resolveConfig("luckperms.conf"));
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
    protected void setupContextManager() {
        this.contextManager = new FabricContextManager(this);

        FabricPlayerCalculator playerCalculator = new FabricPlayerCalculator(this, getConfiguration().get(ConfigKeys.DISABLED_CONTEXTS));
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
    protected void performFinalSetup() {
        // remove all operators on startup if they're disabled
        if (!getConfiguration().get(ConfigKeys.OPS_ENABLED)) {
            ServerLifecycleEvents.SERVER_STARTED.register(server -> {
                ServerOpList opList = server.getPlayerList().getOps();
                opList.getEntries().clear();
                try {
                    opList.save();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            });
        }

        // register autoop listener
        if (getConfiguration().get(ConfigKeys.AUTO_OP)) {
            getApiProvider().getEventBus().subscribe(new MinecraftAutoOpListener(this));
        }

        // register fabric command list updater
        if (getConfiguration().get(ConfigKeys.UPDATE_CLIENT_COMMAND_LIST)) {
            getApiProvider().getEventBus().subscribe(new MinecraftCommandListUpdater(this));
        }
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

}
