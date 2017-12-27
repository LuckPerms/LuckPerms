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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

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
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.metastacking.SimpleMetaStack;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Holds an easily accessible cache of a holders data in a number of contexts
 */
@RequiredArgsConstructor
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

    protected abstract String getHolderName();

    /**
     * Calculates a {@link PermissionCache} instance.
     *
     * @param contexts the contexts to calculate in
     * @param data an old data instance to try to reuse - ignored if null
     * @return the calculated instance
     */
    private PermissionCache calculatePermissions(@NonNull Contexts contexts, PermissionCache data) {
        if (data == null) {
            PermissionCalculatorMetadata metadata = PermissionCalculatorMetadata.of(holder.getType(), getHolderName(), contexts.getContexts());
            data = new PermissionCache(contexts, metadata, holder.getPlugin().getCalculatorFactory());
        }

        if (contexts == Contexts.allowAll()) {
            data.setPermissions(holder.exportNodesAndShorthand(true));
        } else {
            data.setPermissions(holder.exportNodesAndShorthand(contexts, true));
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
    private MetaCache calculateMeta(@NonNull MetaContexts contexts, MetaCache data) {
        if (data == null) {
            data = new MetaCache(contexts);
        }

        if (contexts.getContexts() == Contexts.allowAll()) {
            data.loadMeta(holder.accumulateMeta(newAccumulator(contexts), null));
        } else {
            data.loadMeta(holder.accumulateMeta(newAccumulator(contexts), null, contexts.getContexts()));
        }

        return data;
    }

    @Override
    public PermissionCache getPermissionData(@NonNull Contexts contexts) {
        //noinspection ConstantConditions
        return permission.get(contexts);
    }

    @Override
    public MetaCache getMetaData(@NonNull MetaContexts contexts) {
        //noinspection ConstantConditions
        return meta.get(contexts);
    }

    @Override
    public MetaCache getMetaData(@NonNull Contexts contexts) {
        return getMetaData(makeFromMetaContextsConfig(contexts, holder.getPlugin()));
    }

    @Override
    public PermissionCache calculatePermissions(@NonNull Contexts contexts) {
        return calculatePermissions(contexts, null);
    }

    @Override
    public MetaCache calculateMeta(@NonNull MetaContexts contexts) {
        return calculateMeta(contexts, null);
    }

    @Override
    public MetaCache calculateMeta(@NonNull Contexts contexts) {
        return calculateMeta(makeFromMetaContextsConfig(contexts, holder.getPlugin()));
    }

    @Override
    public void recalculatePermissions(@NonNull Contexts contexts) {
        permission.refresh(contexts);
    }

    @Override
    public void recalculateMeta(@NonNull MetaContexts contexts) {
        meta.refresh(contexts);
    }

    @Override
    public void recalculateMeta(@NonNull Contexts contexts) {
        recalculateMeta(makeFromMetaContextsConfig(contexts, holder.getPlugin()));
    }

    @Override
    public CompletableFuture<PermissionCache> reloadPermissions(@NonNull Contexts contexts) {
        // get the previous value - to use when recalculating
        PermissionCache previous = permission.getIfPresent(contexts);

        // invalidate the entry
        permission.invalidate(contexts);

        // repopulate the cache
        return CompletableFuture.supplyAsync(() -> permission.get(contexts, c -> calculatePermissions(c, previous)));
    }

    @Override
    public CompletableFuture<MetaCache> reloadMeta(@NonNull MetaContexts contexts) {
        // get the previous value - to use when recalculating
        MetaCache previous = meta.getIfPresent(contexts);

        // invalidate the entry
        meta.invalidate(contexts);

        // repopulate the cache
        return CompletableFuture.supplyAsync(() -> meta.get(contexts, c -> calculateMeta(c, previous)));
    }

    @Override
    public CompletableFuture<MetaCache> reloadMeta(@NonNull Contexts contexts) {
        return reloadMeta(makeFromMetaContextsConfig(contexts, holder.getPlugin()));
    }

    @Override
    public void recalculatePermissions() {
        Set<Contexts> keys = permission.asMap().keySet();
        keys.forEach(this::recalculatePermissions);
    }

    @Override
    public void recalculateMeta() {
        Set<MetaContexts> keys = meta.asMap().keySet();
        keys.forEach(this::recalculateMeta);
    }

    @Override
    public CompletableFuture<Void> reloadPermissions() {
        Set<Contexts> keys = new HashSet<>(permission.asMap().keySet());
        return CompletableFuture.allOf(keys.stream().map(this::reloadPermissions).toArray(CompletableFuture[]::new));
    }

    @Override
    public CompletableFuture<Void> reloadMeta() {
        Set<MetaContexts> keys = new HashSet<>(meta.asMap().keySet());
        return CompletableFuture.allOf(keys.stream().map(this::reloadMeta).toArray(CompletableFuture[]::new));
    }

    @Override
    public void preCalculate(@NonNull Contexts contexts) {
        // pre-calculate just by requesting the data from this cache.
        // if the data isn't already loaded, it will be calculated.
        getPermissionData(contexts);
        getMetaData(contexts);
    }

    @Override
    public void invalidatePermissions(Contexts contexts) {
        permission.invalidate(contexts);
    }

    @Override
    public void invalidateMeta(MetaContexts contexts) {
        meta.invalidate(contexts);
    }

    @Override
    public void invalidateMeta(Contexts contexts) {
        meta.invalidate(makeFromMetaContextsConfig(contexts, holder.getPlugin()));
    }

    @Override
    public void invalidatePermissionCalculators() {
        permission.asMap().values().forEach(PermissionCache::invalidateCache);
    }

    public void invalidateCaches() {
        permission.invalidateAll();
        meta.invalidateAll();
    }

    public void doCacheCleanup() {
        permission.cleanUp();
        meta.cleanUp();
    }

    private final class PermissionCacheLoader implements CacheLoader<Contexts, PermissionCache> {
        @Override
        public PermissionCache load(Contexts contexts) {
            return calculatePermissions(contexts);
        }

        @Override
        public PermissionCache reload(Contexts contexts, PermissionCache oldData) {
            return calculatePermissions(contexts, oldData);
        }
    }

    private final class MetaCacheLoader implements CacheLoader<MetaContexts, MetaCache> {
        @Override
        public MetaCache load(MetaContexts contexts) {
            return calculateMeta(contexts);
        }

        @Override
        public MetaCache reload(MetaContexts contexts, MetaCache oldData) {
            return calculateMeta(contexts, oldData);
        }
    }

    private static MetaContexts makeFromMetaContextsConfig(Contexts contexts, LuckPermsPlugin plugin) {
        return new MetaContexts(
                contexts,
                plugin.getConfiguration().get(ConfigKeys.PREFIX_FORMATTING_OPTIONS),
                plugin.getConfiguration().get(ConfigKeys.SUFFIX_FORMATTING_OPTIONS)
        );
    }

    private static MetaAccumulator newAccumulator(MetaContexts contexts) {
        return new MetaAccumulator(
                new SimpleMetaStack(contexts.getPrefixStackDefinition(), ChatMetaType.PREFIX),
                new SimpleMetaStack(contexts.getSuffixStackDefinition(), ChatMetaType.SUFFIX)
        );
    }

}
