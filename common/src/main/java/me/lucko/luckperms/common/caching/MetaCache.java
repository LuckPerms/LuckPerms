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

package me.lucko.luckperms.common.caching;

import lombok.Getter;
import lombok.NoArgsConstructor;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

import me.lucko.luckperms.api.caching.MetaData;
import me.lucko.luckperms.common.caching.stacking.MetaStack;
import me.lucko.luckperms.common.caching.stacking.NoopMetaStack;

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

    @Getter
    private MetaStack prefixStack = NoopMetaStack.INSTANCE;

    @Getter
    private MetaStack suffixStack = NoopMetaStack.INSTANCE;

    public void loadMeta(MetaAccumulator meta) {
        lock.writeLock().lock();
        try {
            this.meta = ImmutableMap.copyOf(meta.getMeta());
            this.prefixes = ImmutableSortedMap.copyOfSorted(meta.getPrefixes());
            this.suffixes = ImmutableSortedMap.copyOfSorted(meta.getSuffixes());
            this.prefixStack = meta.getPrefixStack();
            this.suffixStack = meta.getSuffixStack();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String getPrefix() {
        lock.readLock().lock();
        try {
            return prefixStack.toFormattedString();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String getSuffix() {
        lock.readLock().lock();
        try {
            return suffixStack.toFormattedString();
        } finally {
            lock.readLock().unlock();
        }
    }

}
