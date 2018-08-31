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

package me.lucko.luckperms.velocity.contexts;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.velocitypowered.api.proxy.Player;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.contexts.ContextManager;
import me.lucko.luckperms.common.contexts.ContextsCache;
import me.lucko.luckperms.common.contexts.ContextsSupplier;
import me.lucko.luckperms.velocity.LPVelocityPlugin;

import java.util.concurrent.TimeUnit;

public class VelocityContextManager extends ContextManager<Player> {

    private final LoadingCache<Player, ContextsCache<Player>> subjectCaches = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(key -> new ContextsCache<>(key, this));

    public VelocityContextManager(LPVelocityPlugin plugin) {
        super(plugin, Player.class);
    }

    @Override
    public ContextsSupplier getCacheFor(Player subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }

        return subjectCaches.get(subject);
    }

    @Override
    public void invalidateCache(Player subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }

        ContextsCache<Player> cache = this.subjectCaches.getIfPresent(subject);
        if (cache != null) {
            cache.invalidate();
        }
    }

    @Override
    public Contexts formContexts(Player subject, ImmutableContextSet contextSet) {
        return formContexts(contextSet);
    }
}
