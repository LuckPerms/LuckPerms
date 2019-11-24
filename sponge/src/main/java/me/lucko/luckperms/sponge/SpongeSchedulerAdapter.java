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

import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;
import me.lucko.luckperms.common.util.Iterators;

import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.scheduler.Task;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class SpongeSchedulerAdapter implements SchedulerAdapter {
    private final LPSpongeBootstrap bootstrap;
    
    private final Scheduler scheduler;
    private final SpongeExecutorService sync;
    private final SpongeExecutorService async;
    
    private final Set<Task> tasks = Collections.newSetFromMap(new WeakHashMap<>());

    public SpongeSchedulerAdapter(LPSpongeBootstrap bootstrap, Scheduler scheduler, SpongeExecutorService sync, SpongeExecutorService async) {
        this.bootstrap = bootstrap;
        this.scheduler = scheduler;
        this.sync = sync;
        this.async = async;
    }

    @Override
    public Executor async() {
        return this.async;
    }

    @Override
    public Executor sync() {
        return this.sync;
    }

    @Override
    public void executeAsync(Runnable runnable) {
        this.scheduler.createTaskBuilder().async().execute(runnable).submit(this.bootstrap);
    }

    @Override
    public void executeSync(Runnable runnable) {
        this.scheduler.createTaskBuilder().execute(runnable).submit(this.bootstrap);
    }

    @Override
    public SchedulerTask asyncLater(Runnable task, long delay, TimeUnit unit) {
        Task t = this.scheduler.createTaskBuilder()
                .async()
                .delay(delay, unit)
                .execute(task)
                .submit(this.bootstrap);

        this.tasks.add(t);
        return t::cancel;
    }

    @Override
    public SchedulerTask asyncRepeating(Runnable task, long interval, TimeUnit unit) {
        Task t = this.scheduler.createTaskBuilder()
                .async()
                .interval(interval, unit)
                .delay(interval, unit)
                .execute(task)
                .submit(this.bootstrap);

        this.tasks.add(t);
        return t::cancel;
    }

    @Override
    public void shutdown() {
        Iterators.tryIterate(this.tasks, Task::cancel);
    }

}
