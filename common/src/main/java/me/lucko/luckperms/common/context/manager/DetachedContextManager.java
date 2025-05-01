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

package me.lucko.luckperms.common.context.manager;

import com.github.benmanes.caffeine.cache.LoadingCache;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.util.CaffeineFactory;
import net.luckperms.api.query.QueryOptions;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link ContextManager} which utilises 'detached' supplier caches stored alongside the subject instances.
 */
public abstract class DetachedContextManager<S, P extends S> extends ContextManager<S, P> {

    private final LoadingCache<S, QueryOptions> fallbackContextsCache = CaffeineFactory.newBuilder()
            .expireAfterWrite(50, TimeUnit.MILLISECONDS)
            .build(this::calculate);

    protected DetachedContextManager(LuckPermsPlugin plugin, Class<S> subjectClass, Class<P> playerClass) {
        super(plugin, subjectClass, playerClass);
    }

    @Override
    public QueryOptions getQueryOptions(S subject) {
        QueryOptionsSupplier supplier = getQueryOptionsSupplier(subject);
        if (supplier != null) {
            return supplier.getQueryOptions();
        }
        return this.fallbackContextsCache.get(subject);
    }

    @Override
    public void invalidateCache(S subject) {
        QueryOptionsSupplier queryOptionsSupplier = getQueryOptionsSupplier(subject);
        if (queryOptionsSupplier != null) {
            queryOptionsSupplier.invalidateCache();
        }
        this.fallbackContextsCache.invalidate(subject);
    }

    public QueryOptionsSupplier createQueryOptionsSupplier(S subject) {
        return new QueryOptionsCache<>(subject, this);
    }

    public abstract @Nullable QueryOptionsSupplier getQueryOptionsSupplier(S subject);

}
