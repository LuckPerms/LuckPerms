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

package me.lucko.luckperms.forge.loader;

import me.lucko.luckperms.common.loader.JarInJarClassLoader;
import me.lucko.luckperms.common.loader.LoaderBootstrap;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Mod(value = "luckperms")
public class ForgeLoaderPlugin implements Supplier<ModContainer> {

    private static final String JAR_NAME = "luckperms-forge.jarinjar";
    private static final String BOOTSTRAP_CLASS = "me.lucko.luckperms.forge.LPForgeBootstrap";

    private final ModContainer container;
    private final LoaderBootstrap plugin;
    private final AtomicBoolean state;

    public ForgeLoaderPlugin() {
        JarInJarClassLoader loader = new JarInJarClassLoader(getClass().getClassLoader(), JAR_NAME);

        this.container = ModList.get().getModContainerByObject(this).orElse(null);
        this.plugin = loader.instantiatePlugin(BOOTSTRAP_CLASS, Supplier.class, this);
        this.state = new AtomicBoolean(false);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        MinecraftForge.EVENT_BUS.register(this);

        // Ensures LuckPerms is correctly shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!this.state.compareAndSet(true, false)) {
                return;
            }

            this.plugin.onDisable();
        }));
    }

    @Override
    public ModContainer get() {
        return this.container;
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        if (!this.state.compareAndSet(false, true)) {
            return;
        }

        this.plugin.onLoad();
        this.plugin.onEnable();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerStopped(ServerStoppedEvent event) {
        /* This event is fired client side when exiting a world,
           the client is still running, so we cannot disable LuckPerms. */
        if (!(event.getServer() instanceof DedicatedServer)) {
            return;
        }
        
        if (!this.state.compareAndSet(true, false)) {
            return;
        }

        this.plugin.onDisable();
    }

}
