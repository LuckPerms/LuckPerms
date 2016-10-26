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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Map;
import java.util.function.Function;

/**
 * An abstract manager class
 * @param <I> the class used to identify each object held in this manager
 * @param <T> the class this manager is "managing"
 */
public abstract class AbstractManager<I, T extends Identifiable<I>> implements Function<I, T> {

    private final LoadingCache<I, T> objects = CacheBuilder.newBuilder()
            .build(new CacheLoader<I, T>() {
                @Override
                public T load(I i) {
                    return apply(i);
                }

                @Override
                public ListenableFuture<T> reload(I i, T t) {
                    return Futures.immediateFuture(t); // Never needs to be refreshed.
                }
            });

    public final Map<I, T> getAll() {
        return ImmutableMap.copyOf(objects.asMap());
    }

    /**
     * Get an object by id
     * @param id The id to search by
     * @return a {@link T} object if the object is loaded or makes and returns a new object
     */
    public final T getOrMake(I id) {
        return objects.getUnchecked(id);
    }

    /**
     * Get an object by id
     * @param id The id to search by
     * @return a {@link T} object if the object is loaded, returns null if the object is not loaded
     */
    public final T get(I id) {
        return objects.getIfPresent(id);
    }

    /**
     * Check to see if a object is loaded or not
     * @param id The id of the object
     * @return true if the object is loaded
     */
    public final boolean isLoaded(I id) {
        return objects.asMap().containsKey(id);
    }

    /**
     * Removes and unloads the object from the manager
     * @param t The object to unload
     */
    public final void unload(T t) {
        if (t != null) {
            objects.invalidate(t.getId());
        }
    }

    /**
     * Unloads all objects from the manager
     */
    public final void unloadAll() {
        objects.invalidateAll();
    }

}
