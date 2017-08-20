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

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

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
    @Accessors(fluent = true)
    private ExecutorService asyncLp;

    @Getter
    @Accessors(fluent = true)
    private Executor asyncBukkit;

    @Getter
    @Accessors(fluent = true)
    private Executor sync;

    @Getter
    @Setter
    private boolean useBukkitAsync = false;

    private Set<BukkitTask> tasks = ConcurrentHashMap.newKeySet();

    public LPBukkitScheduler(LPBukkitPlugin plugin) {
        this.plugin = plugin;

        this.asyncLp = Executors.newCachedThreadPool();
        this.asyncBukkit = r -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, r);
        this.sync = r -> plugin.getServer().getScheduler().runTask(plugin, r);
    }

    @Override
    public Executor async() {
        return useBukkitAsync ? asyncBukkit : asyncLp;
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
    public void asyncRepeating(Runnable runnable, long intervalTicks) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, runnable, intervalTicks, intervalTicks);
        tasks.add(task);
    }

    @Override
    public void syncRepeating(Runnable runnable, long intervalTicks) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, runnable, intervalTicks, intervalTicks);
        tasks.add(task);
    }

    @Override
    public void asyncLater(Runnable runnable, long delayTicks) {
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, runnable, delayTicks);
    }

    @Override
    public void syncLater(Runnable runnable, long delayTicks) {
        plugin.getServer().getScheduler().runTaskLater(plugin, runnable, delayTicks);
    }

    @Override
    public void shutdown() {
        tasks.forEach(BukkitTask::cancel);
        // wait for executor
        asyncLp.shutdown();
        try {
            asyncLp.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
