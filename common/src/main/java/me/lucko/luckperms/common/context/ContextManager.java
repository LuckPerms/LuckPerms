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

import me.lucko.luckperms.common.cache.ExpiringCache;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.context.StaticContextCalculator;
import net.luckperms.api.query.QueryOptions;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Base implementation of {@link ContextManager} which caches content lookups.
 *
 * @param <T> the calculator type
 */
public abstract class ContextManager<T> {

    protected final LuckPermsPlugin plugin;
    private final Class<T> subjectClass;

    private final List<ContextCalculator<? super T>> calculators = new CopyOnWriteArrayList<>();
    private final List<StaticContextCalculator> staticCalculators = new CopyOnWriteArrayList<>();

    // caches static context lookups
    private final StaticLookupCache staticLookupCache = new StaticLookupCache();

    protected ContextManager(LuckPermsPlugin plugin, Class<T> subjectClass) {
        this.plugin = plugin;
        this.subjectClass = subjectClass;
    }

    public ImmutableContextSet getPotentialContexts() {
        ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
        for (ContextCalculator<? super T> calculator : this.calculators) {
            builder.addAll(calculator.estimatePotentialContexts());
        }
        return builder.build();
    }

    public Class<T> getSubjectClass() {
        return this.subjectClass;
    }

    public abstract QueryOptionsSupplier getCacheFor(T subject);

    public QueryOptions getQueryOptions(T subject) {
        return getCacheFor(subject).getQueryOptions();
    }

    public ImmutableContextSet getContext(T subject) {
        return getCacheFor(subject).getContextSet();
    }

    public QueryOptions getStaticQueryOptions() {
        return this.staticLookupCache.get();
    }

    public ImmutableContextSet getStaticContext() {
        return getStaticQueryOptions().context();
    }

    public QueryOptions formQueryOptions(ImmutableContextSet contextSet) {
        return this.plugin.getConfiguration().get(ConfigKeys.GLOBAL_QUERY_OPTIONS).toBuilder().context(contextSet).build();
    }

    public abstract QueryOptions formQueryOptions(T subject, ImmutableContextSet contextSet);

    public abstract void invalidateCache(T subject);

    public void registerCalculator(ContextCalculator<? super T> calculator) {
        // calculators registered first should have priority (and be checked last.)
        this.calculators.add(0, calculator);

        if (calculator instanceof StaticContextCalculator) {
            StaticContextCalculator staticCalculator = (StaticContextCalculator) calculator;
            this.staticCalculators.add(0, staticCalculator);
        }
    }

    public void unregisterCalculator(ContextCalculator<? super T> calculator) {
        this.calculators.remove(calculator);
        if (calculator instanceof StaticContextCalculator) {
            this.staticCalculators.remove(calculator);
        }
    }

    protected QueryOptions calculate(T subject) {
        ImmutableContextSet.Builder accumulator = new ImmutableContextSetImpl.BuilderImpl();

        for (ContextCalculator<? super T> calculator : this.calculators) {
            try {
                calculator.calculate(subject, accumulator::add);
            } catch (Throwable e) {
                ContextManager.this.plugin.getLogger().warn("An exception was thrown by " + getCalculatorClass(calculator) + " whilst calculating the context of subject " + subject);
                e.printStackTrace();
            }
        }

        return formQueryOptions(subject, accumulator.build());
    }

    private QueryOptions calculateStatic() {
        ImmutableContextSet.Builder accumulator = new ImmutableContextSetImpl.BuilderImpl();

        for (StaticContextCalculator calculator : this.staticCalculators) {
            try {
                calculator.calculate(accumulator::add);
            } catch (Throwable e) {
                this.plugin.getLogger().warn("An exception was thrown by " + getCalculatorClass(calculator) + " whilst calculating static contexts");
                e.printStackTrace();
            }
        }

        return formQueryOptions(accumulator.build());
    }

    private final class StaticLookupCache extends ExpiringCache<QueryOptions> {
        StaticLookupCache() {
            super(50L, TimeUnit.MILLISECONDS);
        }

        @Override
        public @NonNull QueryOptions supply() {
            return calculateStatic();
        }
    }

    private static String getCalculatorClass(ContextCalculator<?> calculator) {
        Class<?> calculatorClass;
        if (calculator instanceof ProxiedContextCalculator) {
            calculatorClass = ((ProxiedContextCalculator) calculator).getDelegate().getClass();
        } else {
            calculatorClass = calculator.getClass();
        }
        return calculatorClass.getName();
    }

}
