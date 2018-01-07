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

import java.lang.ref.WeakReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Thread-safe request buffer.
 *
 * Waits for the buffer time to pass before performing the operation. If the task is called again in that time, the
 * buffer time is reset.
 *
 * @param <T> the return type
 */
public abstract class BufferedRequest<T> {
    private final long bufferTimeMillis;
    private final long sleepInterval;
    private final Executor executor;

    private WeakReference<Processor<T>> processor = null;
    private final ReentrantLock lock = new ReentrantLock();

    public BufferedRequest(long bufferTimeMillis, long sleepInterval, Executor executor) {
        this.bufferTimeMillis = bufferTimeMillis;
        this.sleepInterval = sleepInterval;
        this.executor = executor;
    }

    public CompletableFuture<T> request() {
        this.lock.lock();
        try {
            if (this.processor != null) {
                Processor<T> p = this.processor.get();
                if (p != null && p.isUsable()) {
                    return p.getAndExtend();
                }
            }

            Processor<T> p = new Processor<>(this.bufferTimeMillis, this.sleepInterval, this::perform);
            this.executor.execute(p);
            this.processor = new WeakReference<>(p);
            return p.get();

        } finally {
            this.lock.unlock();
        }
    }

    public T requestDirectly() {
        return perform();
    }

    protected abstract T perform();

    private static class Processor<R> implements Runnable {
        private final long delayMillis;
        private final long sleepMillis;
        private final Supplier<R> supplier;
        private final ReentrantLock lock = new ReentrantLock();
        private final CompletableFuture<R> future = new CompletableFuture<>();
        private boolean usable = true;
        private long executionTime;

        public Processor(long delayMillis, long sleepMillis, Supplier<R> supplier) {
            this.delayMillis = delayMillis;
            this.sleepMillis = sleepMillis;
            this.supplier = supplier;
        }

        @Override
        public void run() {
            this.lock.lock();
            try {
                this.executionTime = System.currentTimeMillis() + this.delayMillis;
            } finally {
                this.lock.unlock();
            }

            while (true) {
                this.lock.lock();
                try {
                    if (System.currentTimeMillis() > this.executionTime) {
                        this.usable = false;
                        break;
                    }

                } finally {
                    this.lock.unlock();
                }

                try {
                    Thread.sleep(this.sleepMillis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            R result = this.supplier.get();
            this.future.complete(result);
        }

        public CompletableFuture<R> get() {
            return this.future;
        }

        public CompletableFuture<R> getAndExtend() {
            this.lock.lock();
            try {
                this.executionTime = System.currentTimeMillis() + this.delayMillis;
            } finally {
                this.lock.unlock();
            }

            return this.future;
        }

        public boolean isUsable() {
            return this.usable;
        }
    }

}
