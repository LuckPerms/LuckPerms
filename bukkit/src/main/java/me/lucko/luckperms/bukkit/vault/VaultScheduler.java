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

package me.lucko.luckperms.bukkit.vault;

import me.lucko.luckperms.bukkit.LPBukkitPlugin;

import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Sequential executor for Vault modifications
 */
public class VaultScheduler implements Runnable, Executor {

    private BukkitTask task = null;
    private final List<Runnable> tasks = new ArrayList<>();

    public VaultScheduler(LPBukkitPlugin plugin) {
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this, 1L, 1L);
    }

    @Override
    public void execute(Runnable r) {
        synchronized (tasks) {
            tasks.add(r);
        }
    }

    @Override
    public void run() {
        List<Runnable> toRun;
        synchronized (tasks) {
            if (tasks.isEmpty()) {
                return;
            }

            toRun = new ArrayList<>();
            toRun.addAll(tasks);
            tasks.clear();
        }

        toRun.forEach(Runnable::run);
    }

    public void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
