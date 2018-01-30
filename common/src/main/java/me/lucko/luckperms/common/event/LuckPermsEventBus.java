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

package me.lucko.luckperms.common.event;

import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.api.event.Cancellable;
import me.lucko.luckperms.api.event.EventBus;
import me.lucko.luckperms.api.event.EventHandler;
import me.lucko.luckperms.api.event.LuckPermsEvent;
import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

public class LuckPermsEventBus implements EventBus {

    private final LuckPermsPlugin plugin;

    private final LuckPermsApiProvider apiProvider;

    private final Map<Class<? extends LuckPermsEvent>, Set<LuckPermsEventHandler<?>>> handlerMap = new ConcurrentHashMap<>();

    public LuckPermsEventBus(LuckPermsPlugin plugin, LuckPermsApiProvider apiProvider) {
        this.plugin = plugin;
        this.apiProvider = apiProvider;
    }

    @Nonnull
    @Override
    public <T extends LuckPermsEvent> EventHandler<T> subscribe(@Nonnull Class<T> eventClass, @Nonnull Consumer<T> handler) {
        Objects.requireNonNull(eventClass, "eventClass");
        Objects.requireNonNull(handler, "handler");

        if (!eventClass.isInterface()) {
            throw new IllegalArgumentException("class " + eventClass + " is not an interface");
        }
        if (!LuckPermsEvent.class.isAssignableFrom(eventClass)) {
            throw new IllegalArgumentException("class " + eventClass.getName() + " does not implement LuckPermsEvent");
        }

        Set<LuckPermsEventHandler<?>> handlers = this.handlerMap.computeIfAbsent(eventClass, c -> ConcurrentHashMap.newKeySet());

        LuckPermsEventHandler<T> eventHandler = new LuckPermsEventHandler<>(this, eventClass, handler);
        handlers.add(eventHandler);

        return eventHandler;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends LuckPermsEvent> Set<EventHandler<T>> getHandlers(@Nonnull Class<T> eventClass) {
        Set<LuckPermsEventHandler<?>> handlers = this.handlerMap.get(eventClass);
        if (handlers == null) {
            return ImmutableSet.of();
        } else {
            ImmutableSet.Builder<EventHandler<T>> ret = ImmutableSet.builder();
            for (LuckPermsEventHandler<?> handler : handlers) {
                ret.add((EventHandler<T>) handler);
            }

            return ret.build();
        }
    }

    public void unregisterHandler(LuckPermsEventHandler<?> handler) {
        Set<LuckPermsEventHandler<?>> handlers = this.handlerMap.get(handler.getEventClass());
        if (handlers != null) {
            handlers.remove(handler);
        }
    }

    public void fireEvent(LuckPermsEvent event) {
        if (event instanceof AbstractEvent) {
            ((AbstractEvent) event).setApi(this.apiProvider);
        }

        for (Map.Entry<Class<? extends LuckPermsEvent>, Set<LuckPermsEventHandler<?>>> ent : this.handlerMap.entrySet()) {
            if (!ent.getKey().isAssignableFrom(event.getClass())) {
                continue;
            }

            ent.getValue().forEach(h -> h.handle(event));
        }
    }

    public void fireEventAsync(LuckPermsEvent event) {
        if (event instanceof Cancellable) {
            throw new IllegalArgumentException("cannot call Cancellable event async");
        }
        this.plugin.getScheduler().doAsync(() -> fireEvent(event));
    }

    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }
}
