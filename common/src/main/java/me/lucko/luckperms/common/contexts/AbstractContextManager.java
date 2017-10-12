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
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * An abstract implementation of {@link ContextManager} which caches content lookups.
 *
 * @param <T> the calculator type
 */
public abstract class AbstractContextManager<T> implements ContextManager<T> {

    protected final LuckPermsPlugin plugin;
    private final List<ContextCalculator<T>> calculators = new CopyOnWriteArrayList<>();
    private final List<ContextCalculator<?>> staticCalculators = new CopyOnWriteArrayList<>();

    // caches context lookups
    private final LoadingCache<T, Contexts> lookupCache = Caffeine.newBuilder()
            .weakKeys()
            .expireAfterWrite(50L, TimeUnit.MILLISECONDS) // expire roughly every tick
            .build(new Loader());

    protected AbstractContextManager(LuckPermsPlugin plugin) {
        this.plugin = plugin;
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
        MutableContextSet accumulator = MutableContextSet.create();
        for (ContextCalculator<?> calculator : staticCalculators) {
            calculator.giveApplicableContext(null, accumulator);
        }
        return accumulator.makeImmutable();
    }

    @Override
    public Contexts getStaticContexts() {
        return formContexts(getStaticContext());
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
    public void registerCalculator(ContextCalculator<T> calculator, boolean isStatic) {
        // calculators registered first should have priority (and be checked last.)
        calculators.add(0, calculator);

        if (isStatic) {
            staticCalculators.add(0, calculator);
        }
    }

    @Override
    public void invalidateCache(@NonNull T subject){
        lookupCache.invalidate(subject);
    }

    @Override
    public int getCalculatorsSize() {
        return calculators.size();
    }

    // iterates the calculators in this manager and accumulates contexts from them all.
    private void calculateApplicableContext(T subject, MutableContextSet accumulator) {
        for (ContextCalculator<T> calculator : calculators) {
            try {
                calculator.giveApplicableContext(subject, accumulator);
            } catch (Exception e) {
                new RuntimeException("Exception thrown by ContextCalculator: " + calculator.getClass().getName(), e).printStackTrace();
            }
        }
    }

    private final class Loader implements CacheLoader<T, Contexts> {

        @Override
        public Contexts load(T subject) {
            MutableContextSet accumulator = MutableContextSet.create();
            calculateApplicableContext(subject, accumulator);

            ImmutableContextSet ret = accumulator.makeImmutable();
            return formContexts(subject, ret);
        }
    }

}
