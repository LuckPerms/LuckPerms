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

package me.lucko.luckperms.common.config;

import lombok.Getter;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import me.lucko.luckperms.common.api.delegates.LPConfigurationDelegate;
import me.lucko.luckperms.common.config.keys.EnduringKey;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An abstract implementation of {@link LuckPermsConfiguration}, backed by a cache.
 */
public abstract class AbstractConfiguration implements LuckPermsConfiguration {

    // the loading cache for config keys --> their value
    // the value is wrapped in an optional as null values don't get cached.
    private final LoadingCache<ConfigKey<?>, Optional<Object>> cache = Caffeine.newBuilder().build(this::loadKeyValue);

    @Getter
    private final LPConfigurationDelegate delegate = new LPConfigurationDelegate(this);

    @Getter
    private final ContextsFile contextsFile = new ContextsFile(this);

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(ConfigKey<T> key) {
        Optional<Object> ret = cache.get(key);
        if (ret == null) {
            return null;
        }
        return (T) ret.orElse(null);
    }

    @Override
    public void loadAll() {
        ConfigKeys.getAllKeys().values().forEach(cache::get);
        contextsFile.load();
    }

    @Override
    public void reload() {
        init();

        Set<ConfigKey<?>> toInvalidate = cache.asMap().keySet().stream().filter(k -> !(k instanceof EnduringKey)).collect(Collectors.toSet());
        cache.invalidateAll(toInvalidate);

        loadAll();
        getPlugin().getApiProvider().getEventFactory().handleConfigReload();
    }

    private Optional<Object> loadKeyValue(ConfigKey<?> key) {
        return Optional.ofNullable(key.get(this));
    }
}
