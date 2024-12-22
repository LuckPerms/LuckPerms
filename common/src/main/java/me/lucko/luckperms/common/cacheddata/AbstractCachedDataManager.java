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

import me.lucko.luckperms.common.cache.LoadingMap;
import me.lucko.luckperms.common.cacheddata.type.MetaAccumulator;
import me.lucko.luckperms.common.cacheddata.type.MonitoredMetaCache;
import me.lucko.luckperms.common.cacheddata.type.PermissionCache;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.calculator.PermissionCalculator;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.util.CaffeineFactory;
import me.lucko.luckperms.common.util.CompletableFutures;
import net.luckperms.api.cacheddata.CachedData;
import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.metastacking.MetaStackDefinition;
import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.node.Node;
import net.luckperms.api.query.QueryOptions;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Abstract implementation of {@link CachedDataManager}.
 */
public abstract class AbstractCachedDataManager implements CachedDataManager {
    private final LuckPermsPlugin plugin;
    private final AbstractContainer<PermissionCache, CachedPermissionData> permission;
    private final AbstractContainer<MonitoredMetaCache, CachedMetaData> meta;

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
    public @NonNull MonitoredMetaCache getMetaData(@NonNull QueryOptions queryOptions) {
        return this.meta.get(queryOptions);
    }

    @Override
    public @NonNull PermissionCache getPermissionData() {
        return getPermissionData(getQueryOptions());
    }

    @Override
    public @NonNull MonitoredMetaCache getMetaData() {
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
     * @param mapFactory a function to create a map instance to return the results in
     * @param queryOptions the query options
     * @param <M> the map type
     * @return the resolved permissions
     */
    protected abstract <M extends Map<String, Node>> M resolvePermissions(IntFunction<M> mapFactory, QueryOptions queryOptions);

    /**
     * Resolves the owners meta data for the given {@link QueryOptions}.
     *
     * @param accumulator the accumulator to add resolved meta to
     * @param queryOptions the query options
     */
    protected abstract void resolveMeta(MetaAccumulator accumulator, QueryOptions queryOptions);
    
    private PermissionCache calculatePermissions(QueryOptions queryOptions) {
        Objects.requireNonNull(queryOptions, "queryOptions");
        CacheMetadata metadata = getMetadataForQueryOptions(queryOptions);

        ConcurrentHashMap<String, Node> sourcePermissions = resolvePermissions(ConcurrentHashMap::new, queryOptions);
        return new PermissionCache(queryOptions, metadata, getCalculatorFactory(), sourcePermissions);
    }
    
    private MonitoredMetaCache calculateMeta(QueryOptions queryOptions) {
        Objects.requireNonNull(queryOptions, "queryOptions");
        CacheMetadata metadata = getMetadataForQueryOptions(queryOptions);

        MetaAccumulator accumulator = newAccumulator(queryOptions);
        resolveMeta(accumulator, queryOptions);

        return new MonitoredMetaCache(this.plugin, queryOptions, metadata, accumulator);
    }

    @Override
    public final void invalidate() {
        this.permission.invalidate();
        this.meta.invalidate();
    }

    @Override
    public final void invalidatePermissionCalculators() {
        this.permission.cache.values().forEach(PermissionCache::invalidateCache);
    }

    public final void performCacheCleanup() {
        this.permission.cleanup();
        this.meta.cleanup();
    }

    private static final class AbstractContainer<C extends I, I extends CachedData> implements Container<I> {
        private final Function<QueryOptions, C> cacheLoader;
        private final LoadingMap<QueryOptions, C> cache;

        public AbstractContainer(Function<QueryOptions, C> cacheLoader) {
            this.cacheLoader = cacheLoader;
            this.cache = LoadingMap.of(this.cacheLoader);
        }

        public void cleanup() {
            this.cache.values().removeIf(value -> !((UsageTracked) value).usedInTheLast(2, TimeUnit.MINUTES));
        }

        @Override
        public @NonNull C get(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");

            C data = this.cache.get(queryOptions);
            ((UsageTracked) data).recordUsage();
            return data;
        }

        @Override
        public @NonNull C calculate(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");
            return this.cacheLoader.apply(queryOptions);
        }

        @Override
        public void recalculate(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");
            CompletableFuture.runAsync(() -> {
                final C value = this.cacheLoader.apply(queryOptions);
                this.cache.put(queryOptions, value);
            }, CaffeineFactory.executor());
        }

        @Override
        public @NonNull CompletableFuture<? extends C> reload(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");

            // invalidate the previous value until we're done recalculating
            this.cache.remove(queryOptions);

            // request recalculation from the cache
            return CompletableFuture.supplyAsync(() -> this.cache.get(queryOptions), CaffeineFactory.executor());
        }

        @Override
        public void recalculate() {
            Set<QueryOptions> keys = this.cache.keySet();
            keys.forEach(this::recalculate);
        }

        @Override
        public @NonNull CompletableFuture<Void> reload() {
            Set<QueryOptions> keys = this.cache.keySet();
            return CompletableFutures.allOf(keys.stream().map(this::reload));
        }

        @Override
        public void invalidate(@NonNull QueryOptions queryOptions) {
            Objects.requireNonNull(queryOptions, "queryOptions");
            this.cache.remove(queryOptions);
        }

        @Override
        public void invalidate() {
            this.cache.clear();
        }
    }
    
    private MetaAccumulator newAccumulator(QueryOptions queryOptions) {
        return new MetaAccumulator(
                queryOptions.option(MetaStackDefinition.PREFIX_STACK_KEY).orElseGet(() -> getDefaultMetaStackDefinition(ChatMetaType.PREFIX)),
                queryOptions.option(MetaStackDefinition.SUFFIX_STACK_KEY).orElseGet(() -> getDefaultMetaStackDefinition(ChatMetaType.SUFFIX))
        );
    }

}
