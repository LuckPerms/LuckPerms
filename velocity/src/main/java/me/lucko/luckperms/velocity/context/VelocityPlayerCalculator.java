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

package me.lucko.luckperms.velocity.context;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.velocity.LPVelocityPlugin;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

public class VelocityPlayerCalculator implements ContextCalculator<Player> {
    private final LPVelocityPlugin plugin;

    public VelocityPlayerCalculator(LPVelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void calculate(@NonNull Player subject, @NonNull ContextConsumer consumer) {
        ServerConnection server = subject.getCurrentServer().orElse(null);
        if (server == null) {
            return;
        }
        this.plugin.getConfiguration().get(ConfigKeys.WORLD_REWRITES).rewriteAndSubmit(server.getServerInfo().getName(), consumer);
    }

    @Override
    public @NotNull @NonNull ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();
        for (RegisteredServer server : this.plugin.getBootstrap().getProxy().getAllServers()) {
            builder.add(DefaultContextKeys.WORLD_KEY, server.getServerInfo().getName());
        }
        return builder.build();
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onServerConnect(ServerConnectedEvent e) {
        this.plugin.getContextManager().signalContextUpdate(e.getPlayer());
    }
}
