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

package me.lucko.luckperms.utils;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

/**
 * An abstract manager class
 * @param <I> the class used to identify each object held in this manager
 * @param <T> the class this manager is "managing"
 */
public abstract class AbstractManager<I, T extends Identifiable<I>> {
    private final Map<I, T> objects = new HashMap<>();

    public final Map<I, T> getAll() {
        synchronized (objects) {
            return ImmutableMap.copyOf(objects);
        }
    }

    /**
     * Get an object by id
     * @param id The id to search by
     * @return a {@link T} object if the object is loaded, returns null if the object is not loaded
     */
    public final T get(I id) {
        synchronized (objects) {
            return objects.get(id);
        }
    }

    /**
     * Add a object to the loaded objects map
     * @param t The object to add
     */
    public final void set(T t) {
        preSet(t);
        synchronized (objects) {
            objects.put(t.getId(), t);
        }
    }

    protected void preSet(T t) {

    }

    /**
     * Updates (or sets if the object wasn't already loaded) an object in the objects map
     * @param t The object to update or set
     */
    public final void updateOrSet(T t) {
        synchronized (objects) {
            if (!isLoaded(t.getId())) {
                // The object isn't already loaded
                set(t);
            } else {
                copy(t, objects.get(t.getId()));
            }
        }
    }

    public abstract void copy(T from, T to);

    /**
     * Check to see if a object is loaded or not
     * @param id The id of the object
     * @return true if the object is loaded
     */
    public final boolean isLoaded(I id) {
        synchronized (objects) {
            return objects.containsKey(id);
        }
    }

    /**
     * Removes and unloads the object from the manager
     * @param t The object to unload
     */
    public final void unload(T t) {
        if (t != null) {
            preUnload(t);
            synchronized (objects) {
                objects.remove(t.getId());
            }
        }
    }

    protected void preUnload(T t) {

    }

    /**
     * Unloads all objects from the manager
     */
    public final void unloadAll() {
        synchronized (objects) {
            objects.clear();
        }
    }

    /**
     * Makes a new object
     * @param id the id of the object
     * @return a new {@link T} object
     */
    public abstract T make(I id);

}
