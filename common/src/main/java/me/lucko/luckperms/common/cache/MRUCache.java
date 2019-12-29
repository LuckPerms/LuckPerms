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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of a most-recently-used cache with a mod counter, to prevent race conditions
 * occurring when the cache is cleared whilst a calculation is taking place.
 *
 * @param <T> the cached type
 */
public abstract class MRUCache<T> {
    private volatile T recent;
    private final AtomicInteger modCount = new AtomicInteger();

    protected int modCount() {
        return this.modCount.get();
    }

    protected T getRecent() {
        return this.recent;
    }

    protected void offerRecent(int validAt, T offer) {
        if (validAt == this.modCount.get()) {
            this.recent = offer;
        }
    }

    protected void clearRecent() {
        this.recent = null;
        this.modCount.incrementAndGet();
    }
}
