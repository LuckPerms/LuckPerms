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

package me.lucko.luckperms.common.primarygroup;

import com.github.benmanes.caffeine.cache.LoadingCache;

import me.lucko.luckperms.api.query.QueryOptions;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.factory.NodeFactory;
import me.lucko.luckperms.common.util.CaffeineFactory;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Abstract implementation of {@link PrimaryGroupHolder} which caches all lookups.
 */
public abstract class ContextualHolder extends StoredHolder {

    // cache lookups
    private final LoadingCache<QueryOptions, Optional<String>> cache = CaffeineFactory.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(this::calculateValue);

    public ContextualHolder(User user) {
        super(user);
    }

    protected abstract @NonNull Optional<String> calculateValue(QueryOptions queryOptions);

    public void invalidateCache() {
        this.cache.invalidateAll();
    }

    @Override
    public final String getValue() {
        QueryOptions queryOptions = this.user.getPlugin().getQueryOptionsForUser(this.user).orElse(null);
        if (queryOptions == null) {
            queryOptions = this.user.getPlugin().getContextManager().getStaticQueryOptions();
        }

        return Objects.requireNonNull(this.cache.get(queryOptions))
                .orElseGet(() -> getStoredValue().orElse(NodeFactory.DEFAULT_GROUP_NAME));
    }

}
