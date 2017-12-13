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

import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class BungeeSchedulerAdapter implements SchedulerAdapter {
    private final LPBungeePlugin plugin;

    private final Executor asyncExecutor;
    private final Set<ScheduledTask> tasks = ConcurrentHashMap.newKeySet();

    public BungeeSchedulerAdapter(LPBungeePlugin plugin) {
        this.plugin = plugin;
        this.asyncExecutor = r -> plugin.getProxy().getScheduler().runAsync(plugin, r);
    }

    @Override
    public Executor async() {
        return asyncExecutor;
    }

    @Override
    public Executor sync() {
        return asyncExecutor;
    }

    @Override
    public void doAsync(Runnable runnable) {
        asyncExecutor.execute(runnable);
    }

    @Override
    public void doSync(Runnable runnable) {
        doAsync(runnable);
    }

    @Override
    public void asyncRepeating(Runnable runnable, long intervalTicks) {
        long millis = intervalTicks * 50L; // convert from ticks to milliseconds
        ScheduledTask task = plugin.getProxy().getScheduler().schedule(plugin, runnable, millis, millis, TimeUnit.MILLISECONDS);
        tasks.add(task);
    }

    @Override
    public void syncRepeating(Runnable runnable, long intervalTicks) {
        asyncRepeating(runnable, intervalTicks);
    }

    @Override
    public void asyncLater(Runnable runnable, long delayTicks) {
        long millis = delayTicks * 50L; // convert from ticks to milliseconds
        plugin.getProxy().getScheduler().schedule(plugin, runnable, millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void syncLater(Runnable runnable, long delayTicks) {
        asyncLater(runnable, delayTicks);
    }

    @Override
    public void shutdown() {
        tasks.forEach(ScheduledTask::cancel);
    }
}
