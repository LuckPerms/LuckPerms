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
import com.github.benmanes.caffeine.cache.Caffeine;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.FullySatisfiedContexts;
import me.lucko.luckperms.api.caching.CachedData;
import me.lucko.luckperms.api.caching.MetaContexts;
import me.lucko.luckperms.common.cacheddata.type.MetaAccumulator;
import me.lucko.luckperms.common.cacheddata.type.MetaCache;
import me.lucko.luckperms.common.cacheddata.type.PermissionCache;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.calculator.PermissionCalculator;
import me.lucko.luckperms.common.metastacking.SimpleMetaStack;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Abstract implementation of {@link CachedData}.
 */
public abstract class AbstractCachedData implements CachedData {

    /**
     * The plugin instance
     */
    private final LuckPermsPlugin plugin;
    
    /**
     * The cache used for {@link PermissionCache} instances.
     */
    private final AsyncLoadingCache<Contexts, PermissionCache> permission = Caffeine.newBuilder()
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .buildAsync(new PermissionCacheLoader());

    /**
     * The cache used for {@link MetaCache} instances.
     */
    private final AsyncLoadingCache<MetaContexts, MetaCache> meta = Caffeine.newBuilder()
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .buildAsync(new MetaCacheLoader());

    public AbstractCachedData(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    /**
     * Returns a {@link CacheMetadata} instance for the given {@link Contexts}.
     * 
     * @param contexts the contexts the cache is for
     * @return the metadata instance
     */
    protected abstract CacheMetadata getMetadataForContexts(Contexts contexts);

    /**
     * Gets the {@link CalculatorFactory} used to build {@link PermissionCalculator}s.
     * 
     * @return the calculator factory
     */
    protected abstract CalculatorFactory getCalculatorFactory();

    /**
     * Upgrades the given {@link Contexts} to a {@link MetaContexts} instance using the default settings.
     * 
     * @param contexts the contexts to upgrade
     * @return a meta contexts instance
     */
    protected abstract MetaContexts getDefaultMetaContexts(Contexts contexts);

    /**
     * Resolves the owners permissions data according to the specification
     * outlined by {@link FullySatisfiedContexts}.
     * 
     * @return a map of permissions to back the {@link PermissionCache}
     */
    protected abstract Map<String, Boolean> resolvePermissions();

    /**
     * Resolves the owners permissions data in the given {@link Contexts}.
     *
     * @param contexts the contexts
     * @return a map of permissions to back the {@link PermissionCache}
     */
    protected abstract Map<String, Boolean> resolvePermissions(Contexts contexts);

    /**
     * Resolves the owners meta data according to the specification
     * outlined by {@link FullySatisfiedContexts}.
     *
     * @param accumulator the accumulator to add resolved meta to
     */
    protected abstract void resolveMeta(MetaAccumulator accumulator);

    /**
     * Resolves the owners meta data in the given {@link Contexts}.
     *
     * @param accumulator the accumulator to add resolved meta to
     * @param contexts the contexts
     */
    protected abstract void resolveMeta(MetaAccumulator accumulator, MetaContexts contexts);
    
    /**
     * Calculates a {@link PermissionCache} instance.
     *
     * @param contexts the contexts to calculate in
     * @param data an old data instance to try to reuse - ignored if null
     * @return the calculated instance
     */
    private PermissionCache calculatePermissions(Contexts contexts, PermissionCache data) {
        Objects.requireNonNull(contexts, "contexts");

        if (data == null) {
            CacheMetadata metadata = getMetadataForContexts(contexts);
            data = new PermissionCache(contexts, metadata, getCalculatorFactory());
        }

        if (contexts == Contexts.allowAll()) {
            data.setPermissions(resolvePermissions());
        } else {
            data.setPermissions(resolvePermissions(contexts));
        }

        return data;
    }

    /**
     * Calculates a {@link MetaCache} instance.
     *
     * @param contexts the contexts to calculate in
     * @param data an old data instance to try to reuse - ignored if null
     * @return the calculated instance
     */
    private MetaCache calculateMeta(MetaContexts contexts, MetaCache data) {
        Objects.requireNonNull(contexts, "contexts");

        if (data == null) {
            CacheMetadata metadata = getMetadataForContexts(contexts.getContexts());
            data = new MetaCache(contexts, metadata);
        }

        MetaAccumulator accumulator = newAccumulator(contexts);
        if (contexts.getContexts() == Contexts.allowAll()) {
            resolveMeta(accumulator);
        } else {
            resolveMeta(accumulator, contexts);
        }
        data.loadMeta(accumulator);

        return data;
    }

    @Override
    public final @NonNull PermissionCache getPermissionData(@NonNull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");

        //noinspection ConstantConditions
        return this.permission.synchronous().get(contexts);
    }

    @Override
    public final @NonNull MetaCache getMetaData(@NonNull MetaContexts contexts) {
        Objects.requireNonNull(contexts, "contexts");

        //noinspection ConstantConditions
        return this.meta.synchronous().get(contexts);
    }

    @Override
    public final @NonNull MetaCache getMetaData(@NonNull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        return getMetaData(getDefaultMetaContexts(contexts));
    }

    @Override
    public final @NonNull PermissionCache calculatePermissions(@NonNull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        return calculatePermissions(contexts, null);
    }

    @Override
    public final @NonNull MetaCache calculateMeta(@NonNull MetaContexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        return calculateMeta(contexts, null);
    }

    @Override
    public final @NonNull MetaCache calculateMeta(@NonNull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        return calculateMeta(getDefaultMetaContexts(contexts));
    }

    @Override
    public final void recalculatePermissions(@NonNull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        this.permission.synchronous().refresh(contexts);
    }

    @Override
    public final void recalculateMeta(@NonNull MetaContexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        this.meta.synchronous().refresh(contexts);
    }

    @Override
    public final void recalculateMeta(@NonNull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        recalculateMeta(getDefaultMetaContexts(contexts));
    }

    @Override
    public final @NonNull CompletableFuture<PermissionCache> reloadPermissions(@NonNull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");

        // get the previous value - to use when recalculating
        CompletableFuture<PermissionCache> previous = this.permission.getIfPresent(contexts);

        // invalidate any previous setting
        this.permission.synchronous().invalidate(contexts);

        // if the previous value is already calculated, use it when recalculating.
        PermissionCache value = getIfReady(previous);
        if (value != null) {
            return this.permission.get(contexts, c -> calculatePermissions(c, value));
        }

        // otherwise, just calculate a new value
        return this.permission.get(contexts);
    }

    @Override
    public final @NonNull CompletableFuture<MetaCache> reloadMeta(@NonNull MetaContexts contexts) {
        Objects.requireNonNull(contexts, "contexts");

        // get the previous value - to use when recalculating
        CompletableFuture<MetaCache> previous = this.meta.getIfPresent(contexts);

        // invalidate any previous setting
        this.meta.synchronous().invalidate(contexts);

        // if the previous value is already calculated, use it when recalculating.
        MetaCache value = getIfReady(previous);
        if (value != null) {
            return this.meta.get(contexts, c -> calculateMeta(c, value));
        }

        // otherwise, just calculate a new value
        return this.meta.get(contexts);
    }

    @Override
    public final @NonNull CompletableFuture<MetaCache> reloadMeta(@NonNull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        return reloadMeta(getDefaultMetaContexts(contexts));
    }

    @Override
    public final void recalculatePermissions() {
        Set<Contexts> keys = this.permission.synchronous().asMap().keySet();
        keys.forEach(this::recalculatePermissions);
    }

    @Override
    public final void recalculateMeta() {
        Set<MetaContexts> keys = this.meta.synchronous().asMap().keySet();
        keys.forEach(this::recalculateMeta);
    }

    @Override
    public final @NonNull CompletableFuture<Void> reloadPermissions() {
        Set<Contexts> keys = this.permission.synchronous().asMap().keySet();
        return CompletableFuture.allOf(keys.stream().map(this::reloadPermissions).toArray(CompletableFuture[]::new));
    }

    @Override
    public final @NonNull CompletableFuture<Void> reloadMeta() {
        Set<MetaContexts> keys = this.meta.synchronous().asMap().keySet();
        return CompletableFuture.allOf(keys.stream().map(this::reloadMeta).toArray(CompletableFuture[]::new));
    }

    @Override
    public final void invalidatePermissions(@NonNull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        this.permission.synchronous().invalidate(contexts);
    }

    @Override
    public final void invalidateMeta(@NonNull MetaContexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        this.meta.synchronous().invalidate(contexts);
    }

    @Override
    public final void invalidateMeta(@NonNull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        this.meta.synchronous().invalidate(getDefaultMetaContexts(contexts));
    }

    @Override
    public final void invalidatePermissions() {
        this.permission.synchronous().invalidateAll();
    }

    @Override
    public final void invalidateMeta() {
        this.meta.synchronous().invalidateAll();
    }

    @Override
    public final void invalidatePermissionCalculators() {
        this.permission.synchronous().asMap().values().forEach(PermissionCache::invalidateCache);
    }

    public final void invalidate() {
        invalidatePermissions();
        invalidateMeta();
    }

    public final void doCacheCleanup() {
        this.permission.synchronous().cleanUp();
        this.meta.synchronous().cleanUp();
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

    private final class PermissionCacheLoader implements CacheLoader<Contexts, PermissionCache> {
        @Override
        public PermissionCache load(@NonNull Contexts contexts) {
            return calculatePermissions(contexts);
        }

        @Override
        public PermissionCache reload(@NonNull Contexts contexts, @NonNull PermissionCache oldData) {
            return calculatePermissions(contexts, oldData);
        }
    }

    private final class MetaCacheLoader implements CacheLoader<MetaContexts, MetaCache> {
        @Override
        public MetaCache load(@NonNull MetaContexts contexts) {
            return calculateMeta(contexts);
        }

        @Override
        public MetaCache reload(@NonNull MetaContexts contexts, @NonNull MetaCache oldData) {
            return calculateMeta(contexts, oldData);
        }
    }

    private static MetaAccumulator newAccumulator(MetaContexts contexts) {
        return new MetaAccumulator(
                new SimpleMetaStack(contexts.getPrefixStackDefinition(), ChatMetaType.PREFIX),
                new SimpleMetaStack(contexts.getSuffixStackDefinition(), ChatMetaType.SUFFIX)
        );
    }

}
