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

package me.lucko.luckperms.common.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Holds a buffer of objects to be processed after they've been in the buffer for a given time.
 * @param <T> the type of objects in the buffer
 * @param <R> the type of result produced by the final process
 */
public abstract class Buffer<T, R> implements Runnable {
    private static final long DEFAULT_FLUSH_TIME = 1000; // 1 second

    private final List<BufferedObject<T, R>> buffer = new LinkedList<>();

    public LPFuture<R> enqueue(@NonNull T t) {
        synchronized (buffer) {
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
                o = new BufferedObject<>(System.currentTimeMillis(), t, new AbstractFuture<R>());
            } else {
                o.setBufferTime(System.currentTimeMillis());
            }

            buffer.add(o);
            return o.getFuture();
        }
    }

    protected abstract R dequeue(T t);

    public void flush(long flushTime) {
        long time = System.currentTimeMillis();

        synchronized (buffer) {
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
        }
    }

    @Override
    public void run() {
        flush(DEFAULT_FLUSH_TIME);
    }

    @AllArgsConstructor
    private static class BufferedObject<T, R> {

        @Getter
        @Setter
        private long bufferTime;

        @Getter
        private final T object;

        @Getter
        private final AbstractFuture<R> future;

        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Buffer.BufferedObject)) return false;

            final BufferedObject other = (BufferedObject) o;
            return this.getObject() == null ? other.getObject() == null : this.getObject().equals(other.getObject());
        }

        public int hashCode() {
            return 59 + (this.getObject() == null ? 43 : this.getObject().hashCode());
        }
    }
}
