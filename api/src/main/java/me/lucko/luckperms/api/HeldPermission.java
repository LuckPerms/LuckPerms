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

package me.lucko.luckperms.api;

import me.lucko.luckperms.api.context.ContextSet;

import java.util.Optional;
import java.util.OptionalLong;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * A relationship between a {@link PermissionHolder} and a permission.
 *
 * @param <T> the identifier type of the holder
 * @since 2.17
 */
@Immutable
public interface HeldPermission<T> {

    /**
     * Gets the holder of the permission
     *
     * @return the holder
     */
    @Nonnull
    T getHolder();

    /**
     * Gets the permission being held
     *
     * @return the permission
     */
    @Nonnull
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
    @Nonnull
    Optional<String> getServer();

    /**
     * Gets the world where the permission is held
     *
     * @return the world
     */
    @Nonnull
    Optional<String> getWorld();

    /**
     * Gets the time in unix time when the permission will expire
     *
     * @return the expiry time
     */
    OptionalLong getExpiry();

    /**
     * Gets the extra context for the permission.
     *
     * @return the extra context
     */
    ContextSet getContexts();

    /**
     * Converts this permission into a Node
     *
     * @return a Node copy of this permission
     */
    @Nonnull
    Node asNode();

}
