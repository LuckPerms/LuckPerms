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

package me.lucko.luckperms.common.treeview;

import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class AsyncPermissionRegistry extends PermissionRegistry implements AutoCloseable {

    /** A queue of permission strings to be added to the tree */
    private final Queue<String> queue;
    /** The tick task */
    private final SchedulerTask task;

    public AsyncPermissionRegistry(SchedulerAdapter scheduler) {
        this.queue = new ConcurrentLinkedQueue<>();
        this.task = scheduler.asyncRepeating(this::tick, 1, TimeUnit.SECONDS);
    }

    @Override
    public void offer(String permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }
        this.queue.offer(permission);
    }

    private void tick() {
        for (String e; (e = this.queue.poll()) != null; ) {
            try {
                doInsert(e);
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    @Override
    public void close() {
        this.task.cancel();
    }

}
