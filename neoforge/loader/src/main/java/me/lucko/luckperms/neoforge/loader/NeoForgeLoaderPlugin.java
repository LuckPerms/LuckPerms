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

package me.lucko.luckperms.neoforge.loader;

import me.lucko.luckperms.common.loader.JarInJarClassLoader;
import me.lucko.luckperms.common.loader.LoaderBootstrap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

@Mod(value = "luckperms")
public class NeoForgeLoaderPlugin implements Supplier<ModContainer> {
    private static final Logger LOGGER = LogManager.getLogger("luckperms");

    private static final String JAR_NAME = "luckperms-neoforge.jarinjar";
    private static final String BOOTSTRAP_CLASS = "me.lucko.luckperms.neoforge.LPNeoForgeBootstrap";

    private final ModContainer container;

    private JarInJarClassLoader loader;
    private LoaderBootstrap plugin;

    public NeoForgeLoaderPlugin(final ModContainer modContainer, final IEventBus modBus) {
        this.container = modContainer;

        if (FMLEnvironment.dist.isClient()) {
            LOGGER.info("Skipping LuckPerms init (not supported on the client!)");
            return;
        }

        this.loader = new JarInJarClassLoader(getClass().getClassLoader(), JAR_NAME);
        modBus.addListener(this::onCommonSetup);
    }

    @Override
    public ModContainer get() {
        return this.container;
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        this.plugin = this.loader.instantiatePlugin(BOOTSTRAP_CLASS, Supplier.class, this);
        this.plugin.onLoad();
    }

}
