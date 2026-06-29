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

import me.lucko.luckperms.common.api.implementation.ApiUser;
import me.lucko.luckperms.common.context.manager.ContextManager;
import me.lucko.luckperms.common.event.LuckPermsEventListener;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.context.ContextUpdateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.query.QueryOptions;

import java.util.Map;
import java.util.UUID;

/**
 * Implements the LuckPerms auto op feature.
 */
public abstract class AbstractAutoOpListener<P extends LuckPermsPlugin, T> implements LuckPermsEventListener {
    private static final String NODE = "luckperms.autoop";

    protected final P plugin;
    private final ContextManager<T, T> contextManager;
    private final Class<T> playerClass;

    public AbstractAutoOpListener(P plugin, ContextManager<T, T> contextManager, Class<T> playerClass) {
        this.plugin = plugin;
        this.contextManager = contextManager;
        this.playerClass = playerClass;
    }

    protected abstract boolean isServerAvailable();
    protected abstract UUID getUniqueId(T player);
    protected abstract void setOp(T player, boolean value, boolean callerIsSync);

    @Override
    public final void bind(EventBus bus) {
        bus.subscribe(ContextUpdateEvent.class, this::onContextUpdate);
        bus.subscribe(UserDataRecalculateEvent.class, this::onUserDataRecalculate);
    }

    private void onContextUpdate(ContextUpdateEvent event) {
        event.getSubject(this.playerClass).ifPresent(player -> refreshAutoOp(player, true));
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        User user = ApiUser.cast(event.getUser());
        T player = getPlayerByUuid(user.getUniqueId());
        if (player != null) {
            refreshAutoOp(player, false);
        }
    }

    @SuppressWarnings("unchecked")
    private T getPlayerByUuid(UUID uuid) {
        return (T) this.plugin.getBootstrap().getPlayer(uuid).orElse(null);
    }

    private void refreshAutoOp(T player, boolean callerIsSync) {
        if (!callerIsSync && !isServerAvailable()) {
            return;
        }

        User user = this.plugin.getUserManager().getIfLoaded(getUniqueId(player));

        boolean value;
        if (user != null) {
            QueryOptions queryOptions = this.contextManager.getQueryOptions(player);
            Map<String, Boolean> permData = user.getCachedData().getPermissionData(queryOptions).getPermissionMap();
            value = permData.getOrDefault(NODE, false);
        } else {
            value = false;
        }

        setOp(player, value, callerIsSync);
    }

}
