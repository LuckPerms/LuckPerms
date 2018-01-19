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
import me.lucko.luckperms.api.PermissionHolder;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

/**
 * Holds cached permission and meta lookup data for a {@link PermissionHolder}.
 *
 * <p>All calls will account for inheritance, as well as any default data provided by
 * the platform. This calls are heavily cached and are therefore fast.</p>
 *
 * <p>For meta, both methods accepting {@link Contexts} and {@link MetaContexts} are provided. The only difference is that
 * the latter allows you to define how the meta stack should be structured internally. Where {@link Contexts} are passed, the
 * values from the configuration are used.</p>
 *
 * @since 4.0
 */
public interface CachedData {

    /**
     * Gets PermissionData from the cache, given a specified context.
     *
     * @param contexts the contexts to get the permission data in
     * @return a permission data instance
     * @throws NullPointerException if contexts is null
     */
    @Nonnull
    PermissionData getPermissionData(@Nonnull Contexts contexts);

    /**
     * Gets MetaData from the cache, given a specified context.
     *
     * @param contexts the contexts to get the permission data in
     * @return a meta data instance
     * @throws NullPointerException if contexts is null
     * @since 3.2
     */
    @Nonnull
    MetaData getMetaData(@Nonnull MetaContexts contexts);

    /**
     * Gets MetaData from the cache, given a specified context.
     *
     * @param contexts the contexts to get the permission data in
     * @return a meta data instance
     * @throws NullPointerException if contexts is null
     */
    @Nonnull
    MetaData getMetaData(@Nonnull Contexts contexts);

    /**
     * Calculates permission data, bypassing the cache.
     *
     * <p>The result of this operation is calculated each time the method is called.
     * The result is not added to the internal cache.</p>
     *
     * <p>It is therefore highly recommended to use {@link #getPermissionData(Contexts)} instead.</p>
     *
     * <p>The use cases of this method are more around constructing one-time
     * instances of {@link PermissionData}, without adding the result to the cache.</p>
     *
     * @param contexts the contexts to get permission data in
     * @return a permission data instance
     * @throws NullPointerException if contexts is null
     */
    @Nonnull
    PermissionData calculatePermissions(@Nonnull Contexts contexts);

    /**
     * Calculates meta data, bypassing the cache.
     *
     * <p>The result of this operation is calculated each time the method is called.
     * The result is not added to the internal cache.</p>
     *
     * <p>It is therefore highly recommended to use {@link #getMetaData(MetaContexts)} instead.</p>
     *
     * <p>The use cases of this method are more around constructing one-time
     * instances of {@link MetaData}, without adding the result to the cache.</p>
     *
     * @param contexts the contexts to get meta data in
     * @return a meta data instance
     * @throws NullPointerException if contexts is null
     * @since 3.2
     */
    @Nonnull
    MetaData calculateMeta(@Nonnull MetaContexts contexts);

    /**
     * Calculates meta data, bypassing the cache.
     *
     * <p>The result of this operation is calculated each time the method is called.
     * The result is not added to the internal cache.</p>
     *
     * <p>It is therefore highly recommended to use {@link #getMetaData(Contexts)} instead.</p>
     *
     * <p>The use cases of this method are more around constructing one-time
     * instances of {@link MetaData}, without adding the result to the cache.</p>
     *
     * @param contexts the contexts to get meta data in
     * @return a meta data instance
     * @throws NullPointerException if contexts is null
     */
    @Nonnull
    MetaData calculateMeta(@Nonnull Contexts contexts);

    /**
     * (Re)calculates permission data for a given context.
     *
     * <p>This method returns immediately in all cases. The (re)calculation is
     * performed asynchronously and applied to the cache in the background.</p>
     *
     * <p>If there was a previous {@link PermissionData} instance associated with
     * the given {@link Contexts}, then that instance will continue to be returned by
     * {@link #getPermissionData(Contexts)} until the recalculation is completed.</p>
     *
     * <p>If there was no value calculated and cached prior to the call of this
     * method, then one will be calculated.</p>
     *
     * @param contexts the contexts to recalculate in.
     * @throws NullPointerException if contexts is null
     */
    void recalculatePermissions(@Nonnull Contexts contexts);

    /**
     * (Re)calculates meta data for a given context.
     *
     * <p>This method returns immediately in all cases. The (re)calculation is
     * performed asynchronously and applied to the cache in the background.</p>
     *
     * <p>If there was a previous {@link MetaData} instance associated with
     * the given {@link MetaContexts}, then that instance will continue to be returned by
     * {@link #getMetaData(MetaContexts)} until the recalculation is completed.</p>
     *
     * <p>If there was no value calculated and cached prior to the call of this
     * method, then one will be calculated.</p>
     *
     * @param contexts the contexts to recalculate in.
     * @throws NullPointerException if contexts is null
     * @since 3.2
     */
    void recalculateMeta(@Nonnull MetaContexts contexts);

    /**
     * (Re)calculates meta data for a given context.
     *
     * <p>This method returns immediately in all cases. The (re)calculation is
     * performed asynchronously and applied to the cache in the background.</p>
     *
     * <p>If there was a previous {@link MetaData} instance associated with
     * the given {@link Contexts}, then that instance will continue to be returned by
     * {@link #getMetaData(Contexts)} until the recalculation is completed.</p>
     *
     * <p>If there was no value calculated and cached prior to the call of this
     * method, then one will be calculated.</p>
     *
     * @param contexts the contexts to recalculate in.
     * @throws NullPointerException if contexts is null
     */
    void recalculateMeta(@Nonnull Contexts contexts);

    /**
     * (Re)loads permission data for a given context.
     *
     * <p>Unlike {@link #recalculatePermissions(Contexts)}, this method immediately
     * invalidates any previous {@link PermissionData} values contained within the cache,
     * and then schedules a task to reload a new {@link PermissionData} instance to
     * replace the one which was invalidated.</p>
     *
     * <p>The invalidation happens immediately during the execution of this method.
     * The result of the re-computation encapsulated by the future.</p>
     *
     * <p>Subsequent calls to {@link #getPermissionData(Contexts)} will block until
     * the result of this operation is complete.</p>
     *
     * <p>If there was no value calculated and cached prior to the call of this
     * method, then one will be calculated.</p>
     *
     * <p>This method returns a Future so users can optionally choose to wait
     * until the recalculation has been performed.</p>
     *
     * @param contexts the contexts to reload in.
     * @throws NullPointerException if contexts is null
     * @return a future
     * @since 4.0
     */
    @Nonnull
    CompletableFuture<? extends PermissionData> reloadPermissions(@Nonnull Contexts contexts);

    /**
     * (Re)loads meta data for a given context.
     *
     * <p>Unlike {@link #recalculateMeta(MetaContexts)}, this method immediately
     * invalidates any previous {@link MetaData} values contained within the cache,
     * and then schedules a task to reload a new {@link MetaData} instance to
     * replace the one which was invalidated.</p>
     *
     * <p>The invalidation happens immediately during the execution of this method.
     * The result of the re-computation encapsulated by the future.</p>
     *
     * <p>Subsequent calls to {@link #getMetaData(MetaContexts)} will block until
     * the result of this operation is complete.</p>
     *
     * <p>If there was no value calculated and cached prior to the call of this
     * method, then one will be calculated.</p>
     *
     * <p>This method returns a Future so users can optionally choose to wait
     * until the recalculation has been performed.</p>
     *
     * @param contexts the contexts to reload in.
     * @throws NullPointerException if contexts is null
     * @return a future
     * @since 4.0
     */
    @Nonnull
    CompletableFuture<? extends MetaData> reloadMeta(@Nonnull MetaContexts contexts);

    /**
     * (Re)loads meta data for a given context.
     *
     * <p>Unlike {@link #recalculateMeta(Contexts)}, this method immediately
     * invalidates any previous {@link MetaData} values contained within the cache,
     * and then schedules a task to reload a new {@link MetaData} instance to
     * replace the one which was invalidated.</p>
     *
     * <p>The invalidation happens immediately during the execution of this method.
     * The result of the re-computation encapsulated by the future.</p>
     *
     * <p>Subsequent calls to {@link #getMetaData(Contexts)} will block until
     * the result of this operation is complete.</p>
     *
     * <p>If there was no value calculated and cached prior to the call of this
     * method, then one will be calculated.</p>
     *
     * <p>This method returns a Future so users can optionally choose to wait
     * until the recalculation has been performed.</p>
     *
     * @param contexts the contexts to reload in.
     * @throws NullPointerException if contexts is null
     * @return a future
     * @since 4.0
     */
    @Nonnull
    CompletableFuture<? extends MetaData> reloadMeta(@Nonnull Contexts contexts);

    /**
     * Recalculates permission data for all known contexts.
     *
     * <p>This method returns immediately. The recalculation is performed
     * asynchronously and applied to the cache in the background.</p>
     *
     * <p>The previous {@link PermissionData} instances will continue to be returned
     * by {@link #getPermissionData(Contexts)} until the recalculation is completed.</p>
     */
    void recalculatePermissions();

    /**
     * Recalculates meta data for all known contexts.
     *
     * <p>This method returns immediately. The recalculation is performed
     * asynchronously and applied to the cache in the background.</p>
     *
     * <p>The previous {@link MetaData} instances will continue to be returned
     * by {@link #getMetaData(MetaContexts)} and {@link #getMetaData(Contexts)}
     * until the recalculation is completed.</p>
     */
    void recalculateMeta();

    /**
     * Reloads permission data for all known contexts.
     *
     * <p>Unlike {@link #recalculatePermissions()}, this method immediately
     * invalidates all previous {@link PermissionData} values contained within the cache,
     * and then schedules a task to reload a new {@link PermissionData} instances to
     * replace the ones which were invalidated.</p>
     *
     * <p>The invalidation happens immediately during the execution of this method.
     * The result of the re-computation encapsulated by the future.</p>
     *
     * <p>Subsequent calls to {@link #getPermissionData(Contexts)} will block until
     * the result of this operation is complete.</p>
     *
     * <p>This method returns a Future so users can optionally choose to wait
     * until the recalculation has been performed.</p>
     *
     * @return a future
     * @since 4.0
     */
    @Nonnull
    CompletableFuture<Void> reloadPermissions();

    /**
     * Reloads meta data for all known contexts.
     *
     * <p>Unlike {@link #recalculateMeta()}, this method immediately
     * invalidates all previous {@link MetaData} values contained within the cache,
     * and then schedules a task to reload a new {@link MetaData} instances to
     * replace the ones which were invalidated.</p>
     *
     * <p>The invalidation happens immediately during the execution of this method.
     * The result of the re-computation encapsulated by the future.</p>
     *
     * <p>Subsequent calls to {@link #getMetaData(MetaContexts)} and
     * {@link #getMetaData(Contexts)} will block until the result of this operation
     * is complete.</p>
     *
     * <p>This method returns a Future so users can optionally choose to wait
     * until the recalculation has been performed.</p>
     *
     * @return a future
     * @since 4.0
     */
    @Nonnull
    CompletableFuture<Void> reloadMeta();

    /**
     * Pre-calculates and caches {@link PermissionData} and {@link MetaData}
     * instances for the given contexts.
     *
     * <p>If the cache already contains a value for the given context,
     * no action is taken.</p>
     *
     * <p>This method blocks until the calculation is completed.</p>
     *
     * @param contexts a set of contexts
     * @throws NullPointerException if contexts is null
     */
    default void preCalculate(@Nonnull Set<Contexts> contexts) {
        contexts.forEach(this::preCalculate);
    }

    /**
     * Pre-calculates and caches {@link PermissionData} and {@link MetaData}
     * instances for a given context.
     *
     * <p>If the cache already contains a value for the given context,
     * no action is taken.</p>
     *
     * <p>This method blocks until the calculation is completed.</p>
     *
     * @param contexts the contexts to pre-calculate for
     * @throws NullPointerException if contexts is null
     */
    void preCalculate(@Nonnull Contexts contexts);

    /**
     * Invalidates any cached {@link PermissionData} instances mapped to the given
     * {@link Contexts}.
     *
     * @param contexts the contexts to invalidate for
     * @since 4.0
     */
    void invalidatePermissions(@Nonnull Contexts contexts);

    /**
     * Invalidates any cached {@link MetaData} instances mapped to the given
     * {@link MetaContexts}.
     *
     * @param contexts the contexts to invalidate for
     * @since 4.0
     */
    void invalidateMeta(@Nonnull MetaContexts contexts);

    /**
     * Invalidates any cached {@link MetaData} instances mapped to the given
     * {@link Contexts}.
     *
     * @param contexts the contexts to invalidate for
     * @since 4.0
     */
    void invalidateMeta(@Nonnull Contexts contexts);

    /**
     * Invalidates all of the underlying Permission calculators.
     *
     * <p>Can be called to allow for an update in defaults.</p>
     */
    void invalidatePermissionCalculators();

}
