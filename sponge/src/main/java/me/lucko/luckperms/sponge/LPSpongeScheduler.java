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

import me.lucko.luckperms.common.plugin.LuckPermsScheduler;

import org.spongepowered.api.scheduler.Task;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class LPSpongeScheduler implements LuckPermsScheduler {
    private final LPSpongePlugin plugin;
    private Set<Task> tasks = ConcurrentHashMap.newKeySet();

    public LPSpongeScheduler(LPSpongePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Executor getAsyncExecutor() {
        return plugin.getAsyncExecutorService();
    }

    @Override
    public Executor getSyncExecutor() {
        return plugin.getSyncExecutorService();
    }

    @Override
    public void doAsync(Runnable r) {
        plugin.getSpongeScheduler().createTaskBuilder().async().execute(r).submit(plugin);
    }

    @Override
    public void doSync(Runnable r) {
        plugin.getSpongeScheduler().createTaskBuilder().execute(r).submit(plugin);
    }

    @Override
    public void doAsyncRepeating(Runnable r, long interval) {
        Task task = plugin.getSpongeScheduler().createTaskBuilder().async().intervalTicks(interval).delayTicks(interval).execute(r).submit(plugin);
        tasks.add(task);
    }

    @Override
    public void doSyncRepeating(Runnable r, long interval) {
        Task task = plugin.getSpongeScheduler().createTaskBuilder().intervalTicks(interval).delayTicks(interval).execute(r).submit(plugin);
        tasks.add(task);
    }

    @Override
    public void doAsyncLater(Runnable r, long delay) {
        plugin.getSpongeScheduler().createTaskBuilder().async().delayTicks(delay).execute(r).submit(plugin);
    }

    @Override
    public void doSyncLater(Runnable r, long delay) {
        plugin.getSpongeScheduler().createTaskBuilder().delayTicks(delay).execute(r).submit(plugin);
    }

    @Override
    public void shutdown() {
        tasks.forEach(Task::cancel);
    }
}
