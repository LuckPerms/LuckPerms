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

package me.lucko.luckperms.bungee.context;

import com.github.benmanes.caffeine.cache.LoadingCache;

import me.lucko.luckperms.bungee.LPBungeePlugin;
import me.lucko.luckperms.common.context.ContextManager;
import me.lucko.luckperms.common.context.QueryOptionsSupplier;
import me.lucko.luckperms.common.util.CaffeineFactory;

import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.query.QueryOptions;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BungeeContextManager extends ContextManager<ProxiedPlayer, ProxiedPlayer> {

    private final LoadingCache<ProxiedPlayer, QueryOptions> contextsCache = CaffeineFactory.newBuilder()
            .expireAfterWrite(50, TimeUnit.MILLISECONDS)
            .build(this::calculate);

    public BungeeContextManager(LPBungeePlugin plugin) {
        super(plugin, ProxiedPlayer.class, ProxiedPlayer.class);
    }

    @Override
    public UUID getUniqueId(ProxiedPlayer player) {
        return player.getUniqueId();
    }

    @Override
    public QueryOptionsSupplier getCacheFor(ProxiedPlayer subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }

        return new InlineQueryOptionsSupplier(subject, this.contextsCache);
    }

    @Override
    public QueryOptionsSupplier getCacheForPlayer(ProxiedPlayer player) {
        return getCacheFor(player);
    }

    @Override
    public ImmutableContextSet getContext(ProxiedPlayer subject) {
        return getQueryOptions(subject).context();
    }

    @Override
    public QueryOptions getQueryOptions(ProxiedPlayer subject) {
        return this.contextsCache.get(subject);
    }

    @Override
    public QueryOptions getPlayerQueryOptions(ProxiedPlayer player) {
        return this.contextsCache.get(player);
    }

    @Override
    protected void invalidateCache(ProxiedPlayer subject) {
        this.contextsCache.invalidate(subject);
    }

    @Override
    public QueryOptions formQueryOptions(ProxiedPlayer subject, ImmutableContextSet contextSet) {
        return formQueryOptions(contextSet);
    }

    private static final class InlineQueryOptionsSupplier implements QueryOptionsSupplier {
        private final ProxiedPlayer key;
        private final LoadingCache<ProxiedPlayer, QueryOptions> cache;

        private InlineQueryOptionsSupplier(ProxiedPlayer key, LoadingCache<ProxiedPlayer, QueryOptions> cache) {
            this.key = key;
            this.cache = cache;
        }

        @Override
        public QueryOptions getQueryOptions() {
            return this.cache.get(this.key);
        }
    }
}
