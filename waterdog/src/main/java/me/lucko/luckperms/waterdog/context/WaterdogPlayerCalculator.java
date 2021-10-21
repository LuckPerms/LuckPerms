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

package me.lucko.luckperms.waterdog.context;

import dev.waterdog.waterdogpe.event.EventManager;
import dev.waterdog.waterdogpe.event.defaults.TransferCompleteEvent;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import me.lucko.luckperms.waterdog.LPWaterdogPlugin;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;

import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;

import org.checkerframework.checker.nullness.qual.NonNull;

public class WaterdogPlayerCalculator implements ContextCalculator<ProxiedPlayer> {
    private final LPWaterdogPlugin plugin;

    public WaterdogPlayerCalculator(LPWaterdogPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(EventManager eventManager) {
        eventManager.subscribe(TransferCompleteEvent.class, this::onServerSwitch);
    }

    @Override
    public void calculate(@NonNull ProxiedPlayer subject, @NonNull ContextConsumer consumer) {
        ServerInfo server = subject.getServerInfo();

        if (server != null) {
            this.plugin.getConfiguration().get(ConfigKeys.WORLD_REWRITES).rewriteAndSubmit(server.getServerName(), consumer);
        }
    }

    @Override
    public @NonNull ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();
        for (ServerInfo server : this.plugin.getBootstrap().getProxy().getServers()) {
            builder.add(DefaultContextKeys.WORLD_KEY, server.getServerName());
        }
        return builder.build();
    }

    private void onServerSwitch(TransferCompleteEvent e) {
        this.plugin.getContextManager().signalContextUpdate(e.getPlayer());
    }
}
