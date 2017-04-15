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

import lombok.Setter;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.commands.sender.Sender;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

public class VerboseHandler implements Runnable {
    private final String pluginVersion;

    private final Map<UUID, VerboseListener> listeners;
    private final Queue<CheckData> queue;
    private boolean listening = false;

    @Setter
    private boolean shutdown = false;

    public VerboseHandler(Executor executor, String pluginVersion) {
        this.pluginVersion = "v" + pluginVersion;
        listeners = new ConcurrentHashMap<>();
        queue = new ConcurrentLinkedQueue<>();

        executor.execute(this);
    }

    public void offer(String checked, String node, Tristate value) {
        if (!listening) {
            return;
        }

        queue.offer(new CheckData(checked, node, value));
    }

    public void register(Sender sender, String filter, boolean notify) {
        listening = true;
        listeners.put(sender.getUuid(), new VerboseListener(pluginVersion, sender, filter, notify));
    }

    public VerboseListener unregister(UUID uuid) {
        flush();
        VerboseListener ret = listeners.remove(uuid);
        if (listeners.isEmpty()) {
            listening = false;
        }
        return ret;
    }

    @Override
    public void run() {
        while (true) {
            flush();

            if (shutdown) {
                return;
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {}
        }
    }

    public synchronized void flush() {
        for (CheckData e; (e = queue.poll()) != null; ) {
            for (VerboseListener listener : listeners.values()) {
                listener.acceptData(e);
            }
        }
    }
}
