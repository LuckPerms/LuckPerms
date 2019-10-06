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

package me.lucko.luckperms.common.model.manager;

import java.util.Map;
import java.util.function.Function;

/**
 * A class which manages instances of a class
 *
 * @param <I> the class used to identify each object held in this manager
 * @param <C> the super class being managed
 * @param <T> the implementation class this manager is "managing"
 */
public interface Manager<I, C, T extends C> extends Function<I, T> {

    /**
     * Gets a map containing all cached instances held by this manager.
     *
     * @return all instances held in this manager
     */
    Map<I, T> getAll();

    /**
     * Gets or creates an object by id
     *
     * <p>Should only every be called by the storage implementation.</p>
     *
     * @param id The id to search by
     * @return a {@link T} object if the object is loaded or makes and returns a new object
     */
    T getOrMake(I id);

    /**
     * Get an object by id
     *
     * @param id The id to search by
     * @return a {@link T} object if the object is loaded, returns null if the object is not loaded
     */
    T getIfLoaded(I id);

    /**
     * Check to see if a object is loaded or not
     *
     * @param id The id of the object
     * @return true if the object is loaded
     */
    boolean isLoaded(I id);

    /**
     * Removes and unloads the object from the manager
     *
     * @param id The object id to unload
     */
    void unload(I id);

}
