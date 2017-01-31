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

package me.lucko.luckperms.bukkit;

import lombok.Getter;
import lombok.Setter;

import me.lucko.luckperms.common.plugin.LuckPermsScheduler;

import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LPBukkitScheduler implements LuckPermsScheduler {
    private final LPBukkitPlugin plugin;

    @Getter
    private ExecutorService asyncLpExecutor;
    @Getter
    private Executor asyncBukkitExecutor;
    private Executor syncExecutor;

    @Getter
    @Setter
    private boolean useBukkitAsync = false;

    private Set<BukkitTask> tasks = ConcurrentHashMap.newKeySet();

    public LPBukkitScheduler(LPBukkitPlugin plugin) {
        this.plugin = plugin;

        this.asyncLpExecutor = Executors.newCachedThreadPool();
        this.asyncBukkitExecutor = r -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, r);
        this.syncExecutor = r -> plugin.getServer().getScheduler().runTask(plugin, r);
    }

    @Override
    public Executor getAsyncExecutor() {
        return useBukkitAsync ? asyncBukkitExecutor : asyncLpExecutor;
    }

    @Override
    public Executor getSyncExecutor() {
        return syncExecutor;
    }

    @Override
    public void doAsync(Runnable r) {
        getAsyncExecutor().execute(r);
    }

    @Override
    public void doSync(Runnable r) {
        getSyncExecutor().execute(r);
    }

    @Override
    public void doAsyncRepeating(Runnable r, long interval) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, r, interval, interval);
        tasks.add(task);
    }

    @Override
    public void doSyncRepeating(Runnable r, long interval) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, r, interval, interval);
        tasks.add(task);
    }

    @Override
    public void doAsyncLater(Runnable r, long delay) {
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, r, delay);
    }

    @Override
    public void doSyncLater(Runnable r, long delay) {
        plugin.getServer().getScheduler().runTaskLater(plugin, r, delay);
    }

    @Override
    public void shutdown() {
        tasks.forEach(BukkitTask::cancel);
        // wait for executor
        asyncLpExecutor.shutdown();
        try {
            asyncLpExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
