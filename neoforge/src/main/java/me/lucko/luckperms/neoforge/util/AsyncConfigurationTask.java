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

package me.lucko.luckperms.neoforge.util;

import me.lucko.luckperms.neoforge.LPNeoForgePlugin;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.server.network.ConfigurationTask;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AsyncConfigurationTask implements ConfigurationTask {
    private final LPNeoForgePlugin plugin;
    private final Type type;
    private final Runnable task;
    private final ServerConfigurationPacketListener listener;

    public AsyncConfigurationTask(LPNeoForgePlugin plugin, Type type, Runnable task, ServerConfigurationPacketListener listener) {
        this.plugin = plugin;
        this.type = type;
        this.task = task;
        this.listener = listener;
    }

    @Override
    public void start(Consumer<Packet<?>> send) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(this.task, this.plugin.getBootstrap().getScheduler().async());
        future.whenCompleteAsync((o, e) -> {
            if (e != null) {
                this.plugin.getLogger().warn("Configuration task threw an exception", e);
            }
            this.listener.finishCurrentTask(this.type);
        });
    }

    @Override
    public Type type() {
        return this.type;
    }
}