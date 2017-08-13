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

import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe caching utility
 *
 * @param <T> the type being stored
 */
@RequiredArgsConstructor
public abstract class Cache<T> {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private T cached = null;

    protected abstract T supply();

    public final T get() {
        // try to just read from the cached value
        lock.readLock().lock();
        try {
            if (cached != null) {
                return cached;
            }
        } finally {
            // we have to release the read lock, as it is not possible
            // to acquire the write lock whilst holding a read lock
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            // Since the lock was unlocked momentarily, we need
            // to check again for a cached value
            if (cached != null) {
                return cached;
            }

            // call the supplier and set the cached value
            cached = supply();
            return cached;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public final Optional<T> getIfPresent() {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(cached);
        } finally {
            lock.readLock().unlock();
        }
    }

    public final void invalidate() {
        lock.writeLock().lock();
        try {
            cached = null;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
