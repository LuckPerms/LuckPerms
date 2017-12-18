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

package me.lucko.luckperms.common.contexts;

import lombok.NonNull;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.api.context.StaticContextCalculator;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * An abstract implementation of {@link ContextManager} which caches content lookups.
 *
 * @param <T> the calculator type
 */
public abstract class AbstractContextManager<T> implements ContextManager<T> {

    protected final LuckPermsPlugin plugin;
    private final Class<T> subjectClass;

    private final List<ContextCalculator<? super T>> calculators = new CopyOnWriteArrayList<>();
    private final List<StaticContextCalculator> staticCalculators = new CopyOnWriteArrayList<>();

    // caches context lookups
    private final LoadingCache<T, Contexts> lookupCache = Caffeine.newBuilder()
            .weakKeys()
            .expireAfterWrite(50L, TimeUnit.MILLISECONDS) // expire roughly every tick
            .build(new Loader());

    // caches static context lookups
    private final LoadingCache<Object, Contexts> staticLookupCache = Caffeine.newBuilder()
            .initialCapacity(1)
            .expireAfterWrite(50L, TimeUnit.MILLISECONDS) // expire roughly every tick
            .build(new StaticLoader());

    // the single key used in the static lookup cache
    private final Object staticCacheKey = new Object();

    protected AbstractContextManager(LuckPermsPlugin plugin, Class<T> subjectClass) {
        this.plugin = plugin;
        this.subjectClass = subjectClass;
    }

    @Override
    public Class<T> getSubjectClass() {
        return subjectClass;
    }

    @Override
    public ImmutableContextSet getApplicableContext(@NonNull T subject) {
        // this is actually already immutable, but the Contexts method signature returns the interface.
        // using the makeImmutable method is faster than casting
        return getApplicableContexts(subject).getContexts().makeImmutable();
    }

    @Override
    public Contexts getApplicableContexts(@NonNull T subject) {
        return lookupCache.get(subject);
    }

    @Override
    public ImmutableContextSet getStaticContext() {
        // this is actually already immutable, but the Contexts method signature returns the interface.
        // using the makeImmutable method is faster than casting
        return getStaticContexts().getContexts().makeImmutable();
    }

    @Override
    public Contexts getStaticContexts() {
        return staticLookupCache.get(staticCacheKey);
    }

    @Override
    public Optional<String> getStaticContextString() {
        Set<Map.Entry<String, String>> entries = getStaticContext().toSet();
        if (entries.isEmpty()) {
            return Optional.empty();
        }

        // effectively: if entries contains any non-server keys
        if (entries.stream().anyMatch(pair -> !pair.getKey().equals(Contexts.SERVER_KEY))) {
            // return all entries in 'key=value' form
            return Optional.of(entries.stream().map(pair -> pair.getKey() + "=" + pair.getValue()).collect(Collectors.joining(";")));
        } else {
            // just return the server ids, without the 'server='
            return Optional.of(entries.stream().map(Map.Entry::getValue).collect(Collectors.joining(";")));
        }
    }

    @Override
    public Contexts formContexts(ImmutableContextSet contextSet) {
        return new Contexts(
                contextSet,
                plugin.getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_PERMS),
                plugin.getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_WORLD_PERMS),
                true,
                plugin.getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_GROUPS),
                plugin.getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_WORLD_GROUPS),
                false
        );
    }

    @Override
    public void registerCalculator(ContextCalculator<? super T> calculator) {
        // calculators registered first should have priority (and be checked last.)
        calculators.add(0, calculator);
    }

    @Override
    public void registerStaticCalculator(StaticContextCalculator calculator) {
        registerCalculator(calculator);
        staticCalculators.add(0, calculator);
    }

    @Override
    public void invalidateCache(@NonNull T subject){
        lookupCache.invalidate(subject);
    }

    @Override
    public int getCalculatorsSize() {
        return calculators.size();
    }

    private final class Loader implements CacheLoader<T, Contexts> {
        @Override
        public Contexts load(T subject) {
            MutableContextSet accumulator = MutableContextSet.create();

            for (ContextCalculator<? super T> calculator : calculators) {
                try {
                    MutableContextSet ret = calculator.giveApplicableContext(subject, accumulator);
                    //noinspection ConstantConditions
                    if (ret == null) {
                        throw new IllegalStateException(calculator.getClass() + " returned a null context set");
                    }
                    accumulator = ret;
                } catch (Exception e) {
                    plugin.getLog().warn("An exception was thrown whilst calculating the context of subject " + subject);
                    e.printStackTrace();
                }
            }

            return formContexts(subject, accumulator.makeImmutable());
        }
    }

    private final class StaticLoader implements CacheLoader<Object, Contexts> {
        @Override
        public Contexts load(Object o) {
            MutableContextSet accumulator = MutableContextSet.create();

            for (StaticContextCalculator calculator : staticCalculators) {
                try {
                    MutableContextSet ret = calculator.giveApplicableContext(accumulator);
                    //noinspection ConstantConditions
                    if (ret == null) {
                        throw new IllegalStateException(calculator.getClass() + " returned a null context set");
                    }
                    accumulator = ret;
                } catch (Exception e) {
                    plugin.getLog().warn("An exception was thrown whilst calculating static contexts");
                    e.printStackTrace();
                }
            }

            return formContexts(accumulator.makeImmutable());
        }
    }

}
