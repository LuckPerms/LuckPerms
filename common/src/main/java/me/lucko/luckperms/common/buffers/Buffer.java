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

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Thread-safe buffer utility. Holds a buffer of objects to be processed after they've been waiting in the buffer
 * for a given time. If the same object is pushed to the buffer again in that time, its wait time is reset.
 *
 * @param <T> the type of objects in the buffer
 * @param <R> the type of result produced by the final process
 */
public class Buffer<T, R> implements Runnable {
    private static final long DEFAULT_FLUSH_TIME = 1000; // 1 second

    public static <T, R> Buffer<T, R> of(Function<T, R> dequeueFunc) {
        return new Buffer<>(dequeueFunc);
    }

    private final ReentrantLock lock = new ReentrantLock();
    private final List<BufferedObject<T, R>> buffer = new LinkedList<>();
    private final Function<T, R> dequeueFunc;

    private Buffer(Function<T, R> dequeueFunc) {
        this.dequeueFunc = dequeueFunc;
    }

    public CompletableFuture<R> enqueue(@NonNull T t) {
        lock.lock();
        try {
            ListIterator<BufferedObject<T, R>> it = buffer.listIterator();

            BufferedObject<T, R> o = null;

            while (it.hasNext()) {
                BufferedObject<T, R> obj = it.next();

                if (obj.getObject().equals(t)) {
                    o = obj;
                    it.remove();
                    break;
                }
            }

            if (o == null) {
                o = new BufferedObject<>(System.currentTimeMillis(), t, new CompletableFuture<R>());
            } else {
                o.setBufferTime(System.currentTimeMillis());
            }

            buffer.add(o);
            return o.getFuture();
        } finally {
            lock.unlock();
        }
    }

    protected R dequeue(T t) {
        return dequeueFunc.apply(t);
    }

    public void flush(long flushTime) {
        long time = System.currentTimeMillis();

        lock.lock();
        try {
            ListIterator<BufferedObject<T, R>> it = buffer.listIterator(buffer.size());

            while (it.hasPrevious()) {
                BufferedObject<T, R> obj = it.previous();
                long bufferedTime = time - obj.getBufferTime();

                if (bufferedTime > flushTime) {

                    // Flush
                    R r = dequeue(obj.getObject());
                    obj.getFuture().complete(r);
                    it.remove();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        flush(DEFAULT_FLUSH_TIME);
    }

    @Getter
    @EqualsAndHashCode(of = "object")
    @AllArgsConstructor
    private static final class BufferedObject<T, R> {

        @Setter
        private long bufferTime;
        private final T object;
        private final CompletableFuture<R> future;

    }
}
