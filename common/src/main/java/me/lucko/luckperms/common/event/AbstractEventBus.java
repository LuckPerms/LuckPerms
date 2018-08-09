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

import me.lucko.luckperms.api.event.EventBus;
import me.lucko.luckperms.api.event.EventHandler;
import me.lucko.luckperms.api.event.LuckPermsEvent;
import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import net.kyori.event.EventSubscriber;
import net.kyori.event.SimpleEventBus;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

public abstract class AbstractEventBus<P> implements EventBus, AutoCloseable {

    /**
     * The plugin instance
     */
    private final LuckPermsPlugin plugin;

    /**
     * The api provider instance
     */
    private final LuckPermsApiProvider apiProvider;

    /**
     * The delegate event bus
     */
    private final Bus bus = new Bus();

    protected AbstractEventBus(LuckPermsPlugin plugin, LuckPermsApiProvider apiProvider) {
        this.plugin = plugin;
        this.apiProvider = apiProvider;
    }

    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    public LuckPermsApiProvider getApiProvider() {
        return this.apiProvider;
    }

    /**
     * Checks that the given plugin object is a valid plugin instance for the platform
     *
     * @param plugin the object
     * @return a plugin
     * @throws IllegalArgumentException if the plugin is invalid
     */
    protected abstract P checkPlugin(Object plugin) throws IllegalArgumentException;

    public void post(LuckPermsEvent event) {
        this.bus.post(event);
    }

    @Nonnull
    @Override
    public <T extends LuckPermsEvent> EventHandler<T> subscribe(@Nonnull Class<T> eventClass, @Nonnull Consumer<? super T> handler) {
        Objects.requireNonNull(eventClass, "eventClass");
        Objects.requireNonNull(handler, "handler");
        return registerSubscription(eventClass, handler, null);
    }

    @Nonnull
    @Override
    public <T extends LuckPermsEvent> EventHandler<T> subscribe(Object plugin, @Nonnull Class<T> eventClass, @Nonnull Consumer<? super T> handler) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(eventClass, "eventClass");
        Objects.requireNonNull(handler, "handler");
        return registerSubscription(eventClass, handler, checkPlugin(plugin));
    }

    private <T extends LuckPermsEvent> EventHandler<T> registerSubscription(Class<T> eventClass, Consumer<? super T> handler, Object plugin) {
        if (!eventClass.isInterface()) {
            throw new IllegalArgumentException("class " + eventClass + " is not an interface");
        }
        if (!LuckPermsEvent.class.isAssignableFrom(eventClass)) {
            throw new IllegalArgumentException("class " + eventClass.getName() + " does not implement LuckPermsEvent");
        }

        LuckPermsEventHandler<T> eventHandler = new LuckPermsEventHandler<>(this, eventClass, handler, plugin);
        this.bus.register(eventClass, eventHandler);

        return eventHandler;
    }

    @Nonnull
    @Override
    public <T extends LuckPermsEvent> Set<EventHandler<T>> getHandlers(@Nonnull Class<T> eventClass) {
        Set<LuckPermsEventHandler<?>> handlers = this.bus.getHandlers();
        handlers.removeIf(h -> !h.getEventClass().isAssignableFrom(eventClass));

        //noinspection unchecked
        return (Set) ImmutableSet.copyOf(handlers);
    }

    /**
     * Removes a specific handler from the bus
     *
     * @param handler the handler to remove
     */
    public void unregisterHandler(LuckPermsEventHandler<?> handler) {
        this.bus.unregister(handler);
    }

    /**
     * Removes all handlers for a specific plugin
     *
     * @param plugin the plugin
     */
    protected void unregisterHandlers(P plugin) {
        this.bus.unregisterMatching(sub -> ((LuckPermsEventHandler) sub).getPlugin() == plugin);
    }

    @Override
    public void close() {
        this.bus.unregisterAll();
    }

    private static final class Bus extends SimpleEventBus<LuckPermsEvent> {

        @Override
        public void unregisterMatching(@Nonnull Predicate<EventSubscriber<?>> predicate) {
            super.unregisterMatching(predicate);
        }

        public void unregisterAll() {
            unregisterMatching(Predicates.alwaysTrue());
        }

        public Set<LuckPermsEventHandler<?>> getHandlers() {
            Set<LuckPermsEventHandler<?>> handlers = new HashSet<>();

            // bit of a hack
            unregisterMatching(sub -> {
                handlers.add((LuckPermsEventHandler<?>) sub);
                return false;
            });

            return handlers;
        }
    }
}
