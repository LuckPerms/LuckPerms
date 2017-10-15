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
import me.lucko.luckperms.api.caching.MetaContexts;
import me.lucko.luckperms.api.caching.UserData;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.metastacking.SimpleMetaStack;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Holds an easily accessible cache of a user's data in a number of contexts
 */
@RequiredArgsConstructor
public class UserCache implements UserData {

    /**
     * The user whom this data instance is representing
     */
    private final User user;

    private final LoadingCache<Contexts, PermissionCache> permission = Caffeine.newBuilder()
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new PermissionCacheLoader());

    private final LoadingCache<MetaContexts, MetaCache> meta = Caffeine.newBuilder()
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new MetaCacheLoader());

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
        // just create a MetaContexts instance using the values in the config
        return getMetaData(makeFromMetaContextsConfig(contexts, user.getPlugin()));
    }

    @Override
    public PermissionCache calculatePermissions(@NonNull Contexts contexts) {
        PermissionCache data = new PermissionCache(contexts, user, user.getPlugin().getCalculatorFactory());

        if (contexts == Contexts.allowAll()) {
            data.setPermissions(user.exportNodesAndShorthand(true));
        } else {
            data.setPermissions(user.exportNodesAndShorthand(contexts, true));
        }

        return data;
    }

    @Override
    public MetaCache calculateMeta(@NonNull MetaContexts contexts) {
        MetaCache data = new MetaCache();

        if (contexts.getContexts() == Contexts.allowAll()) {
            data.loadMeta(user.accumulateMeta(newAccumulator(contexts), null));
        } else {
            data.loadMeta(user.accumulateMeta(newAccumulator(contexts), null, contexts.getContexts()));
        }

        return data;
    }

    @Override
    public MetaCache calculateMeta(@NonNull Contexts contexts) {
        // just create a MetaContexts instance using the values in the config
        return calculateMeta(makeFromMetaContextsConfig(contexts, user.getPlugin()));
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
        recalculateMeta(makeFromMetaContextsConfig(contexts, user.getPlugin()));
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
    public void preCalculate(@NonNull Set<Contexts> contexts) {
        contexts.forEach(this::preCalculate);
    }

    @Override
    public void preCalculate(@NonNull Contexts contexts) {
        // pre-calculate just by requesting the data from this cache.
        // if the data isn't already loaded, it will be calculated.
        getPermissionData(contexts);
        getMetaData(contexts);
    }

    public void invalidateCaches() {
        permission.invalidateAll();
        meta.invalidateAll();
    }

    @Override
    public void invalidatePermissionCalculators() {
        permission.asMap().values().forEach(PermissionCache::invalidateCache);
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
            if (contexts == Contexts.allowAll()) {
                oldData.comparePermissions(user.exportNodesAndShorthand(true));
            } else {
                oldData.comparePermissions(user.exportNodesAndShorthand(contexts, true));
            }

            return oldData;
        }
    }

    private final class MetaCacheLoader implements CacheLoader<MetaContexts, MetaCache> {
        @Override
        public MetaCache load(MetaContexts contexts) {
            return calculateMeta(contexts);
        }

        @Override
        public MetaCache reload(MetaContexts contexts, MetaCache oldData) {
            if (contexts.getContexts() == Contexts.allowAll()) {
                oldData.loadMeta(user.accumulateMeta(newAccumulator(contexts), null));
            } else {
                oldData.loadMeta(user.accumulateMeta(newAccumulator(contexts), null, contexts.getContexts()));
            }

            return oldData;
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
