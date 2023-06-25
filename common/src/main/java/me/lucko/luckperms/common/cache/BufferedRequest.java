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

package me.lucko.luckperms.common.cache;

import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Thread-safe request buffer.
 *
 * Waits for the buffer time to pass before performing the operation.
 * If the request is called again in that time, the buffer time is reset.
 *
 * @param <T> the return type
 */
public abstract class BufferedRequest<T> {

    /** The buffer time */
    private final long bufferTime;
    private final TimeUnit unit;
    private final SchedulerAdapter schedulerAdapter;

    /** The active processor task, if present */
    private Processor<T> processor = null;

    /** Mutex to guard processor */
    private final Object[] mutex = new Object[0];

    /**
     * Creates a new buffer with the given timeout millis
     *
     * @param bufferTime the timeout
     * @param unit the unit of the timeout
     */
    public BufferedRequest(long bufferTime, TimeUnit unit, SchedulerAdapter schedulerAdapter) {
        this.bufferTime = bufferTime;
        this.unit = unit;
        this.schedulerAdapter = schedulerAdapter;
    }

    /**
     * Makes a request to the buffer
     *
     * @return the future
     */
    public CompletableFuture<T> request() {
        synchronized (this.mutex) {
            if (this.processor != null) {
                try {
                    return this.processor.extendAndGetFuture();
                } catch (ProcessorAlreadyRanException e) {
                    // ignore
                }
            }

            Processor<T> p = this.processor = new Processor<>(this::perform, this.bufferTime, this.unit, this.schedulerAdapter);
            return p.getFuture();
        }
    }

    /**
     * Gets if the request buffer has been enqueued
     *
     * @return if the buffer is enqueued
     */
    public boolean isEnqueued() {
        synchronized (this.mutex) {
            return this.processor != null;
        }
    }

    /**
     * Requests the value, bypassing the buffer
     *
     * @return the value
     */
    public T requestDirectly() {
        return perform();
    }

    /**
     * Performs the buffered task
     *
     * @return the result
     */
    protected abstract T perform();

    private static final class Processor<R> {
        private final Supplier<R> supplier;

        private final long delay;
        private final TimeUnit unit;

        private final SchedulerAdapter schedulerAdapter;

        private final Object[] mutex = new Object[0];
        private final CompletableFuture<R> future = new CompletableFuture<>();
        private boolean usable = true;

        private SchedulerTask scheduledTask;
        private CompletionTask boundTask = null;

        Processor(Supplier<R> supplier, long delay, TimeUnit unit, SchedulerAdapter schedulerAdapter) {
            this.supplier = supplier;
            this.delay = delay;
            this.unit = unit;
            this.schedulerAdapter = schedulerAdapter;

            scheduleTask();
        }

        private void rescheduleTask() throws ProcessorAlreadyRanException {
            synchronized (this.mutex) {
                if (!this.usable) {
                    throw new ProcessorAlreadyRanException();
                }
                if (this.scheduledTask != null) {
                    this.scheduledTask.cancel();
                }
                scheduleTask();
            }
        }

        private void scheduleTask() {
            this.boundTask = new CompletionTask();
            try {
                this.scheduledTask = this.schedulerAdapter.asyncLater(this.boundTask, this.delay, this.unit);
            } catch (RejectedExecutionException e) {
                // If we can't schedule the completion in the future, just do it now.
                this.boundTask.run();
            }
        }

        CompletableFuture<R> getFuture() {
            return this.future;
        }

        CompletableFuture<R> extendAndGetFuture() throws ProcessorAlreadyRanException {
            rescheduleTask();
            return this.future;
        }

        private final class CompletionTask implements Runnable {
            @Override
            public void run() {
                synchronized (Processor.this.mutex) {
                    if (!Processor.this.usable) {
                        throw new IllegalStateException("Task has already ran");
                    }

                    // check that we're still the bound task.
                    // prevents a race condition between #run and #rescheduleTask
                    if (Processor.this.boundTask != this) {
                        return;
                    }

                    Processor.this.usable = false;
                }

                // compute result
                try {
                    R result = Processor.this.supplier.get();
                    Processor.this.future.complete(result);
                } catch (Exception e) {
                    new RuntimeException("Processor " + Processor.this.supplier + " threw an exception whilst computing a result", e).printStackTrace();
                    Processor.this.future.completeExceptionally(e);
                }
            }
        }
    }

    private static final class ProcessorAlreadyRanException extends Exception {

    }

}
