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

package me.lucko.luckperms.forge.util;

import me.lucko.luckperms.forge.LPForgePlugin;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraftforge.network.config.ConfigurationTaskContext;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class AsyncConfigurationTask implements ConfigurationTask {
    private final LPForgePlugin plugin;
    private final Type type;
    private final Function<ConfigurationTaskContext, CompletableFuture<?>> task;

    public AsyncConfigurationTask(LPForgePlugin plugin, Type type, Function<ConfigurationTaskContext, CompletableFuture<?>> task) {
        this.plugin = plugin;
        this.type = type;
        this.task = task;
    }

    public AsyncConfigurationTask(LPForgePlugin plugin, Type type, Supplier<CompletableFuture<?>> task) {
        this(plugin, type, c -> task.get());
    }

    @Override
    public void start(ConfigurationTaskContext ctx) {
        CompletableFuture<?> future = this.task.apply(ctx);
        future.whenCompleteAsync((o, e) -> {
            if (e != null) {
                this.plugin.getLogger().warn("Configuration task threw an exception", e);
            }
            ctx.finish(type());
        }, this.plugin.getBootstrap().getScheduler()::sync); // do we need to call this sync?
    }

    @Override
    public void start(Consumer<Packet<?>> send) {
        throw new IllegalStateException("This should never be called");
    }

    @Override
    public Type type() {
        return this.type;
    }
}