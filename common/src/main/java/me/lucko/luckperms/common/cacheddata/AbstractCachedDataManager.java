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

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;

import me.lucko.luckperms.common.cache.MRUCache;
import me.lucko.luckperms.common.cacheddata.type.MetaAccumulator;
import me.lucko.luckperms.common.cacheddata.type.MetaCache;
import me.lucko.luckperms.common.cacheddata.type.PermissionCache;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.calculator.PermissionCalculator;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.util.CaffeineFactory;

import net.luckperms.api.cacheddata.CachedData;
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
    private final LuckPermsPlugin plugin;
    private final AbstractContainer<PermissionCache, CachedPermissionData> permission;
    private final AbstractContainer<MetaCache, CachedMetaData> meta;

    protected AbstractCachedDataManager(LuckPermsPlugin plugin) {
        this.plugin = plugin;
        this.permission = new AbstractContainer<>(this::calculatePermissions);
        this.meta = new AbstractContainer<>(this::calculateMeta);
    }

    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public @NonNull Container<CachedPermissionData> permissionData() {
        return this.permission;
    }

    @Override
    public @NonNull Container<CachedMetaData> metaData() {
        return this.meta;
    }

    @Override
    public @NonNull PermissionCache getPermissionData(@NonNull QueryOptions queryOptions) {
        return this.permission.get(queryOptions);
    }

    @Override
    public @NonNull MetaCache getMetaData(@NonNull QueryOptions queryOptions) {
        return this.meta.get(queryOptions);
    }

    @Override
    public @NonNull PermissionCache getPermissionData() {
        return getPermissionData(getQueryOptions());
    }

    @Override
    public @NonNull MetaCache getMetaData() {
        return getMetaData(getQueryOptions());
    }

    /**
     * Returns a {@link CacheMetadata} instance for the given {@link QueryOptions}.
     * 
     * @param queryOptions the query options the cache is for
     * @return the metadata instance
     */
    protected abstract CacheMetadata getMetadataForQueryOptions(QueryOptions queryOptions);

    /**
     * Gets the most appropriate active query options instance for the holder.
     *
     * @return the query options
     */
    protected abstract QueryOptions getQueryOptions();

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
        this.permission.invalidate();
        this.meta.invalidate();
    }

    @Override
    public final void invalidatePermissionCalculators() {
        this.permission.cache.asMap().values().forEach(PermissionCache::invalidateCache);
    }

    public final void performCacheCleanup() {
        this.permission.cache.cleanUp();
        this.meta.cache.cleanUp();
    }

    private static final class AbstractContainer<C extends I, I extends CachedData> extends MRUCache<RecentData<C>> implements Container<I> {
        private final Loader<QueryOptions, C> cacheLoader;
        private final LoadingCache<QueryOptions, C> cache;

        public AbstractContainer(Loader<QueryOptions, C> cacheLoader) {
            this.cacheLoader = cacheLoader;
            this.cache = CaffeineFactory.newBuilder()
                    .expireAfterAccess(2, TimeUnit.MINUTES)
                    .build(this.cacheLoader);
        }

        @Override
        public @NonNull C get(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");

            RecentData<C> recent = getRecent();
            if (recent != null && queryOptions.equals(recent.queryOptions)) {
                return recent.data;
            }

            int modCount = modCount();
            C data = this.cache.get(queryOptions);
            offerRecent(modCount, new RecentData<>(queryOptions, data));

            //noinspection ConstantConditions
            return data;
        }

        @Override
        public @NonNull C calculate(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");
            return this.cacheLoader.load(queryOptions);
        }

        @Override
        public void recalculate(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");
            this.cache.refresh(queryOptions);
            clearRecent();
        }

        @Override
        public @NonNull CompletableFuture<? extends C> reload(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");

            // get the previous value - we can reuse the same instance
            C previous = this.cache.getIfPresent(queryOptions);

            // invalidate the previous value until we're done recalculating
            this.cache.invalidate(queryOptions);
            clearRecent();

            // request recalculation from the cache
            if (previous != null) {
                return CompletableFuture.supplyAsync(
                        () -> this.cache.get(queryOptions, c -> this.cacheLoader.reload(c, previous)),
                        CaffeineFactory.executor()
                );
            } else {
                return CompletableFuture.supplyAsync(
                        () -> this.cache.get(queryOptions),
                        CaffeineFactory.executor()
                );
            }
        }

        @Override
        public void recalculate() {
            Set<QueryOptions> keys = this.cache.asMap().keySet();
            keys.forEach(this::recalculate);
        }

        @Override
        public @NonNull CompletableFuture<Void> reload() {
            Set<QueryOptions> keys = this.cache.asMap().keySet();
            return CompletableFuture.allOf(keys.stream().map(this::reload).toArray(CompletableFuture[]::new));
        }

        @Override
        public void invalidate(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");
            this.cache.invalidate(queryOptions);
            clearRecent();
        }

        @Override
        public void invalidate() {
            this.cache.invalidateAll();
            clearRecent();
        }
    }

    private interface Loader<K, V> extends CacheLoader<K, V> {
        @NonNull V load(@NonNull K key, @Nullable V oldValue);

        @Override
        default @NonNull V load(@NonNull K key) {
            return load(key, null);
        }

        @Override
        default @NonNull V reload(@NonNull K key, @NonNull V oldValue) {
            return load(key, oldValue);
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
                getMetaStackDefinition(queryOptions, ChatMetaType.PREFIX),
                getMetaStackDefinition(queryOptions, ChatMetaType.SUFFIX)
        );
    }

    private static final class RecentData<T> {
        final QueryOptions queryOptions;
        final T data;

        RecentData(QueryOptions queryOptions, T data) {
            this.queryOptions = queryOptions;
            this.data = data;
        }
    }

}
