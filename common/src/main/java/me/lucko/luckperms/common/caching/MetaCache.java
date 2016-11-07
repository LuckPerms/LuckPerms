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

package me.lucko.luckperms.common.caching;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.lucko.luckperms.api.caching.MetaData;

import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Holds a user's cached meta for a given context
 */
@NoArgsConstructor
public class MetaCache implements MetaData {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Getter
    private Map<String, String> meta = ImmutableMap.of();

    @Getter
    private SortedMap<Integer, String> prefixes = ImmutableSortedMap.of();

    @Getter
    private SortedMap<Integer, String> suffixes = ImmutableSortedMap.of();

    public void loadMeta(MetaHolder meta) {
        lock.writeLock().lock();
        try {
            this.meta = ImmutableMap.copyOf(meta.getMeta());
            this.prefixes = ImmutableSortedMap.copyOfSorted(meta.getPrefixes());
            this.suffixes = ImmutableSortedMap.copyOfSorted(meta.getSuffixes());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String getPrefix() {
        lock.readLock().lock();
        try {
            if (prefixes.isEmpty()) {
                return null;
            }

            return prefixes.get(prefixes.firstKey());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String getSuffix() {
        lock.readLock().lock();
        try {
            if (suffixes.isEmpty()) {
                return null;
            }

            return suffixes.get(suffixes.firstKey());
        } finally {
            lock.readLock().unlock();
        }
    }

}
