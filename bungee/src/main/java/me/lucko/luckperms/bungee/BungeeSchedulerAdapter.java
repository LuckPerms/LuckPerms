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

package me.lucko.luckperms.bungee;

import me.lucko.luckperms.common.plugin.SchedulerAdapter;
import me.lucko.luckperms.common.plugin.SchedulerTask;
import me.lucko.luckperms.common.utils.SafeIteration;

import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.api.scheduler.TaskScheduler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class BungeeSchedulerAdapter implements SchedulerAdapter {

    // the number of ticks which occur in a second - this is a server implementation detail
    public static final int TICKS_PER_SECOND = 20;
    // the number of milliseconds in a second - constant
    public static final int MILLISECONDS_PER_SECOND = 1000;
    // the number of milliseconds in a tick - assuming the server runs at a perfect tick rate
    public static final int MILLISECONDS_PER_TICK = MILLISECONDS_PER_SECOND / TICKS_PER_SECOND;

    private static long ticksToMillis(long ticks) {
        return ticks * MILLISECONDS_PER_TICK;
    }

    private final LPBungeePlugin plugin;

    private final Executor asyncExecutor;
    private final Set<SchedulerTask> tasks = ConcurrentHashMap.newKeySet();

    public BungeeSchedulerAdapter(LPBungeePlugin plugin) {
        this.plugin = plugin;
        this.asyncExecutor = r -> plugin.getProxy().getScheduler().runAsync(plugin, r);
    }

    private TaskScheduler scheduler() {
        return this.plugin.getProxy().getScheduler();
    }

    @Override
    public Executor async() {
        return this.asyncExecutor;
    }

    @Override
    public Executor sync() {
        return this.asyncExecutor;
    }

    @Override
    public void doAsync(Runnable runnable) {
        this.asyncExecutor.execute(runnable);
    }

    @Override
    public void doSync(Runnable runnable) {
        doAsync(runnable);
    }

    @Override
    public SchedulerTask asyncRepeating(Runnable runnable, long intervalTicks) {
        long millis = ticksToMillis(intervalTicks);
        SchedulerTask task = new BungeeSchedulerTask(scheduler().schedule(this.plugin, runnable, millis, millis, TimeUnit.MILLISECONDS));
        this.tasks.add(task);
        return task;
    }

    @Override
    public SchedulerTask syncRepeating(Runnable runnable, long intervalTicks) {
        return asyncRepeating(runnable, intervalTicks);
    }

    @Override
    public SchedulerTask asyncLater(Runnable runnable, long delayTicks) {
        return new BungeeSchedulerTask(scheduler().schedule(this.plugin, runnable, ticksToMillis(delayTicks), TimeUnit.MILLISECONDS));
    }

    @Override
    public SchedulerTask syncLater(Runnable runnable, long delayTicks) {
        return asyncLater(runnable, delayTicks);
    }

    @Override
    public void shutdown() {
        SafeIteration.iterate(this.tasks, SchedulerTask::cancel);
    }

    private static final class BungeeSchedulerTask implements SchedulerTask {
        private final ScheduledTask task;

        private BungeeSchedulerTask(ScheduledTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            this.task.cancel();
        }
    }

}
