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

package me.lucko.luckperms.api.event;

import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

/**
 * The internal LuckPerms event bus.
 *
 * <p>LuckPerms events are posted to any listeners registered with the bus.</p>
 *
 * @since 3.0
 */
public interface EventBus {

    /**
     * Subscribes to an event.
     *
     * @param eventClass the event class
     * @param handler    the event handler
     * @param <T>        the event class
     * @return an event handler instance representing this subscription
     */
    @Nonnull
    <T extends LuckPermsEvent> EventHandler<T> subscribe(@Nonnull Class<T> eventClass, @Nonnull Consumer<T> handler);

    /**
     * Gets a set of all registered handlers for a given event.
     *
     * @param eventClass the event to find handlers for
     * @param <T>        the event class
     * @return an immutable set of event handlers
     */
    @Nonnull
    <T extends LuckPermsEvent> Set<EventHandler<T>> getHandlers(@Nonnull Class<T> eventClass);

}
