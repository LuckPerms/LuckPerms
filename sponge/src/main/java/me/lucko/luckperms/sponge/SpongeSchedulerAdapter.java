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

package me.lucko.luckperms.sponge;

import me.lucko.luckperms.common.plugin.SchedulerAdapter;
import me.lucko.luckperms.common.plugin.SchedulerTask;
import me.lucko.luckperms.common.utils.SafeIterator;

import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.Task;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class SpongeSchedulerAdapter implements SchedulerAdapter {
    private final LPSpongePlugin plugin;
    private final Set<SchedulerTask> tasks = ConcurrentHashMap.newKeySet();

    public SpongeSchedulerAdapter(LPSpongePlugin plugin) {
        this.plugin = plugin;
    }

    private Scheduler scheduler() {
        return this.plugin.getSpongeScheduler();
    }

    @Override
    public Executor async() {
        return this.plugin.getAsyncExecutorService();
    }

    @Override
    public Executor sync() {
        return this.plugin.getSyncExecutorService();
    }

    @Override
    public void doAsync(Runnable runnable) {
        this.plugin.getSpongeScheduler().createTaskBuilder().async().execute(runnable).submit(this.plugin);
    }

    @Override
    public void doSync(Runnable runnable) {
        this.plugin.getSpongeScheduler().createTaskBuilder().execute(runnable).submit(this.plugin);
    }

    @Override
    public SchedulerTask asyncRepeating(Runnable runnable, long intervalTicks) {
        Task task = scheduler().createTaskBuilder()
                .async()
                .intervalTicks(intervalTicks)
                .delayTicks(intervalTicks)
                .execute(runnable)
                .submit(this.plugin);

        SchedulerTask wrapped = new SpongeSchedulerTask(task);
        this.tasks.add(wrapped);
        return wrapped;
    }

    @Override
    public SchedulerTask syncRepeating(Runnable runnable, long intervalTicks) {
        Task task = scheduler().createTaskBuilder()
                .intervalTicks(intervalTicks)
                .delayTicks(intervalTicks)
                .execute(runnable)
                .submit(this.plugin);

        SchedulerTask wrapped = new SpongeSchedulerTask(task);
        this.tasks.add(wrapped);
        return wrapped;
    }

    @Override
    public SchedulerTask asyncLater(Runnable runnable, long delayTicks) {
        Task task = scheduler().createTaskBuilder()
                .async()
                .delayTicks(delayTicks)
                .execute(runnable)
                .submit(this.plugin);

        SchedulerTask wrapped = new SpongeSchedulerTask(task);
        this.tasks.add(wrapped);
        return wrapped;
    }

    @Override
    public SchedulerTask syncLater(Runnable runnable, long delayTicks) {
        Task task = scheduler().createTaskBuilder()
                .delayTicks(delayTicks)
                .execute(runnable)
                .submit(this.plugin);

        SchedulerTask wrapped = new SpongeSchedulerTask(task);
        this.tasks.add(wrapped);
        return wrapped;
    }

    @Override
    public void shutdown() {
        SafeIterator.iterate(this.tasks, SchedulerTask::cancel);
    }

    private static final class SpongeSchedulerTask implements SchedulerTask {
        private final Task task;

        private SpongeSchedulerTask(Task task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            this.task.cancel();
        }
    }
}
