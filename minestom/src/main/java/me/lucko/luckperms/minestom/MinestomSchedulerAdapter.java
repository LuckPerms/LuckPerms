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

package me.lucko.luckperms.minestom;

import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Task;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class MinestomSchedulerAdapter implements SchedulerAdapter {
    private final MinestomExecutor asyncExecutor = new MinestomExecutor(true);
    private final MinestomExecutor syncExecutor = new MinestomExecutor(false);
    private final Set<Task> tasks = Collections.newSetFromMap(new WeakHashMap<>());

    @Override
    public Executor async() {
        return asyncExecutor;
    }

    @Override
    public Executor sync() {
        return syncExecutor;
    }

    @Override
    public SchedulerTask asyncLater(Runnable task, long delay, TimeUnit unit) {
        Task delayedTask = MinecraftServer.getSchedulerManager().buildTask(task).delay(delay, unit.toChronoUnit()).schedule();
        this.tasks.add(delayedTask);
        return delayedTask::cancel;
    }

    @Override
    public SchedulerTask asyncRepeating(Runnable task, long interval, TimeUnit unit) {
        Task repeatingTask = MinecraftServer.getSchedulerManager().buildTask(task).repeat(interval, unit.toChronoUnit()).schedule();
        this.tasks.add(repeatingTask);
        return repeatingTask::cancel;
    }

    @Override
    public void shutdownScheduler() {
        this.tasks.forEach(Task::cancel);
    }

    @Override
    public void shutdownExecutor() {
        this.asyncExecutor.cancel();
        this.syncExecutor.cancel();
    }

    private static class MinestomExecutor implements Executor {
        private final Set<Task> tasks = Collections.newSetFromMap(new WeakHashMap<>());
        private final boolean async;

        public MinestomExecutor(boolean async) {
            this.async = async;
        }

        @Override
        public void execute(@NotNull Runnable command) {
            Task.Builder builder = MinecraftServer.getSchedulerManager().buildTask(command);
            if (this.async) {
                builder.executionType(ExecutionType.ASYNC);
            } else {
                builder.executionType(ExecutionType.SYNC);
            }
            Task task = builder.schedule();
            this.tasks.add(task);
        }

        private void cancel() {
            this.tasks.forEach(Task::cancel);
        }
    }
}
