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

import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.CompletableFuture;

/**
 * Holds cached permission and meta lookup data for a {@link PermissionHolder}.
 *
 * <p>All calls will account for inheritance, as well as any default data
 * provided by the platform. These calls are heavily cached and are therefore
 * fast.</p>
 */
public interface CachedDataManager {

    /**
     * Gets the manager for {@link CachedPermissionData}.
     *
     * @return the permission data manager
     */
    @NonNull Container<CachedPermissionData> permissionData();

    /**
     * Gets the manager for {@link CachedMetaData}.
     *
     * @return the meta data manager
     */
    @NonNull Container<CachedMetaData> metaData();

    /**
     * Gets PermissionData from the cache, using the given query options.
     *
     * @param queryOptions the query options
     * @return a permission data instance
     */
    @NonNull CachedPermissionData getPermissionData(@NonNull QueryOptions queryOptions);

    /**
     * Gets MetaData from the cache, using the given query options.
     *
     * @param queryOptions the query options
     * @return a meta data instance
     */
    @NonNull CachedMetaData getMetaData(@NonNull QueryOptions queryOptions);

    /**
     * Gets PermissionData from the cache, using the most appropriate query options
     * available at the time.
     *
     * <p>For {@link User}s, the most appropriate query options will be their
     * {@link ContextManager#getQueryOptions(User) current active query options} if the
     * corresponding player is online, and otherwise, will fallback to
     * {@link ContextManager#getStaticQueryOptions() the current static query options}.</p>
     *
     * <p>For {@link Group}s, the most appropriate query options will always be
     * {@link ContextManager#getStaticQueryOptions() the current static query options}.</p>
     *
     * @return a permission data instance
     * @since 5.1
     */
    @NonNull CachedPermissionData getPermissionData();

    /**
     * Gets MetaData from the cache, using the most appropriate query options
     * available at the time.
     *
     * <p>For {@link User}s, the most appropriate query options will be their
     * {@link ContextManager#getQueryOptions(User) current active query options} if the
     * corresponding player is online, and otherwise, will fallback to
     * {@link ContextManager#getStaticQueryOptions() the current static query options}.</p>
     *
     * <p>For {@link Group}s, the most appropriate query options will always be
     * {@link ContextManager#getStaticQueryOptions() the current static query options}.</p>
     *
     * @return a meta data instance
     * @since 5.1
     * @see PermissionHolder#getQueryOptions()
     */
    @NonNull CachedMetaData getMetaData();

    /**
     * Invalidates all cached {@link CachedPermissionData} and {@link CachedMetaData}
     * instances.
     */
    void invalidate();

    /**
     * Invalidates all underlying permission calculators.
     *
     * <p>Can be called to allow for an update in defaults.</p>
     */
    void invalidatePermissionCalculators();

    /**
     * Manages a specific type of {@link CachedData cached data} within
     * a {@link CachedDataManager} instance.
     *
     * @param <T> the data type
     */
    interface Container<T extends CachedData> {

        /**
         * Gets {@link T data} from the cache.
         *
         * @param queryOptions the query options
         * @return a data instance
         * @throws NullPointerException if contexts is null
         */
        @NonNull T get(@NonNull QueryOptions queryOptions);

        /**
         * Calculates {@link T data}, bypassing the cache.
         *
         * <p>The result of this operation is calculated each time the method is called.
         * The result is not added to the internal cache.</p>
         *
         * <p>It is therefore highly recommended to use {@link #get(QueryOptions)} instead.</p>
         *
         * <p>The use cases of this method are more around constructing one-time
         * instances of {@link T data}, without adding the result to the cache.</p>
         *
         * @param queryOptions the query options
         * @return a data instance
         * @throws NullPointerException if contexts is null
         */
        @NonNull T calculate(@NonNull QueryOptions queryOptions);

        /**
         * (Re)calculates data for a given context.
         *
         * <p>This method returns immediately in all cases. The (re)calculation is
         * performed asynchronously and applied to the cache in the background.</p>
         *
         * <p>If there was a previous data instance associated with
         * the given {@link QueryOptions}, then that instance will continue to be returned by
         * {@link #get(QueryOptions)} until the recalculation is completed.</p>
         *
         * <p>If there was no value calculated and cached prior to the call of this
         * method, then one will be calculated.</p>
         *
         * @param queryOptions the query options
         * @throws NullPointerException if contexts is null
         */
        void recalculate(@NonNull QueryOptions queryOptions);

        /**
         * (Re)loads permission data for a given context.
         *
         * <p>Unlike {@link #recalculate(QueryOptions)}, this method immediately
         * invalidates any previous data values contained within the cache,
         * and then schedules a task to reload a new data instance to
         * replace the one which was invalidated.</p>
         *
         * <p>The invalidation happens immediately during the execution of this method.
         * The result of the re-computation encapsulated by the future.</p>
         *
         * <p>Subsequent calls to {@link #get(QueryOptions)} will block until
         * the result of this operation is complete.</p>
         *
         * <p>If there was no value calculated and cached prior to the call of this
         * method, then one will be calculated.</p>
         *
         * <p>This method returns a Future so users can optionally choose to wait
         * until the recalculation has been performed.</p>
         *
         * @param queryOptions the query options.
         * @return a future
         * @throws NullPointerException if contexts is null
         */
        @NonNull CompletableFuture<? extends T> reload(@NonNull QueryOptions queryOptions);

        /**
         * Recalculates data for all known contexts.
         *
         * <p>This method returns immediately. The recalculation is performed
         * asynchronously and applied to the cache in the background.</p>
         *
         * <p>The previous data instances will continue to be returned
         * by {@link #get(QueryOptions)} until the recalculation is completed.</p>
         */
        void recalculate();

        /**
         * Reloads permission data for all known contexts.
         *
         * <p>Unlike {@link #recalculate()}, this method immediately
         * invalidates all previous data values contained within the cache,
         * and then schedules a task to reload new data instances to
         * replace the ones which were invalidated.</p>
         *
         * <p>The invalidation happens immediately during the execution of this method.
         * The result of the re-computation encapsulated by the future.</p>
         *
         * <p>Subsequent calls to {@link #get(QueryOptions)} will block until
         * the result of this operation is complete.</p>
         *
         * <p>This method returns a Future so users can optionally choose to wait
         * until the recalculation has been performed.</p>
         *
         * @return a future
         */
        @NonNull CompletableFuture<Void> reload();

        /**
         * Invalidates any cached data instances mapped to the given context.
         *
         * @param queryOptions the queryOptions to invalidate for
         */
        void invalidate(@NonNull QueryOptions queryOptions);

        /**
         * Invalidates all cached data instances.
         */
        void invalidate();
    }

}
