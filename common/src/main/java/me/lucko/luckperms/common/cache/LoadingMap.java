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

import com.google.common.collect.ForwardingMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class LoadingMap<K, V> extends ForwardingMap<K, V> implements Map<K, V> {
    public static <K, V> LoadingMap<K, V> of(Map<K, V> map, Function<K, V> function) {
        return new LoadingMap<>(map, function);
    }

    public static <K, V> LoadingMap<K, V> of(Function<K, V> function) {
        return of(new ConcurrentHashMap<>(), function);
    }

    private final Map<K, V> map;
    private final Function<K, V> function;

    private LoadingMap(Map<K, V> map, Function<K, V> function) {
        this.map = map;
        this.function = function;
    }

    @Override
    protected Map<K, V> delegate() {
        return this.map;
    }

    public V getIfPresent(K key) {
        return this.map.get(key);
    }

    @Override
    public V get(Object key) {
        V value = this.map.get(key);
        if (value != null) {
            return value;
        }
        //noinspection unchecked
        return this.map.computeIfAbsent((K) key, this.function);
    }
}