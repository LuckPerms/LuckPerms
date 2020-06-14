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
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.util.List;

class FabricEventBus extends AbstractEventBus<ModContainer> {
    protected FabricEventBus(LuckPermsPlugin plugin, LuckPermsApiProvider apiProvider) {
        super(plugin, apiProvider);
    }

    /**
     * Fabric doesn't have a "Mod" class you extend.
     * For simplicity, passing the ModContainer is allowed as other systems to detect the entrypoint get quite exhaustive.
     */
    @Override
    protected ModContainer checkPlugin(Object mod) throws IllegalArgumentException {
        if (mod instanceof ModContainer) {
            return (ModContainer) mod;
        }

        throw new IllegalArgumentException("Object " + mod + " (" + mod.getClass().getName() + ") is not a ModContainer.");
    }
}
