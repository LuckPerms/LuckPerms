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

package me.lucko.luckperms.krypton

import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask
import org.kryptonmc.krypton.api.scheduling.Scheduler
import org.kryptonmc.krypton.api.scheduling.Task
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class KryptonSchedulerAdapter(private val bootstrap: LPKryptonBootstrap, private val scheduler: Scheduler) : SchedulerAdapter {

    private val executor = Executor { scheduler.run(bootstrap) { it.run() } }
    private val tasks = mutableSetOf<Task>()

    override fun sync() = executor

    override fun async() = executor

    override fun asyncLater(task: Runnable, delay: Long, unit: TimeUnit): SchedulerTask {
        val scheduledTask = scheduler.schedule(bootstrap, delay, unit) { task.run() }
        tasks += scheduledTask
        return SchedulerTask { scheduledTask.cancel() }
    }

    override fun asyncRepeating(task: Runnable, interval: Long, unit: TimeUnit): SchedulerTask {
        val scheduledTask = scheduler.schedule(bootstrap, interval, interval, unit) { task.run() }
        tasks += scheduledTask
        return SchedulerTask { scheduledTask.cancel() }
    }

    override fun shutdownScheduler() = tasks.forEach { it.cancel() }

    override fun shutdownExecutor() {}
}