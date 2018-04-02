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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import me.lucko.luckperms.api.event.Cancellable;
import me.lucko.luckperms.api.event.EventBus;
import me.lucko.luckperms.api.event.EventHandler;
import me.lucko.luckperms.api.event.LuckPermsEvent;
import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

/**
 * Simple implementation of EventBus.
 */
public class LuckPermsEventBus implements EventBus {

    /**
     * The plugin instance
     */
    private final LuckPermsPlugin plugin;

    /**
     * The api provider instance
     */
    private final LuckPermsApiProvider apiProvider;

    /**
     * The registered handlers in this event bus
     */
    private final Multimap<Class<? extends LuckPermsEvent>, LuckPermsEventHandler<?>> handlerMap = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);

    /**
     * A cache of event class --> applicable handlers.
     *
     * A registered "handler" will be passed all possible events it can handle, according to
     * {@link Class#isAssignableFrom(Class)}.
     */
    private final LoadingCache<Class<? extends LuckPermsEvent>, List<LuckPermsEventHandler<?>>> handlerCache = Caffeine.newBuilder()
            .build(eventClass -> {
                ImmutableList.Builder<LuckPermsEventHandler<?>> matched = ImmutableList.builder();
                LuckPermsEventBus.this.handlerMap.asMap().forEach((clazz, handlers) -> {
                    if (clazz.isAssignableFrom(eventClass)) {
                        matched.addAll(handlers);
                    }
                });
                return matched.build();
            });

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

        LuckPermsEventHandler<T> eventHandler = new LuckPermsEventHandler<>(this, eventClass, handler);
        this.handlerMap.put(eventClass, eventHandler);
        this.handlerCache.invalidateAll();

        return eventHandler;
    }

    @Nonnull
    @Override
    public <T extends LuckPermsEvent> Set<EventHandler<T>> getHandlers(@Nonnull Class<T> eventClass) {
        Collection<LuckPermsEventHandler<?>> handlers = this.handlerMap.asMap().get(eventClass);
        ImmutableSet.Builder<EventHandler<T>> ret = ImmutableSet.builder();
        for (LuckPermsEventHandler<?> handler : handlers) {
            //noinspection unchecked
            ret.add((EventHandler<T>) handler);
        }
        return ret.build();
    }

    public void unregisterHandler(LuckPermsEventHandler<?> handler) {
        this.handlerMap.remove(handler.getEventClass(), handler);
        this.handlerCache.invalidateAll();
    }

    public void fireEvent(LuckPermsEvent event) {
        if (event instanceof AbstractEvent) {
            ((AbstractEvent) event).setApi(this.apiProvider);
        }

        List<LuckPermsEventHandler<?>> handlers = this.handlerCache.get(event.getClass());
        if (handlers == null) {
            return;
        }

        for (LuckPermsEventHandler<?> handler : handlers) {
            handler.handle(event);
        }
    }

    public void fireEventAsync(LuckPermsEvent event) {
        if (event instanceof Cancellable) {
            throw new IllegalArgumentException("cannot call Cancellable event async");
        }
        this.plugin.getBootstrap().getScheduler().doAsync(() -> fireEvent(event));
    }

    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }
}
