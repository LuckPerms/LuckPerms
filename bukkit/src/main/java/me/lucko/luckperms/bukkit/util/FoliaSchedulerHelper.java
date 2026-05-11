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

package me.lucko.luckperms.bukkit.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Reflection-based scheduling helper for the Folia region-aware scheduler API.
 * Uses reflection to avoid compile-time dependency on modern Paper API.
 */
public final class FoliaSchedulerHelper {
    private static final Method GET_GLOBAL_REGION_SCHEDULER;
    private static final Method GET_ASYNC_SCHEDULER;
    private static final Method GET_ENTITY_SCHEDULER;

    private static final Method GLOBAL_EXECUTE;
    private static final Method GLOBAL_RUN_DELAYED;
    private static final Method GLOBAL_RUN_AT_FIXED_RATE;

    private static final Method ASYNC_RUN_NOW;

    private static final Method ENTITY_RUN;
    private static final Method ENTITY_RUN_DELAYED;

    static {
        try {
            Class<?> serverClass = Bukkit.getServer().getClass();

            GET_GLOBAL_REGION_SCHEDULER = serverClass.getMethod("getGlobalRegionScheduler");
            GET_ASYNC_SCHEDULER = serverClass.getMethod("getAsyncScheduler");
            GET_ENTITY_SCHEDULER = Entity.class.getMethod("getScheduler");

            Class<?> globalSchedulerClass = GET_GLOBAL_REGION_SCHEDULER.getReturnType();
            GLOBAL_EXECUTE = globalSchedulerClass.getMethod("execute", Plugin.class, Runnable.class);
            GLOBAL_RUN_DELAYED = globalSchedulerClass.getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
            GLOBAL_RUN_AT_FIXED_RATE = globalSchedulerClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);

            Class<?> asyncSchedulerClass = GET_ASYNC_SCHEDULER.getReturnType();
            ASYNC_RUN_NOW = asyncSchedulerClass.getMethod("runNow", Plugin.class, Consumer.class);

            Class<?> entitySchedulerClass = GET_ENTITY_SCHEDULER.getReturnType();
            ENTITY_RUN = entitySchedulerClass.getMethod("run", Plugin.class, Consumer.class, Runnable.class);
            ENTITY_RUN_DELAYED = entitySchedulerClass.getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private FoliaSchedulerHelper() {
    }

    public static void executeOnGlobalRegion(Plugin plugin, Runnable task) {
        try {
            Object scheduler = GET_GLOBAL_REGION_SCHEDULER.invoke(Bukkit.getServer());
            GLOBAL_EXECUTE.invoke(scheduler, plugin, task);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void runOnGlobalRegionDelayed(Plugin plugin, Runnable task, long delayTicks) {
        try {
            Object scheduler = GET_GLOBAL_REGION_SCHEDULER.invoke(Bukkit.getServer());
            Consumer<Object> consumer = t -> task.run();
            GLOBAL_RUN_DELAYED.invoke(scheduler, plugin, consumer, delayTicks);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void runOnGlobalRegionAtFixedRate(Plugin plugin, BooleanSupplier task, long initialDelayTicks, long periodTicks) {
        try {
            Object scheduler = GET_GLOBAL_REGION_SCHEDULER.invoke(Bukkit.getServer());
            Consumer<Object> consumer = scheduledTask -> {
                if (task.getAsBoolean()) {
                    try {
                        scheduledTask.getClass().getMethod("cancel").invoke(scheduledTask);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            GLOBAL_RUN_AT_FIXED_RATE.invoke(scheduler, plugin, consumer, initialDelayTicks, periodTicks);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void executeOnEntity(Entity entity, Plugin plugin, Runnable task) {
        try {
            Object scheduler = GET_ENTITY_SCHEDULER.invoke(entity);
            Consumer<Object> consumer = t -> task.run();
            ENTITY_RUN.invoke(scheduler, plugin, consumer, (Runnable) null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void runOnEntityDelayed(Entity entity, Plugin plugin, Runnable task, long delayTicks) {
        try {
            Object scheduler = GET_ENTITY_SCHEDULER.invoke(entity);
            Consumer<Object> consumer = t -> task.run();
            ENTITY_RUN_DELAYED.invoke(scheduler, plugin, consumer, (Runnable) null, delayTicks);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void executeAsync(Plugin plugin, Runnable task) {
        try {
            Object scheduler = GET_ASYNC_SCHEDULER.invoke(Bukkit.getServer());
            Consumer<Object> consumer = t -> task.run();
            ASYNC_RUN_NOW.invoke(scheduler, plugin, consumer);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
