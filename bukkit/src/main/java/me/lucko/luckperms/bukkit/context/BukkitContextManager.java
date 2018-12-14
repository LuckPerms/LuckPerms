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

package me.lucko.luckperms.bukkit.context;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LookupSetting;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.ContextManager;
import me.lucko.luckperms.common.context.ContextsCache;
import me.lucko.luckperms.common.context.ContextsSupplier;
import me.lucko.luckperms.common.util.LoadingMap;

import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public class BukkitContextManager extends ContextManager<Player> {

    // cache the creation of ContextsCache instances for online players with no expiry
    private final LoadingMap<Player, ContextsCache<Player>> onlineSubjectCaches = LoadingMap.of(key -> new ContextsCache<>(key, this));

    // cache the creation of ContextsCache instances for offline players with a 1m expiry
    private final LoadingCache<Player, ContextsCache<Player>> offlineSubjectCaches = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(key -> {
                ContextsCache<Player> cache = this.onlineSubjectCaches.getIfPresent(key);
                if (cache != null) {
                    return cache;
                }
                return new ContextsCache<>(key, this);
            });

    public BukkitContextManager(LPBukkitPlugin plugin) {
        super(plugin, Player.class);
    }

    public void onPlayerQuit(Player player) {
        this.onlineSubjectCaches.remove(player);
    }

    @Override
    public ContextsSupplier getCacheFor(Player subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }

        if (subject.isOnline()) {
            return this.onlineSubjectCaches.get(subject);
        } else {
            return this.offlineSubjectCaches.get(subject);
        }
    }

    @Override
    public void invalidateCache(Player subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }

        ContextsCache<Player> cache = this.onlineSubjectCaches.getIfPresent(subject);
        if (cache != null) {
            cache.invalidate();
        }

        cache = this.offlineSubjectCaches.getIfPresent(subject);
        if (cache != null) {
            cache.invalidate();
        }
    }

    @Override
    public Contexts formContexts(Player subject, ImmutableContextSet contextSet) {
        Contexts contexts = this.plugin.getConfiguration().get(ConfigKeys.GLOBAL_CONTEXTS);
        if (subject.isOp()) {
            EnumSet<LookupSetting> settings = EnumSet.copyOf(contexts.getSettings());
            settings.add(LookupSetting.IS_OP);
            contexts = contexts.setSettings(settings);
        }

        return contexts.setContexts(contextSet);
    }
}
