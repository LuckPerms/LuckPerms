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

package me.lucko.luckperms.waterdog;

import dev.waterdog.waterdogpe.scheduler.TaskHandler;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;
import me.lucko.luckperms.common.util.Iterators;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class WaterdogSchedulerAdapter implements SchedulerAdapter {

    private final LPWaterdogBootstrap bootstrap;

    private final Set<TaskHandler<Runnable>> tasks = Collections.newSetFromMap(new WeakHashMap<>());

    private final Executor asyncExecutor;
    private final Executor syncExecutor;

    public WaterdogSchedulerAdapter(LPWaterdogBootstrap bootstrap) {
        this.bootstrap = bootstrap;
        this.syncExecutor = r -> bootstrap.getProxy().getScheduler().scheduleTask(r, false);
        this.asyncExecutor = r -> bootstrap.getProxy().getScheduler().scheduleTask(r, true);
    }

    @Override
    public Executor async() {
        return this.asyncExecutor;
    }

    @Override
    public Executor sync() {
        return this.syncExecutor;
    }

    @Override
    public SchedulerTask asyncLater(Runnable task, long delay, TimeUnit unit) {
        int delayInTicks = (int) (unit.toMillis(delay) / 50);
        TaskHandler<Runnable> t = this.bootstrap.getProxy().getScheduler().scheduleDelayed(task, delayInTicks, true);
        this.tasks.add(t);
        return t::cancel;
    }

    @Override
    public SchedulerTask asyncRepeating(Runnable task, long interval, TimeUnit unit) {
        int intervalInTicks = (int) (unit.toMillis(interval) / 50);
        TaskHandler<Runnable> t = this.bootstrap.getProxy().getScheduler().scheduleDelayed(task, intervalInTicks, true);
        this.tasks.add(t);
        return t::cancel;
    }

    @Override
    public void shutdownScheduler() {
        Iterators.tryIterate(this.tasks, TaskHandler::cancel);
    }

    @Override
    public void shutdownExecutor() {
        // do nothing
    }
}
