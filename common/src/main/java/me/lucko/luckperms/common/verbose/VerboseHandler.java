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

package me.lucko.luckperms.common.verbose;

import me.lucko.luckperms.common.cacheddata.result.StringResult;
import me.lucko.luckperms.common.cacheddata.result.TristateResult;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.common.verbose.event.MetaCheckEvent;
import me.lucko.luckperms.common.verbose.event.PermissionCheckEvent;
import me.lucko.luckperms.common.verbose.event.VerboseEvent;
import net.luckperms.api.query.QueryOptions;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Accepts {@link VerboseEvent}s and passes them onto registered {@link VerboseListener}s.
 */
public class VerboseHandler implements AutoCloseable {

    /** A map of currently registered listeners */
    private final Map<UUID, VerboseListener> listeners;
    /** A queue of verbose events to be handled */
    private final Queue<VerboseEvent> queue;
    /** If there are any listeners registered */
    private boolean listening = false;
    /** The tick task */
    private final SchedulerTask task;

    public VerboseHandler(SchedulerAdapter scheduler) {
        this.listeners = new ConcurrentHashMap<>();
        this.queue = new ConcurrentLinkedQueue<>();
        this.task = scheduler.asyncRepeating(this::tick, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Offers permission check data to the handler, to be eventually passed onto listeners.
     *
     * <p>The check data is added to a queue to be processed later, to avoid blocking
     * the main thread each time a permission check is made.</p>
     *
     * @param origin the origin of the check
     * @param checkTarget the target of the permission check
     * @param checkQueryOptions the query options used for the check
     * @param permission the permission which was checked for
     * @param result the result of the permission check
     */
    public void offerPermissionCheckEvent(CheckOrigin origin, VerboseCheckTarget checkTarget, QueryOptions checkQueryOptions, String permission, TristateResult result) {
        // don't bother even processing the check if there are no listeners registered
        if (!this.listening) {
            return;
        }

        long time = System.currentTimeMillis();
        Throwable trace = new Throwable();
        String thread = Thread.currentThread().getName();

        // add the check data to a queue to be processed later.
        this.queue.offer(new PermissionCheckEvent(origin, checkTarget, checkQueryOptions, time, trace, thread, permission, result));
    }

    /**
     * Offers meta check data to the handler, to be eventually passed onto listeners.
     *
     * <p>The check data is added to a queue to be processed later, to avoid blocking
     * the main thread each time a meta check is made.</p>
     *
     * @param origin the origin of the check
     * @param checkTarget the target of the meta check
     * @param checkQueryOptions the query options used for the check
     * @param key the meta key which was checked for
     * @param result the result of the meta check
     */
    public void offerMetaCheckEvent(CheckOrigin origin, VerboseCheckTarget checkTarget, QueryOptions checkQueryOptions, String key, StringResult<?> result) {
        // don't bother even processing the check if there are no listeners registered
        if (!this.listening) {
            return;
        }

        long time = System.currentTimeMillis();
        Throwable trace = new Throwable();
        String thread = Thread.currentThread().getName();

        // add the check data to a queue to be processed later.
        this.queue.offer(new MetaCheckEvent(origin, checkTarget, checkQueryOptions, time, trace, thread, key, result));
    }

    /**
     * Registers a new listener for the given player.
     *
     * @param sender the sender to notify, if notify is true
     * @param filter the filter string
     * @param notify if the sender should be notified in chat on each check
     */
    public void registerListener(Sender sender, VerboseFilter filter, boolean notify) {
        // flush out anything before this listener was added
        flush();

        this.listeners.put(sender.getUniqueId(), new VerboseListener(sender, filter, notify));
        this.listening = true;
    }

    /**
     * Removes a listener for a given player
     *
     * @param sender the sender
     * @return the existing listener, if one was actually registered
     */
    public VerboseListener unregisterListener(Sender sender) {
        // immediately flush, so the listener gets all current data
        flush();

        return this.listeners.remove(sender.getUniqueId());
    }

    private void tick() {
        // remove listeners where the sender is no longer valid
        this.listeners.values().removeIf(l -> !l.getNotifiedSender().isValid());

        // handle all events in the queue
        flush();

        // update listening state
        this.listening = !this.listeners.isEmpty();
    }

    /**
     * Flushes the pending events to listeners.
     */
    public synchronized void flush() {
        for (VerboseEvent e; (e = this.queue.poll()) != null; ) {
            for (VerboseListener listener : this.listeners.values()) {
                listener.acceptEvent(e);
            }
        }
    }

    @Override
    public void close() {
        this.task.cancel();
    }

}
