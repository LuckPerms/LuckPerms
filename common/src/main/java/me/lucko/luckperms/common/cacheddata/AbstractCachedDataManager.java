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

package me.lucko.luckperms.common.cacheddata;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.CacheLoader;

import me.lucko.luckperms.common.cache.MRUCache;
import me.lucko.luckperms.common.cacheddata.type.MetaAccumulator;
import me.lucko.luckperms.common.cacheddata.type.MetaCache;
import me.lucko.luckperms.common.cacheddata.type.PermissionCache;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.calculator.PermissionCalculator;
import me.lucko.luckperms.common.metastacking.SimpleMetaStack;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.util.CaffeineFactory;

import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.metastacking.MetaStackDefinition;
import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.query.QueryOptions;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Abstract implementation of {@link CachedDataManager}.
 */
public abstract class AbstractCachedDataManager implements CachedDataManager {

    /**
     * The plugin instance
     */
    private final LuckPermsPlugin plugin;

    private final Permission permissionDataManager;
    private final Meta metaDataManager;

    protected AbstractCachedDataManager(LuckPermsPlugin plugin) {
        this.plugin = plugin;
        this.permissionDataManager = new Permission();
        this.metaDataManager = new Meta();
    }

    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public @NonNull Container<CachedPermissionData> permissionData() {
        return this.permissionDataManager;
    }

    @Override
    public @NonNull Container<CachedMetaData> metaData() {
        return this.metaDataManager;
    }

    @Override
    public @NonNull PermissionCache getPermissionData(@NonNull QueryOptions queryOptions) {
        return this.permissionDataManager.get(queryOptions);
    }

    @Override
    public @NonNull MetaCache getMetaData(@NonNull QueryOptions queryOptions) {
        return this.metaDataManager.get(queryOptions);
    }

    /**
     * Returns a {@link CacheMetadata} instance for the given {@link QueryOptions}.
     * 
     * @param queryOptions the query options the cache is for
     * @return the metadata instance
     */
    protected abstract CacheMetadata getMetadataForQueryOptions(QueryOptions queryOptions);

    /**
     * Gets the {@link CalculatorFactory} used to build {@link PermissionCalculator}s.
     * 
     * @return the calculator factory
     */
    protected abstract CalculatorFactory getCalculatorFactory();

    /**
     * Gets the default {@link MetaStackDefinition} for use if one wasn't specifically provided.
     *
     * @param type the type of meta stack
     * @return a meta stack definition instance
     */
    protected abstract MetaStackDefinition getDefaultMetaStackDefinition(ChatMetaType type);

    /**
     * Resolves the owners permissions data for the given {@link QueryOptions}.
     * 
     * @param queryOptions the query options
     * @return a map of permissions to back the {@link PermissionCache}
     */
    protected abstract Map<String, Boolean> resolvePermissions(QueryOptions queryOptions);

    /**
     * Resolves the owners meta data for the given {@link QueryOptions}.
     *
     * @param accumulator the accumulator to add resolved meta to
     * @param queryOptions the query options
     */
    protected abstract void resolveMeta(MetaAccumulator accumulator, QueryOptions queryOptions);
    
    private PermissionCache calculatePermissions(QueryOptions queryOptions, PermissionCache data) {
        Objects.requireNonNull(queryOptions, "queryOptions");

        if (data == null) {
            CacheMetadata metadata = getMetadataForQueryOptions(queryOptions);
            data = new PermissionCache(queryOptions, metadata, getCalculatorFactory());
        }

        data.setPermissions(resolvePermissions(queryOptions));
        return data;
    }
    
    private MetaCache calculateMeta(QueryOptions queryOptions, MetaCache data) {
        Objects.requireNonNull(queryOptions, "queryOptions");

        if (data == null) {
            CacheMetadata metadata = getMetadataForQueryOptions(queryOptions);
            data = new MetaCache(this.plugin, queryOptions, metadata);
        }

        MetaAccumulator accumulator = newAccumulator(queryOptions);
        resolveMeta(accumulator, queryOptions);
        data.loadMeta(accumulator);

        return data;
    }

    @Override
    public final void invalidate() {
        this.permissionDataManager.invalidate();
        this.metaDataManager.invalidate();
    }

    @Override
    public final void invalidatePermissionCalculators() {
        this.permissionDataManager.cache.synchronous().asMap().values().forEach(PermissionCache::invalidateCache);
    }

    public final void performCacheCleanup() {
        this.permissionDataManager.cache.synchronous().cleanUp();
        this.metaDataManager.cache.synchronous().cleanUp();
    }

    private final class Permission extends MRUCache<RecentPermissionData> implements Container<CachedPermissionData> {
        private final AsyncLoadingCache<QueryOptions, PermissionCache> cache = CaffeineFactory.newBuilder()
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .buildAsync(new PermissionCacheLoader());

        @Override
        public @NonNull PermissionCache get(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");

            RecentPermissionData recent = getRecent();
            if (recent != null && queryOptions.equals(recent.queryOptions)) {
                return recent.permissionData;
            }

            int modCount = modCount();
            PermissionCache data = this.cache.synchronous().get(queryOptions);
            offerRecent(modCount, new RecentPermissionData(queryOptions, data));

            //noinspection ConstantConditions
            return data;
        }

        @Override
        public @NonNull PermissionCache calculate(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");
            return calculatePermissions(queryOptions, null);
        }

        @Override
        public void recalculate(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");
            this.cache.synchronous().refresh(queryOptions);
            clearRecent();
        }

        @Override
        public @NonNull CompletableFuture<? extends PermissionCache> reload(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");

            // get the previous value - to use when recalculating
            CompletableFuture<PermissionCache> previous = this.cache.getIfPresent(queryOptions);

            // invalidate any previous setting
            this.cache.synchronous().invalidate(queryOptions);
            clearRecent();

            // if the previous value is already calculated, use it when recalculating.
            PermissionCache value = getIfReady(previous);
            if (value != null) {
                return this.cache.get(queryOptions, c -> calculatePermissions(c, value));
            }

            // otherwise, just calculate a new value
            return this.cache.get(queryOptions);
        }

        @Override
        public void recalculate() {
            Set<QueryOptions> keys = this.cache.synchronous().asMap().keySet();
            keys.forEach(this::recalculate);
        }

        @Override
        public @NonNull CompletableFuture<Void> reload() {
            Set<QueryOptions> keys = this.cache.synchronous().asMap().keySet();
            return CompletableFuture.allOf(keys.stream().map(this::reload).toArray(CompletableFuture[]::new));
        }

        @Override
        public void invalidate(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");
            this.cache.synchronous().invalidate(queryOptions);
            clearRecent();
        }

        @Override
        public void invalidate() {
            this.cache.synchronous().invalidateAll();
            clearRecent();
        }
    }

    private final class Meta extends MRUCache<RecentMetaData> implements Container<CachedMetaData> {
        private final AsyncLoadingCache<QueryOptions, MetaCache> cache = CaffeineFactory.newBuilder()
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .buildAsync(new MetaCacheLoader());

        @Override
        public @NonNull MetaCache get(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");

            RecentMetaData recent = getRecent();
            if (recent != null && queryOptions.equals(recent.queryOptions)) {
                return recent.metaData;
            }

            int modCount = modCount();
            MetaCache data = this.cache.synchronous().get(queryOptions);
            offerRecent(modCount, new RecentMetaData(queryOptions, data));

            //noinspection ConstantConditions
            return data;
        }

        @Override
        public @NonNull MetaCache calculate(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");
            return calculateMeta(queryOptions, null);
        }

        @Override
        public void recalculate(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");
            this.cache.synchronous().refresh(queryOptions);
            clearRecent();
        }

        @Override
        public @NonNull CompletableFuture<? extends MetaCache> reload(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");

            // get the previous value - to use when recalculating
            CompletableFuture<MetaCache> previous = this.cache.getIfPresent(queryOptions);

            // invalidate any previous setting
            this.cache.synchronous().invalidate(queryOptions);
            clearRecent();

            // if the previous value is already calculated, use it when recalculating.
            MetaCache value = getIfReady(previous);
            if (value != null) {
                return this.cache.get(queryOptions, c -> calculateMeta(c, value));
            }

            // otherwise, just calculate a new value
            return this.cache.get(queryOptions);
        }

        @Override
        public void recalculate() {
            Set<QueryOptions> keys = this.cache.synchronous().asMap().keySet();
            keys.forEach(this::recalculate);
        }

        @Override
        public @NonNull CompletableFuture<Void> reload() {
            Set<QueryOptions> keys = this.cache.synchronous().asMap().keySet();
            return CompletableFuture.allOf(keys.stream().map(this::reload).toArray(CompletableFuture[]::new));
        }

        @Override
        public void invalidate(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");
            this.cache.synchronous().invalidate(queryOptions);
            clearRecent();
        }

        @Override
        public void invalidate() {
            this.cache.synchronous().invalidateAll();
            clearRecent();
        }
    }

    private static boolean isReady(@Nullable CompletableFuture<?> future) {
        return (future != null) && future.isDone()
                && !future.isCompletedExceptionally()
                && (future.join() != null);
    }

    /** Returns the current value or null if either not done or failed. */
    private static <V> V getIfReady(@Nullable CompletableFuture<V> future) {
        return isReady(future) ? future.join() : null;
    }

    private final class PermissionCacheLoader implements CacheLoader<QueryOptions, PermissionCache> {
        @Override
        public PermissionCache load(@NonNull QueryOptions queryOptions) {
            return calculatePermissions(queryOptions, null);
        }

        @Override
        public PermissionCache reload(@NonNull QueryOptions queryOptions, @NonNull PermissionCache oldData) {
            return calculatePermissions(queryOptions, oldData);
        }
    }

    private final class MetaCacheLoader implements CacheLoader<QueryOptions, MetaCache> {
        @Override
        public MetaCache load(@NonNull QueryOptions queryOptions) {
            return calculateMeta(queryOptions, null);
        }

        @Override
        public MetaCache reload(@NonNull QueryOptions queryOptions, @NonNull MetaCache oldData) {
            return calculateMeta(queryOptions, oldData);
        }
    }

    private MetaStackDefinition getMetaStackDefinition(QueryOptions queryOptions, ChatMetaType type) {
        MetaStackDefinition stack = queryOptions.option(type == ChatMetaType.PREFIX ?
                MetaStackDefinition.PREFIX_STACK_KEY :
                MetaStackDefinition.SUFFIX_STACK_KEY
        ).orElse(null);
        if (stack == null) {
            stack = getDefaultMetaStackDefinition(type);
        }
        return stack;
    }
    
    private MetaAccumulator newAccumulator(QueryOptions queryOptions) {
        return new MetaAccumulator(
                new SimpleMetaStack(getMetaStackDefinition(queryOptions, ChatMetaType.PREFIX), ChatMetaType.PREFIX),
                new SimpleMetaStack(getMetaStackDefinition(queryOptions, ChatMetaType.SUFFIX), ChatMetaType.SUFFIX)
        );
    }

    private static final class RecentPermissionData {
        final QueryOptions queryOptions;
        final PermissionCache permissionData;

        RecentPermissionData(QueryOptions queryOptions, PermissionCache permissionData) {
            this.queryOptions = queryOptions;
            this.permissionData = permissionData;
        }
    }

    private static final class RecentMetaData {
        final QueryOptions queryOptions;
        final MetaCache metaData;

        RecentMetaData(QueryOptions queryOptions, MetaCache metaData) {
            this.queryOptions = queryOptions;
            this.metaData = metaData;
        }
    }

}
