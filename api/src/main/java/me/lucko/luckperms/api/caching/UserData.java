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

package me.lucko.luckperms.api.caching;

import me.lucko.luckperms.api.Contexts;

import java.util.Set;

/**
 * Holds cached permission and meta lookup data for a {@link me.lucko.luckperms.api.User}.
 *
 * <p>Data is only likely to be available for online users. All calls will account for inheritance, as well as any
 * default data provided by the platform. This calls are heavily cached and are therefore fast.</p>
 *
 * <p>For meta, both methods accepting {@link Contexts} and {@link MetaContexts} are provided. The only difference is that
 * the latter allows you to define how the meta stack should be structured internally. Where {@link Contexts} are passed, the
 * values from the configuration are used.</p>
 *
 * @since 2.13
 */
public interface UserData {

    /**
     * Gets PermissionData from the cache, given a specified context.
     *
     * <p>If the data is not cached, it is calculated. Therefore, this call could be costly.</p>
     *
     * @param contexts the contexts to get the permission data in
     * @return a permission data instance
     * @throws NullPointerException if contexts is null
     */
    PermissionData getPermissionData(Contexts contexts);

    /**
     * Gets MetaData from the cache, given a specified context.
     *
     * <p>If the data is not cached, it is calculated. Therefore, this call could be costly.</p>
     *
     * @param contexts the contexts to get the permission data in
     * @return a meta data instance
     * @throws NullPointerException if contexts is null
     * @since 3.2
     */
    MetaData getMetaData(MetaContexts contexts);

    /**
     * Gets MetaData from the cache, given a specified context.
     *
     * <p>If the data is not cached, it is calculated. Therefore, this call could be costly.</p>
     *
     * @param contexts the contexts to get the permission data in
     * @return a meta data instance
     * @throws NullPointerException if contexts is null
     */
    MetaData getMetaData(Contexts contexts);

    /**
     * Calculates permission data, bypassing the cache.
     *
     * @param contexts the contexts to get permission data in
     * @return a permission data instance
     * @throws NullPointerException if contexts is null
     */
    PermissionData calculatePermissions(Contexts contexts);

    /**
     * Calculates meta data, bypassing the cache.
     *
     * @param contexts the contexts to get meta data in
     * @return a meta data instance
     * @throws NullPointerException if contexts is null
     * @since 3.2
     */
    MetaData calculateMeta(MetaContexts contexts);

    /**
     * Calculates meta data, bypassing the cache.
     *
     * @param contexts the contexts to get meta data in
     * @return a meta data instance
     * @throws NullPointerException if contexts is null
     */
    MetaData calculateMeta(Contexts contexts);

    /**
     * Calculates permission data and stores it in the cache.
     *
     * <p>If there is already data cached for the given contexts, and if the resultant output is different,
     * the cached value is updated.</p>
     *
     * @param contexts the contexts to recalculate in.
     * @throws NullPointerException if contexts is null
     */
    void recalculatePermissions(Contexts contexts);

    /**
     * Calculates meta data and stores it in the cache.
     *
     * <p>If there is already data cached for the given contexts, and if the resultant output is different,
     * the cached value is updated.</p>
     *
     * @param contexts the contexts to recalculate in.
     * @throws NullPointerException if contexts is null
     * @since 3.2
     */
    void recalculateMeta(MetaContexts contexts);

    /**
     * Calculates meta data and stores it in the cache.
     *
     * <p>If there is already data cached for the given contexts, and if the resultant output is different,
     * the cached value is updated.</p>
     *
     * @param contexts the contexts to recalculate in.
     * @throws NullPointerException if contexts is null
     */
    void recalculateMeta(Contexts contexts);

    /**
     * Calls {@link #recalculatePermissions(Contexts)} for all current loaded contexts
     */
    void recalculatePermissions();

    /**
     * Calls {@link #recalculateMeta(Contexts)} for all current loaded contexts
     */
    void recalculateMeta();

    /**
     * Calls {@link #preCalculate(Contexts)} for the given contexts
     *
     * @param contexts a set of contexts
     * @throws NullPointerException if contexts is null
     */
    void preCalculate(Set<Contexts> contexts);

    /**
     * Ensures that PermissionData and MetaData is cached for a context.
     *
     * <p>If the cache does not contain any data for the context, it will be calculated and saved.</p>
     *
     * @param contexts the contexts to pre-calculate for
     * @throws NullPointerException if contexts is null
     */
    void preCalculate(Contexts contexts);

    /**
     * Invalidates all of the underlying Permission calculators.
     *
     * <p>Can be called to allow for an update in defaults.</p>
     */
    void invalidatePermissionCalculators();

}
