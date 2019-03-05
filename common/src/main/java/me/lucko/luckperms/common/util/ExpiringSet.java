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

package me.lucko.luckperms.common.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.ForwardingSet;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A simple expiring set implementation using Caffeine caches
 *
 * @param <E> element type
 */
public class ExpiringSet<E> extends ForwardingSet<E> {
    private final Cache<E, Boolean> cache;
    private final Set<E> setView;

    public ExpiringSet(long duration, TimeUnit unit) {
        this.cache = CaffeineFactory.newBuilder().expireAfterAccess(duration, unit).build();
        this.setView = this.cache.asMap().keySet();
    }

    @Override
    public boolean add(@NonNull E element) {
        this.cache.put(element, Boolean.TRUE);

        // we don't care about the return value
        return true;
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends E> collection) {
        for (E element : collection) {
            add(element);
        }

        // we don't care about the return value
        return true;
    }

    @Override
    public boolean remove(@NonNull Object key) {
        this.cache.invalidate(key);

        // we don't care about the return value
        return true;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> keys) {
        this.cache.invalidateAll(keys);

        // we don't care about the return value
        return true;
    }

    @Override
    protected Set<E> delegate() {
        return this.setView;
    }
}
