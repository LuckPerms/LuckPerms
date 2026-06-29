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

package me.lucko.luckperms.common.event.listeners;

import com.github.benmanes.caffeine.cache.LoadingCache;
import me.lucko.luckperms.common.api.implementation.ApiGroup;
import me.lucko.luckperms.common.cache.BufferedRequest;
import me.lucko.luckperms.common.event.LuckPermsEventListener;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.util.CaffeineFactory;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.context.ContextUpdateEvent;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Re-sends the command list/tree when a players permissions change.
 */
public abstract class AbstractCommandListUpdater<P extends LuckPermsPlugin, T> implements LuckPermsEventListener {
    protected final P plugin;
    private final Class<T> playerClass;
    private final LoadingCache<UUID, SendBuffer> sendingBuffers = CaffeineFactory.newBuilder()
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .build(SendBuffer::new);

    public AbstractCommandListUpdater(P plugin, Class<T> playerClass) {
        this.plugin = plugin;
        this.playerClass = playerClass;
    }

    protected abstract boolean isServerAvailable();
    protected abstract UUID getUniqueId(T player);

    // Called when the buffer times out.
    protected abstract void sendCommandListUpdate(UUID uniqueId);

    @Override
    public final void bind(EventBus bus) {
        bus.subscribe(UserDataRecalculateEvent.class, this::onUserDataRecalculate);
        bus.subscribe(GroupDataRecalculateEvent.class, this::onGroupDataRecalculate);
        bus.subscribe(ContextUpdateEvent.class, this::onContextUpdate);
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent e) {
        requestUpdate(e.getUser().getUniqueId());
    }

    private void onGroupDataRecalculate(GroupDataRecalculateEvent e) {
        for (User user : this.plugin.getUserManager().getAll().values()) {
            if (user.resolveInheritanceTree(user.getQueryOptions()).contains(ApiGroup.cast(e.getGroup()))) {
                requestUpdate(user.getUniqueId());
            }
        }
    }

    private void onContextUpdate(ContextUpdateEvent e) {
        e.getSubject(this.playerClass).ifPresent(p -> requestUpdate(getUniqueId(p)));
    }

    private void requestUpdate(UUID uniqueId) {
        if (!isServerAvailable()) {
            return;
        }

        if (!this.plugin.getBootstrap().isPlayerOnline(uniqueId)) {
            return;
        }

        this.sendingBuffers.get(uniqueId).request();
    }

    private final class SendBuffer extends BufferedRequest<Void> {
        private final UUID uniqueId;

        SendBuffer(UUID uniqueId) {
            super(500, TimeUnit.MILLISECONDS, AbstractCommandListUpdater.this.plugin.getBootstrap().getScheduler());
            this.uniqueId = uniqueId;
        }

        @Override
        protected Void perform() {
            sendCommandListUpdate(this.uniqueId);
            return null;
        }
    }

}
