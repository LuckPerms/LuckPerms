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

package me.lucko.luckperms.bukkit.listeners;

import com.github.benmanes.caffeine.cache.LoadingCache;
import me.lucko.luckperms.bukkit.LPBukkitBootstrap;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.cache.BufferedRequest;
import me.lucko.luckperms.common.event.LuckPermsEventListener;
import me.lucko.luckperms.common.util.CaffeineFactory;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.context.ContextUpdateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Calls {@link Player#updateCommands()} when a players permissions change.
 */
public class BukkitCommandListUpdater implements LuckPermsEventListener {

    public static boolean isSupported() {
        try {
            Player.class.getMethod("updateCommands");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private final LPBukkitPlugin plugin;
    private final LoadingCache<UUID, SendBuffer> sendingBuffers = CaffeineFactory.newBuilder()
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .build(SendBuffer::new);

    public BukkitCommandListUpdater(LPBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void bind(EventBus bus) {
        bus.subscribe(UserDataRecalculateEvent.class, this::onUserDataRecalculate);
        bus.subscribe(ContextUpdateEvent.class, this::onContextUpdate);
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent e) {
        requestUpdate(e.getUser().getUniqueId());
    }

    private void onContextUpdate(ContextUpdateEvent e) {
        e.getSubject(Player.class).ifPresent(p -> requestUpdate(p.getUniqueId()));
    }

    private void requestUpdate(UUID uniqueId) {
        if (this.plugin.getBootstrap().isServerStopping()) {
            return;
        }

        if (!this.plugin.getBootstrap().isPlayerOnline(uniqueId)) {
            return;
        }

        // Buffer the request to send a commands update.
        this.sendingBuffers.get(uniqueId).request();
    }

    // Called when the buffer times out.
    private void sendUpdate(UUID uniqueId) {
        LPBukkitBootstrap bootstrap = this.plugin.getBootstrap();
        if (bootstrap.isServerStopping()) {
            return;
        }

        Player player = bootstrap.getPlayer(uniqueId).orElse(null);
        if (player != null) {
            bootstrap.getScheduler().sync(player, player::updateCommands);
        }
    }

    private final class SendBuffer extends BufferedRequest<Void> {
        private final UUID uniqueId;

        SendBuffer(UUID uniqueId) {
            super(500, TimeUnit.MILLISECONDS, BukkitCommandListUpdater.this.plugin.getBootstrap().getScheduler());
            this.uniqueId = uniqueId;
        }

        @Override
        protected Void perform() {
            sendUpdate(this.uniqueId);
            return null;
        }
    }
}
