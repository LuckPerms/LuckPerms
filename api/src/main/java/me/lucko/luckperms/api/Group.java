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

import me.lucko.luckperms.api.caching.GroupData;
import me.lucko.luckperms.api.context.ContextSet;

import java.util.OptionalInt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An inheritable holder of permission data.
 */
public interface Group extends PermissionHolder {

    /**
     * Get the name of the group
     *
     * @return the name of the group
     */
    @Nonnull
    String getName();

    /**
     * Gets the groups "display name", if it has one that differs from it's actual name.
     *
     * <p>The lookup is made using the current servers active context.</p>
     *
     * <p>Will return <code>null</code> if the groups display name is equal to it's
     * {@link #getName() actual name}.</p>
     *
     * @return the display name
     * @since 4.3
     */
    @Nullable
    String getDisplayName();

    /**
     * Gets the groups "display name", if it has one that differs from it's actual name.
     *
     * <p>Will return <code>null</code> if the groups display name is equal to it's
     * {@link #getName() actual name}.</p>
     *
     * @param contextSet the contexts to lookup in
     * @return the display name
     * @since 4.3
     */
    @Nullable
    String getDisplayName(@Nonnull ContextSet contextSet);

    /**
     * Gets the weight of this group, if present.
     *
     * @return the group weight
     * @since 2.17
     */
    @Nonnull
    OptionalInt getWeight();

    /**
     * Gets the groups's {@link GroupData} cache.
     *
     * @return the groups cached data.
     * @since 4.0
     */
    @Nonnull
    @Override
    GroupData getCachedData();

}
