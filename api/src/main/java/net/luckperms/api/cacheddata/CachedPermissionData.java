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

package net.luckperms.api.cacheddata;

import net.luckperms.api.node.Node;
import net.luckperms.api.util.Tristate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;

/**
 * Holds cached permission lookup data for a specific set of contexts.
 *
 * <p>All calls will account for inheritance, as well as any default data
 * provided by the platform. These calls are heavily cached and are therefore
 * fast.</p>
 */
public interface CachedPermissionData extends CachedData {

    /**
     * Performs a permission check for the given {@code permission} node.
     *
     * <p>This check is equivalent to the "hasPermission" method call on most platforms.
     * You can use {@link Tristate#asBoolean()} if you need a truthy result.</p>
     *
     * @param permission the permission node
     * @return a result containing the tristate
     * @throws NullPointerException if permission is null
     * @since 5.4
     */
    @NonNull Result<Tristate, Node> queryPermission(@NonNull String permission);

    /**
     * Performs a permission check for the given {@code permission} node.
     *
     * <p>This check is equivalent to the "hasPermission" method call on most platforms.
     * You can use {@link Tristate#asBoolean()} if you need a truthy result.</p>
     *
     * @param permission the permission node
     * @return a tristate result
     * @throws NullPointerException if permission is null
     */
    default @NonNull Tristate checkPermission(@NonNull String permission) {
        return queryPermission(permission).result();
    }

    /**
     * Invalidates the underlying permission calculator cache.
     *
     * <p>Can be called to allow for an update in defaults.</p>
     */
    void invalidateCache();

    /**
     * Gets an immutable copy of the permission map backing the permission calculator
     *
     * @return an immutable set of permissions
     */
    @NonNull @Unmodifiable Map<String, Boolean> getPermissionMap();

}
