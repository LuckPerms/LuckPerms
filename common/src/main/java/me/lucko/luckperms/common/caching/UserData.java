/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.users.User;

import java.util.Set;

/**
 * Holds an easily accessible cache of a user's data in a number of contexts
 */
@RequiredArgsConstructor
public class UserData {

    /**
     * The user whom this data instance is representing
     */
    private final User user;

    /**
     * A provider of {@link me.lucko.luckperms.common.calculators.PermissionCalculator}s for the instance
     */
    private final CalculatorFactory calculatorFactory;

    private final LoadingCache<Contexts, PermissionData> permission = CacheBuilder.newBuilder()
            .build(new CacheLoader<Contexts, PermissionData>() {
                @Override
                public PermissionData load(Contexts contexts) {
                    return calculatePermissions(contexts);
                }

                @Override
                public ListenableFuture<PermissionData> reload(Contexts contexts, PermissionData oldData) {
                    oldData.comparePermissions(user.exportNodes(contexts, true));
                    return Futures.immediateFuture(oldData);
                }
            });

    private final LoadingCache<Contexts, MetaData> meta = CacheBuilder.newBuilder()
            .build(new CacheLoader<Contexts, MetaData>() {
                @Override
                public MetaData load(Contexts contexts) {
                    return calculateMeta(contexts);
                }

                @Override
                public ListenableFuture<MetaData> reload(Contexts contexts, MetaData oldData) {
                    oldData.loadMeta(user.getAllNodes(null, contexts));
                    return Futures.immediateFuture(oldData);
                }
            });

    /**
     * Gets PermissionData from the cache, given a specified context.
     * If the data is not cached, it is calculated. Therefore, this call could be costly.
     * @param contexts the contexts to get the permission data in
     * @return a permission data instance
     */
    public PermissionData getPermissionData(Contexts contexts) {
        return permission.getUnchecked(contexts);
    }

    /**
     * Gets MetaData from the cache, given a specified context.
     * If the data is not cached, it is calculated. Therefore, this call could be costly.
     * @param contexts the contexts to get the permission data in
     * @return a meta data instance
     */
    public MetaData getMetaData(Contexts contexts) {
        return meta.getUnchecked(contexts);
    }

    /**
     * Calculates permission data, bypassing the cache.
     * @param contexts the contexts to get permission data in
     * @return a permission data instance
     */
    public PermissionData calculatePermissions(Contexts contexts) {
        PermissionData data = new PermissionData(contexts, user, calculatorFactory);
        data.setPermissions(user.exportNodes(contexts, true));
        return data;
    }

    /**
     * Calculates meta data, bypassing the cache.
     * @param contexts the contexts to get meta data in
     * @return a meta data instance
     */
    public MetaData calculateMeta(Contexts contexts) {
        MetaData data = new MetaData(contexts);
        data.loadMeta(user.getAllNodes(null, contexts));
        return data;
    }

    /**
     * Calculates permission data and stores it in the cache. If there is already data cached for the given contexts,
     * and if the resultant output is different, the cached value is updated.
     * @param contexts the contexts to recalculate in.
     */
    public void recalculatePermissions(Contexts contexts) {
        permission.refresh(contexts);
    }

    /**
     * Calculates meta data and stores it in the cache. If there is already data cached for the given contexts,
     * and if the resultant output is different, the cached value is updated.
     * @param contexts the contexts to recalculate in.
     */
    public void recalculateMeta(Contexts contexts) {
        meta.refresh(contexts);
    }

    /**
     * Calls {@link #recalculatePermissions(Contexts)} for all current loaded contexts
     */
    public void recalculatePermissions() {
        Set<Contexts> keys = ImmutableSet.copyOf(permission.asMap().keySet());
        keys.forEach(permission::refresh);
    }

    /**
     * Calls {@link #recalculateMeta(Contexts)} for all current loaded contexts
     */
    public void recalculateMeta() {
        Set<Contexts> keys = ImmutableSet.copyOf(meta.asMap().keySet());
        keys.forEach(meta::refresh);
    }

    /**
     * Calls {@link #preCalculate(Contexts)} for the given contexts
     * @param contexts a set of contexts
     */
    public void preCalculate(Set<Contexts> contexts) {
        contexts.forEach(this::preCalculate);
    }

    /**
     * Ensures that PermissionData and MetaData is cached for a context. If the cache does not contain any data for the
     * context, it will be calculated and saved.
     * @param contexts the contexts to pre-calculate for
     */
    public void preCalculate(Contexts contexts) {
        permission.getUnchecked(contexts);
        meta.getUnchecked(contexts);
    }

    public void invalidateCache() {
        permission.invalidateAll();
        meta.invalidateAll();
    }

    public void invalidatePermissionCalculators() {
        permission.asMap().values().forEach(PermissionData::invalidateCache);
    }

}
