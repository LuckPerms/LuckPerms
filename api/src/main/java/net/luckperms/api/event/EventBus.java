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

package net.luckperms.api.event;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Set;
import java.util.function.Consumer;

/**
 * The LuckPerms event bus.
 *
 * <p>Used to subscribe (or "listen") to LuckPerms events.</p>
 */
public interface EventBus {

    /**
     * Registers a new subscription to the given event.
     *
     * <p>The returned {@link EventSubscription} instance encapsulates the subscription state. It has
     * methods which can be used to terminate the subscription, or view stats about the nature of
     * the subscription.</p>
     *
     * @param eventClass the event class
     * @param handler    the event handler
     * @param <T>        the event class
     * @return an event handler instance representing this subscription
     */
    @NonNull <T extends LuckPermsEvent> EventSubscription<T> subscribe(@NonNull Class<T> eventClass, @NonNull Consumer<? super T> handler);

    /**
     * Registers a new subscription to the given event.
     *
     * <p>The returned {@link EventSubscription} instance encapsulates the subscription state. It has
     * methods which can be used to terminate the subscription, or view stats about the nature of
     * the subscription.</p>
     *
     * <p>Unlike {@link #subscribe(Class, Consumer)}, this method accepts an additional parameter
     * for {@code plugin}. This object must be a "plugin" instance on the platform, and is used to
     * automatically {@link EventSubscription#close() unregister} the subscription when the
     * corresponding plugin is disabled.</p>
     *
     * @param <T>        the event class
     * @param plugin     a plugin instance to bind the subscription to.
     * @param eventClass the event class
     * @param handler    the event handler
     * @return an event handler instance representing this subscription
     */
    @NonNull <T extends LuckPermsEvent> EventSubscription<T> subscribe(Object plugin, @NonNull Class<T> eventClass, @NonNull Consumer<? super T> handler);

    /**
     * Gets a set of all registered handlers for a given event.
     *
     * @param eventClass the event to find handlers for
     * @param <T>        the event class
     * @return an immutable set of event handlers
     */
    @NonNull <T extends LuckPermsEvent> Set<EventSubscription<T>> getSubscriptions(@NonNull Class<T> eventClass);

}
