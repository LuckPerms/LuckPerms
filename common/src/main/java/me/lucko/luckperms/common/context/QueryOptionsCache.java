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

package me.lucko.luckperms.common.context;

import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.query.QueryOptions;
import me.lucko.luckperms.common.cache.ExpiringCache;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link QueryOptionsSupplier} that caches results.
 *
 * @param <T> the player type
 */
public final class QueryOptionsCache<T> extends ExpiringCache<QueryOptions> implements QueryOptionsSupplier {
    private final T subject;
    private final ContextManager<T> contextManager;

    public QueryOptionsCache(T subject, ContextManager<T> contextManager) {
        super(50L, TimeUnit.MILLISECONDS); // expire roughly every tick
        this.subject = subject;
        this.contextManager = contextManager;
    }

    @Override
    protected @NonNull QueryOptions supply() {
        return this.contextManager.calculate(this.subject);
    }

    @Override
    public QueryOptions getQueryOptions() {
        return get();
    }

    @Override
    public ImmutableContextSet getContextSet() {
        return get().context();
    }
}
