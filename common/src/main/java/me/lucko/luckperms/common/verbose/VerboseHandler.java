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

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.commands.sender.Sender;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * Accepts {@link CheckData} and passes it onto registered {@link VerboseListener}s.
 */
public class VerboseHandler implements Runnable {
    private final String pluginVersion;

    // the listeners currently registered
    private final Map<UUID, VerboseListener> listeners;

    // a queue of check data
    private final Queue<CheckData> queue;

    // if there are any listeners currently registered
    private boolean listening = false;

    // if the handler should shutdown
    private boolean shutdown = false;

    public VerboseHandler(Executor executor, String pluginVersion) {
        this.pluginVersion = "v" + pluginVersion;
        listeners = new ConcurrentHashMap<>();
        queue = new ConcurrentLinkedQueue<>();

        executor.execute(this);
    }

    /**
     * Offers check data to the handler, to be eventually passed onto listeners.
     *
     * <p>The check data is added to a queue to be processed later, to avoid blocking
     * the main thread each time a permission check is made.</p>
     *
     * @param checkOrigin the origin of the check
     * @param checkTarget the target of the permission check
     * @param checkContext the contexts where the check occurred
     * @param permission the permission which was checked for
     * @param result the result of the permission check
     */
    public void offerCheckData(CheckOrigin checkOrigin, String checkTarget, ContextSet checkContext, String permission, Tristate result) {
        // don't bother even processing the check if there are no listeners registered
        if (!listening) {
            return;
        }

        //noinspection ThrowableNotThrown
        StackTraceElement[] trace = new Exception().getStackTrace();

        // add the check data to a queue to be processed later.
        queue.offer(new CheckData(checkOrigin, checkTarget, checkContext.makeImmutable(), trace, permission, result));
    }

    /**
     * Registers a new listener for the given player.
     *
     * @param sender the sender to notify, if notify is true
     * @param filter the filter string
     * @param notify if the sender should be notified in chat on each check
     */
    public void registerListener(Sender sender, String filter, boolean notify) {
        listeners.put(sender.getUuid(), new VerboseListener(pluginVersion, sender, filter, notify));
        listening = true;
    }

    /**
     * Removes a listener for a given player
     *
     * @param uuid the players uuid
     * @return the existing listener, if one was actually registered
     */
    public VerboseListener unregisterListener(UUID uuid) {
        // immediately flush, so the listener gets all current data
        flush();

        return listeners.remove(uuid);
    }

    @Override
    public void run() {
        while (true) {

            // remove listeners where the sender is no longer valid
            listeners.values().removeIf(l -> !l.getNotifiedSender().isValid());

            // handle all checks in the queue
            flush();

            // break the loop if the handler has been shutdown
            if (shutdown) {
                return;
            }

            // update listening state
            listening = !listeners.isEmpty();

            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        }
    }

    /**
     * Flushes the current check data to the listeners.
     */
    public synchronized void flush() {
        for (CheckData e; (e = queue.poll()) != null; ) {
            for (VerboseListener listener : listeners.values()) {
                listener.acceptData(e);
            }
        }
    }

    public void shutdown() {
        shutdown = true;
    }
}
