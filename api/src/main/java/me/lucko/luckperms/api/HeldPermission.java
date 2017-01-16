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

package me.lucko.luckperms.api;

import com.google.common.collect.Multimap;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * A relationship between a Holder and a permission
 *
 * @param <T> the identifier type of the holder
 * @since 2.17
 */
public interface HeldPermission<T> {

    /**
     * Gets the holder of the permission
     *
     * @return the holder
     */
    T getHolder();

    /**
     * Gets the permission being held
     *
     * @return the permission
     */
    String getPermission();

    /**
     * Gets the value of the permission
     *
     * @return the value
     */
    boolean getValue();

    /**
     * Gets the server where the permission is held
     *
     * @return the server
     */
    Optional<String> getServer();

    /**
     * Gets the world where the permission is held
     *
     * @return the world
     */
    Optional<String> getWorld();

    /**
     * Gets the time in unix time when the permission will expire
     *
     * @return the expiry time
     */
    OptionalLong getExpiry();

    /**
     * Gets the context for the permission.
     *
     * @return the context
     */
    Multimap<String, String> getContext();

    /**
     * Converts this permission into a Node
     *
     * @return a Node copy of this permission
     */
    Node asNode();

}
