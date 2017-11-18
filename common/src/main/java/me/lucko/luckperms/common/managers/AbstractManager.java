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

package me.lucko.luckperms.common.managers;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.common.references.Identifiable;

import java.util.Map;

/**
 * An abstract manager class
 *
 * @param <I> the class used to identify each object held in this manager
 * @param <T> the class this manager is "managing"
 */
public abstract class AbstractManager<I, T extends Identifiable<I>> implements Manager<I, T> {

    private final LoadingCache<I, T> objects = Caffeine.newBuilder()
            .build(new CacheLoader<I, T>() {
                @Override
                public T load(I i) {
                    return apply(i);
                }

                @Override
                public T reload(I i, T t) {
                    return t; // Never needs to be refreshed.
                }
            });

    @Override
    public Map<I, T> getAll() {
        return ImmutableMap.copyOf(objects.asMap());
    }

    @Override
    public T getOrMake(I id) {
        return objects.get(sanitizeIdentifier(id));
    }

    @Override
    public T getIfLoaded(I id) {
        return objects.getIfPresent(sanitizeIdentifier(id));
    }

    @Override
    public boolean isLoaded(I id) {
        return objects.asMap().containsKey(sanitizeIdentifier(id));
    }

    @Override
    public void unload(I id) {
        if (id != null) {
            objects.invalidate(sanitizeIdentifier(id));
        }
    }

    @Override
    public void unload(T t) {
        if (t != null) {
            unload(t.getId());
        }
    }

    @Override
    public void unloadAll() {
        objects.invalidateAll();
    }

    protected I sanitizeIdentifier(I i) {
        return i;
    }

}
