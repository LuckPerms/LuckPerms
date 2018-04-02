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

import me.lucko.luckperms.api.event.EventHandler;
import me.lucko.luckperms.api.event.LuckPermsEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Simple implementation of {@link EventHandler}.
 *
 * @param <T> the event type
 */
public class LuckPermsEventHandler<T extends LuckPermsEvent> implements EventHandler<T> {

    /**
     * The event bus which created this handler
     */
    private final AbstractEventBus<?> eventBus;

    /**
     * The event class
     */
    private final Class<T> eventClass;

    /**
     * The delegate "event handler"
     */
    private final Consumer<? super T> consumer;

    /**
     * The plugin which "owns" this handler
     */
    @Nullable
    private final Object plugin;

    /**
     * If this handler is active
     */
    private final AtomicBoolean active = new AtomicBoolean(true);

    /**
     * How many times this handler has been called
     */
    private final AtomicInteger callCount = new AtomicInteger(0);

    public LuckPermsEventHandler(AbstractEventBus<?> eventBus, Class<T> eventClass, Consumer<? super T> consumer, @Nullable Object plugin) {
        this.eventBus = eventBus;
        this.eventClass = eventClass;
        this.consumer = consumer;
        this.plugin = plugin;
    }

    @Override
    public boolean isActive() {
        return this.active.get();
    }

    @Override
    public boolean unregister() {
        // already unregistered
        if (!this.active.getAndSet(false)) {
            return false;
        }

        this.eventBus.unregisterHandler(this);
        return true;
    }

    @Override
    public int getCallCount() {
        return this.callCount.get();
    }

    @SuppressWarnings("unchecked") // we know that this method will never be called if the class doesn't match eventClass
    void handle(LuckPermsEvent event) {
        try {
            T t = (T) event;
            this.consumer.accept(t);
            this.callCount.incrementAndGet();
        } catch (Throwable t) {
            this.eventBus.getPlugin().getLogger().warn("Unable to pass event " + event.getClass().getSimpleName() + " to handler " + this.consumer.getClass().getName());
            t.printStackTrace();
        }
    }

    @Nonnull
    @Override
    public Class<T> getEventClass() {
        return this.eventClass;
    }

    @Nonnull
    @Override
    public Consumer<? super T> getConsumer() {
        return this.consumer;
    }

    @Nullable
    public Object getPlugin() {
        return this.plugin;
    }
}
