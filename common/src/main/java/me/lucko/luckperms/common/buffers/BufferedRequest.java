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

package me.lucko.luckperms.common.buffers;

import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;

import java.util.concurrent.CompletableFuture;
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
                } catch (IllegalStateException e) {
                    // ignore
                }
            }

            Processor<T> p = this.processor = new Processor<>(this::perform, this.bufferTime, this.unit, this.schedulerAdapter);
            return p.getFuture();
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

    private static class Processor<R> implements Runnable {
        private Supplier<R> supplier;

        private final long delay;
        private final TimeUnit unit;

        private SchedulerAdapter schedulerAdapter;
        private SchedulerTask task;

        private final Object[] mutex = new Object[0];
        private CompletableFuture<R> future = new CompletableFuture<>();
        private boolean usable = true;

        Processor(Supplier<R> supplier, long delay, TimeUnit unit, SchedulerAdapter schedulerAdapter) {
            this.supplier = supplier;
            this.delay = delay;
            this.unit = unit;
            this.schedulerAdapter = schedulerAdapter;

            rescheduleTask();
        }

        private void rescheduleTask() {
            synchronized (this.mutex) {
                if (!this.usable) {
                    throw new IllegalStateException("Processor not usable");
                }
                if (this.task != null) {
                    this.task.cancel();
                }
                this.task = this.schedulerAdapter.asyncLater(this, this.delay, this.unit);
            }
        }

        @Override
        public void run() {
            synchronized (this.mutex) {
                if (!this.usable) {
                    throw new IllegalStateException("Task has already ran");
                }
                this.usable = false;
            }

            // compute result
            try {
                R result = this.supplier.get();
                this.future.complete(result);
            } catch (Exception e) {
                this.future.completeExceptionally(e);
            }

            // allow supplier and future to be GCed
            this.task = null;
            this.supplier = null;
            this.future = null;
        }

        CompletableFuture<R> getFuture() {
            return this.future;
        }

        CompletableFuture<R> extendAndGetFuture() {
            rescheduleTask();
            return this.future;
        }

    }

}
