/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

import me.lucko.luckperms.common.plugin.LuckPermsScheduler;

import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class LPBungeeScheduler implements LuckPermsScheduler {
    private final LPBungeePlugin plugin;

    private Executor asyncExecutor;
    private Set<ScheduledTask> tasks = ConcurrentHashMap.newKeySet();

    public LPBungeeScheduler(LPBungeePlugin plugin) {
        this.plugin = plugin;
        this.asyncExecutor = r -> plugin.getProxy().getScheduler().runAsync(plugin, r);
    }

    @Override
    public Executor getAsyncExecutor() {
        return asyncExecutor;
    }

    @Override
    public Executor getSyncExecutor() {
        return asyncExecutor;
    }

    @Override
    public void doAsync(Runnable r) {
        asyncExecutor.execute(r);
    }

    @Override
    public void doSync(Runnable r) {
        doAsync(r);
    }

    @Override
    public void doAsyncRepeating(Runnable r, long interval) {
        long millis = interval * 50L; // convert from ticks to milliseconds
        ScheduledTask task = plugin.getProxy().getScheduler().schedule(plugin, r, millis, millis, TimeUnit.MILLISECONDS);
        tasks.add(task);
    }

    @Override
    public void doSyncRepeating(Runnable r, long interval) {
        doAsyncRepeating(r, interval);
    }

    @Override
    public void doAsyncLater(Runnable r, long delay) {
        long millis = delay * 50L; // convert from ticks to milliseconds
        plugin.getProxy().getScheduler().schedule(plugin, r, millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void doSyncLater(Runnable r, long delay) {
        doAsyncLater(r, delay);
    }

    @Override
    public void shutdown() {
        tasks.forEach(ScheduledTask::cancel);
    }
}
