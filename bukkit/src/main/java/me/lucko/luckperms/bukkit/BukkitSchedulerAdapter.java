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

package me.lucko.luckperms.bukkit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import me.lucko.luckperms.common.plugin.SchedulerAdapter;
import me.lucko.luckperms.common.plugin.SchedulerTask;
import me.lucko.luckperms.common.utils.SafeIteration;

import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

public class BukkitSchedulerAdapter implements SchedulerAdapter {
    private final LPBukkitPlugin plugin;

    private final ExecutorService asyncFallback;
    private final Executor asyncBukkit;
    private final Executor sync;
    private final Executor async;

    private boolean useFallback = true;

    private final Set<SchedulerTask> tasks = ConcurrentHashMap.newKeySet();

    public BukkitSchedulerAdapter(LPBukkitPlugin plugin) {
        this.plugin = plugin;

        this.sync = new SyncExecutor();
        this.asyncFallback = new FallbackAsyncExecutor();
        this.asyncBukkit = new BukkitAsyncExecutor();
        this.async = new AsyncExecutor();
    }

    private BukkitScheduler scheduler() {
        return this.plugin.getServer().getScheduler();
    }

    @Override
    public void doAsync(Runnable runnable) {
        async().execute(runnable);
    }

    @Override
    public void doSync(Runnable runnable) {
        sync().execute(runnable);
    }

    @Override
    public SchedulerTask asyncRepeating(Runnable runnable, long intervalTicks) {
        SchedulerTask task = new BukkitSchedulerTask(scheduler().runTaskTimerAsynchronously(this.plugin, runnable, intervalTicks, intervalTicks));
        this.tasks.add(task);
        return task;
    }

    @Override
    public SchedulerTask syncRepeating(Runnable runnable, long intervalTicks) {
        SchedulerTask task = new BukkitSchedulerTask(scheduler().runTaskTimer(this.plugin, runnable, intervalTicks, intervalTicks));
        this.tasks.add(task);
        return task;
    }

    @Override
    public SchedulerTask asyncLater(Runnable runnable, long delayTicks) {
        return new BukkitSchedulerTask(scheduler().runTaskLaterAsynchronously(this.plugin, runnable, delayTicks));
    }

    @Override
    public SchedulerTask syncLater(Runnable runnable, long delayTicks) {
        return new BukkitSchedulerTask(scheduler().runTaskLater(this.plugin, runnable, delayTicks));
    }

    @Override
    public void shutdown() {
        SafeIteration.iterate(this.tasks, SchedulerTask::cancel);

        // wait for executor
        this.asyncFallback.shutdown();
        try {
            this.asyncFallback.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public ExecutorService asyncFallback() {
        return this.asyncFallback;
    }

    public Executor asyncBukkit() {
        return this.asyncBukkit;
    }

    @Override
    public Executor sync() {
        return this.sync;
    }

    @Override
    public Executor async() {
        return this.async;
    }

    public void setUseFallback(boolean useFallback) {
        this.useFallback = useFallback;
    }

    private final class SyncExecutor implements Executor {
        @Override
        public void execute(@Nonnull Runnable runnable) {
            BukkitSchedulerAdapter.this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(BukkitSchedulerAdapter.this.plugin, runnable);
        }
    }

    private final class AsyncExecutor implements Executor {
        @Override
        public void execute(@Nonnull Runnable runnable) {
            if (BukkitSchedulerAdapter.this.useFallback || !BukkitSchedulerAdapter.this.plugin.isEnabled()) {
                BukkitSchedulerAdapter.this.asyncFallback.execute(runnable);
            } else {
                BukkitSchedulerAdapter.this.asyncBukkit.execute(runnable);
            }
        }
    }

    private final class BukkitAsyncExecutor implements Executor {
        @Override
        public void execute(@Nonnull Runnable runnable) {
            BukkitSchedulerAdapter.this.plugin.getServer().getScheduler().runTaskAsynchronously(BukkitSchedulerAdapter.this.plugin, runnable);
        }
    }

    private static final class FallbackAsyncExecutor extends ThreadPoolExecutor {
        private FallbackAsyncExecutor() {
            super(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadFactoryBuilder().setNameFormat("luckperms-fallback-%d").build());
        }
    }

    private static final class BukkitSchedulerTask implements SchedulerTask {
        private final BukkitTask task;

        private BukkitSchedulerTask(BukkitTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            this.task.cancel();
        }
    }

}
