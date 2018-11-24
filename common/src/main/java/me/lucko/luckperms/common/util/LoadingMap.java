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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public interface LoadingMap<K, V> extends Map<K, V> {
    static <K, V> LoadingMap<K, V> of(Map<K, V> map, Function<K, V> function) {
        return new LoadingMapImpl<>(map, function);
    }

    static <K, V> LoadingMap<K, V> of(Function<K, V> function) {
        return of(new ConcurrentHashMap<>(), function);
    }
}

final class LoadingMapImpl<K, V> implements LoadingMap<K, V> {
    private final Map<K, V> map;
    private final Function<K, V> function;

    LoadingMapImpl(Map<K, V> map, Function<K, V> function) {
        this.map = map;
        this.function = function;
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        V value = this.map.get(key);
        if (value != null) {
            return value;
        }
        return this.map.computeIfAbsent((K) key, this.function);
    }

    @Override
    public V put(K key, V value) {
        return this.map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return this.map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> that) {
        this.map.putAll(that);
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public Set<K> keySet() {
        return this.map.keySet();
    }

    @Override
    public Collection<V> values() {
        return this.map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return this.map.entrySet();
    }
}