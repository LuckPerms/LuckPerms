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

package me.lucko.luckperms.nukkit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import me.lucko.luckperms.common.plugin.SchedulerAdapter;
import me.lucko.luckperms.common.plugin.SchedulerTask;
import me.lucko.luckperms.common.utils.SafeIteration;

import cn.nukkit.scheduler.ServerScheduler;
import cn.nukkit.scheduler.TaskHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

public class NukkitSchedulerAdapter implements SchedulerAdapter {
    private final LPNukkitPlugin plugin;

    private final ExecutorService asyncFallback;
    private final Executor asyncNukkit;
    private final Executor sync;
    private final Executor async;

    private boolean useFallback = true;

    private final Set<SchedulerTask> tasks = ConcurrentHashMap.newKeySet();

    public NukkitSchedulerAdapter(LPNukkitPlugin plugin) {
        this.plugin = plugin;

        this.sync = new SyncExecutor();
        this.asyncFallback = new FallbackAsyncExecutor();
        this.asyncNukkit = new NukkitAsyncExecutor();
        this.async = new AsyncExecutor();
    }

    private ServerScheduler scheduler() {
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
        SchedulerTask task = new NukkitSchedulerTask(scheduler().scheduleDelayedRepeatingTask(this.plugin, runnable, (int) intervalTicks, (int) intervalTicks, true));
        this.tasks.add(task);
        return task;
    }

    @Override
    public SchedulerTask syncRepeating(Runnable runnable, long intervalTicks) {
        SchedulerTask task = new NukkitSchedulerTask(scheduler().scheduleDelayedRepeatingTask(this.plugin, runnable, (int) intervalTicks, (int) intervalTicks, false));
        this.tasks.add(task);
        return task;
    }

    @Override
    public SchedulerTask asyncLater(Runnable runnable, long delayTicks) {
        return new NukkitSchedulerTask(scheduler().scheduleDelayedTask(this.plugin, runnable, (int) delayTicks, true));
    }

    @Override
    public SchedulerTask syncLater(Runnable runnable, long delayTicks) {
        return new NukkitSchedulerTask(scheduler().scheduleDelayedTask(this.plugin, runnable, (int) delayTicks, false));
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

    public Executor asyncNukkit() {
        return this.asyncNukkit;
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
            NukkitSchedulerAdapter.this.plugin.getServer().getScheduler().scheduleTask(NukkitSchedulerAdapter.this.plugin, runnable, false);
        }
    }

    private final class AsyncExecutor implements Executor {
        @Override
        public void execute(@Nonnull Runnable runnable) {
            if (NukkitSchedulerAdapter.this.useFallback || !NukkitSchedulerAdapter.this.plugin.isEnabled()) {
                NukkitSchedulerAdapter.this.asyncFallback.execute(runnable);
            } else {
                NukkitSchedulerAdapter.this.asyncNukkit.execute(runnable);
            }
        }
    }

    private final class NukkitAsyncExecutor implements Executor {
        @Override
        public void execute(@Nonnull Runnable runnable) {
            NukkitSchedulerAdapter.this.plugin.getServer().getScheduler().scheduleTask(NukkitSchedulerAdapter.this.plugin, runnable, true);
        }
    }

    private static final class FallbackAsyncExecutor extends ThreadPoolExecutor {
        private FallbackAsyncExecutor() {
            super(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadFactoryBuilder().setNameFormat("luckperms-fallback-%d").build());
        }
    }

    private static final class NukkitSchedulerTask implements SchedulerTask {
        private final TaskHandler task;

        private NukkitSchedulerTask(TaskHandler task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            this.task.cancel();
        }
    }

}
