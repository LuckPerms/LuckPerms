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

package me.lucko.luckperms.common.caching;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.caching.CachedData;
import me.lucko.luckperms.api.caching.MetaContexts;
import me.lucko.luckperms.common.caching.type.MetaAccumulator;
import me.lucko.luckperms.common.caching.type.MetaCache;
import me.lucko.luckperms.common.caching.type.PermissionCache;
import me.lucko.luckperms.common.calculators.PermissionCalculatorMetadata;
import me.lucko.luckperms.common.metastacking.SimpleMetaStack;
import me.lucko.luckperms.common.model.PermissionHolder;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Holds an easily accessible cache of a holders data in a number of contexts
 */
public abstract class HolderCachedData<T extends PermissionHolder> implements CachedData {

    /**
     * The holder whom this data instance is representing
     */
    protected final T holder;

    /**
     * The cache used for {@link PermissionCache} instances.
     */
    private final LoadingCache<Contexts, PermissionCache> permission = Caffeine.newBuilder()
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new PermissionCacheLoader());

    /**
     * The cache used for {@link MetaCache} instances.
     */
    private final LoadingCache<MetaContexts, MetaCache> meta = Caffeine.newBuilder()
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new MetaCacheLoader());

    public HolderCachedData(T holder) {
        this.holder = holder;
    }

    protected abstract String getHolderName();

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
            PermissionCalculatorMetadata metadata = PermissionCalculatorMetadata.of(this.holder.getType(), getHolderName(), contexts.getContexts());
            data = new PermissionCache(contexts, metadata, this.holder.getPlugin().getCalculatorFactory());
        }

        if (contexts == Contexts.allowAll()) {
            data.setPermissions(this.holder.exportNodesAndShorthand(true));
        } else {
            data.setPermissions(this.holder.exportNodesAndShorthand(contexts, true));
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
            data = new MetaCache(contexts);
        }

        if (contexts.getContexts() == Contexts.allowAll()) {
            data.loadMeta(this.holder.accumulateMeta(newAccumulator(contexts)));
        } else {
            data.loadMeta(this.holder.accumulateMeta(newAccumulator(contexts), contexts.getContexts()));
        }

        return data;
    }

    @Nonnull
    @Override
    public PermissionCache getPermissionData(@Nonnull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");

        //noinspection ConstantConditions
        return this.permission.get(contexts);
    }

    @Nonnull
    @Override
    public MetaCache getMetaData(@Nonnull MetaContexts contexts) {
        Objects.requireNonNull(contexts, "contexts");

        //noinspection ConstantConditions
        return this.meta.get(contexts);
    }

    @Nonnull
    @Override
    public MetaCache getMetaData(@Nonnull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        return getMetaData(this.holder.getPlugin().getContextManager().formMetaContexts(contexts));
    }

    @Nonnull
    @Override
    public PermissionCache calculatePermissions(@Nonnull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        return calculatePermissions(contexts, null);
    }

    @Nonnull
    @Override
    public MetaCache calculateMeta(@Nonnull MetaContexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        return calculateMeta(contexts, null);
    }

    @Nonnull
    @Override
    public MetaCache calculateMeta(@Nonnull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        return calculateMeta(this.holder.getPlugin().getContextManager().formMetaContexts(contexts));
    }

    @Override
    public void recalculatePermissions(@Nonnull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        this.permission.refresh(contexts);
    }

    @Override
    public void recalculateMeta(@Nonnull MetaContexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        this.meta.refresh(contexts);
    }

    @Override
    public void recalculateMeta(@Nonnull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        recalculateMeta(this.holder.getPlugin().getContextManager().formMetaContexts(contexts));
    }

    @Nonnull
    @Override
    public CompletableFuture<PermissionCache> reloadPermissions(@Nonnull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");

        // get the previous value - to use when recalculating
        PermissionCache previous = this.permission.getIfPresent(contexts);

        // invalidate the entry
        this.permission.invalidate(contexts);

        // repopulate the cache
        return CompletableFuture.supplyAsync(() -> this.permission.get(contexts, c -> calculatePermissions(c, previous)));
    }

    @Nonnull
    @Override
    public CompletableFuture<MetaCache> reloadMeta(@Nonnull MetaContexts contexts) {
        Objects.requireNonNull(contexts, "contexts");

        // get the previous value - to use when recalculating
        MetaCache previous = this.meta.getIfPresent(contexts);

        // invalidate the entry
        this.meta.invalidate(contexts);

        // repopulate the cache
        return CompletableFuture.supplyAsync(() -> this.meta.get(contexts, c -> calculateMeta(c, previous)));
    }

    @Nonnull
    @Override
    public CompletableFuture<MetaCache> reloadMeta(@Nonnull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        return reloadMeta(this.holder.getPlugin().getContextManager().formMetaContexts(contexts));
    }

    @Override
    public void recalculatePermissions() {
        Set<Contexts> keys = this.permission.asMap().keySet();
        keys.forEach(this::recalculatePermissions);
    }

    @Override
    public void recalculateMeta() {
        Set<MetaContexts> keys = this.meta.asMap().keySet();
        keys.forEach(this::recalculateMeta);
    }

    @Nonnull
    @Override
    public CompletableFuture<Void> reloadPermissions() {
        Set<Contexts> keys = new HashSet<>(this.permission.asMap().keySet());
        return CompletableFuture.allOf(keys.stream().map(this::reloadPermissions).toArray(CompletableFuture[]::new));
    }

    @Nonnull
    @Override
    public CompletableFuture<Void> reloadMeta() {
        Set<MetaContexts> keys = new HashSet<>(this.meta.asMap().keySet());
        return CompletableFuture.allOf(keys.stream().map(this::reloadMeta).toArray(CompletableFuture[]::new));
    }

    @Override
    public void preCalculate(@Nonnull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");

        // pre-calculate just by requesting the data from this cache.
        // if the data isn't already loaded, it will be calculated.
        getPermissionData(contexts);
        getMetaData(contexts);
    }

    @Override
    public void invalidatePermissions(@Nonnull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        this.permission.invalidate(contexts);
    }

    @Override
    public void invalidateMeta(@Nonnull MetaContexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        this.meta.invalidate(contexts);
    }

    @Override
    public void invalidateMeta(@Nonnull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        this.meta.invalidate(this.holder.getPlugin().getContextManager().formMetaContexts(contexts));
    }

    @Override
    public void invalidatePermissionCalculators() {
        this.permission.asMap().values().forEach(PermissionCache::invalidateCache);
    }

    public void invalidateCaches() {
        this.permission.invalidateAll();
        this.meta.invalidateAll();
    }

    public void doCacheCleanup() {
        this.permission.cleanUp();
        this.meta.cleanUp();
    }

    private final class PermissionCacheLoader implements CacheLoader<Contexts, PermissionCache> {
        @Override
        public PermissionCache load(@Nonnull Contexts contexts) {
            return calculatePermissions(contexts);
        }

        @Override
        public PermissionCache reload(@Nonnull Contexts contexts, @Nonnull PermissionCache oldData) {
            return calculatePermissions(contexts, oldData);
        }
    }

    private final class MetaCacheLoader implements CacheLoader<MetaContexts, MetaCache> {
        @Override
        public MetaCache load(@Nonnull MetaContexts contexts) {
            return calculateMeta(contexts);
        }

        @Override
        public MetaCache reload(@Nonnull MetaContexts contexts, @Nonnull MetaCache oldData) {
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
