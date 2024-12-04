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

import me.lucko.luckperms.common.cache.ExpiringCache;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.context.calculator.ForwardingContextCalculator;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.context.StaticContextCalculator;
import net.luckperms.api.query.QueryOptions;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Base implementation of {@link ContextManager} which caches content lookups.
 *
 * @param <S> the subject type
 * @param <P> the player type
 */
public abstract class ContextManager<S, P extends S> {

    protected final LuckPermsPlugin plugin;
    private final Class<S> subjectClass;
    private final Class<P> playerClass;

    private final CalculatorList calculators = new CalculatorList();

    // caches static context lookups
    private final StaticLookupCache staticLookupCache = new StaticLookupCache();

    protected ContextManager(LuckPermsPlugin plugin, Class<S> subjectClass, Class<P> playerClass) {
        this.plugin = plugin;
        this.subjectClass = subjectClass;
        this.playerClass = playerClass;
    }

    public Class<S> getSubjectClass() {
        return this.subjectClass;
    }

    public Class<P> getPlayerClass() {
        return this.playerClass;
    }

    public abstract UUID getUniqueId(P player);

    public abstract QueryOptionsSupplier getCacheFor(S subject);

    public QueryOptions getQueryOptions(S subject) {
        return getCacheFor(subject).getQueryOptions();
    }

    public ImmutableContextSet getContext(S subject) {
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

    public abstract QueryOptions formQueryOptions(S subject, ImmutableContextSet contextSet);

    public void signalContextUpdate(S subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }

        // invalidate their cache
        invalidateCache(subject);

        // call event
        this.plugin.getEventDispatcher().dispatchContextUpdate(subject);
    }

    protected abstract void invalidateCache(S subject);

    public void registerCalculator(ContextCalculator<? super S> calculator) {
        String calculatorClass = calculator.getClass().getName();

        Set<Predicate<String>> disabledCalculators = this.plugin.getConfiguration().get(ConfigKeys.DISABLED_CONTEXT_CALCULATORS);
        for (Predicate<String> disabledPattern : disabledCalculators) {
            if (disabledPattern.test(calculatorClass)) {
                this.plugin.getLogger().info("Ignoring registration of disabled context calculator: " + calculatorClass);
                return;
            }
        }

        this.calculators.add(calculator);
    }

    public void unregisterCalculator(ContextCalculator<? super S> calculator) {
        this.calculators.remove(calculator);
    }

    protected void callContextCalculator(ContextCalculator<? super S> calculator, S subject, ContextConsumer consumer) {
        try {
            calculator.calculate(subject, consumer);
        } catch (Throwable e) {
            this.plugin.getLogger().warn("An exception was thrown by " + getCalculatorClass(calculator) + " whilst calculating the context of subject " + subject, e);
        }
    }

    protected void callStaticContextCalculator(StaticContextCalculator calculator, ContextConsumer consumer) {
        try {
            calculator.calculate(consumer);
        } catch (Throwable e) {
            this.plugin.getLogger().warn("An exception was thrown by " + getCalculatorClass(calculator) + " whilst calculating static contexts", e);
        }
    }

    protected QueryOptions calculate(S subject) {
        ImmutableContextSet.Builder accumulator = new ImmutableContextSetImpl.BuilderImpl();
        ContextConsumer consumer = accumulator::add;

        for (ContextCalculator<? super S> calculator : this.calculators.calculators()) {
            callContextCalculator(calculator, subject, consumer);
        }

        return formQueryOptions(subject, accumulator.build());
    }

    private QueryOptions calculateStatic() {
        ImmutableContextSet.Builder accumulator = new ImmutableContextSetImpl.BuilderImpl();
        ContextConsumer consumer = accumulator::add;

        for (StaticContextCalculator calculator : this.calculators.staticCalculators()) {
            callStaticContextCalculator(calculator, consumer);
        }

        return formQueryOptions(accumulator.build());
    }

    public ImmutableContextSet getPotentialContexts() {
        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();

        for (ContextCalculator<? super S> calculator : this.calculators.calculators()) {
            ContextSet potentialContexts;
            try {
                potentialContexts = calculator.estimatePotentialContexts();
            } catch (Throwable e) {
                this.plugin.getLogger().warn("An exception was thrown by " + getCalculatorClass(calculator) + " whilst estimating potential contexts", e);
                continue;
            }
            builder.addAll(potentialContexts);
        }

        return builder.build();
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
        if (calculator instanceof ForwardingContextCalculator) {
            calculatorClass = ((ForwardingContextCalculator<?>) calculator).delegate().getClass();
        } else {
            calculatorClass = calculator.getClass();
        }
        return calculatorClass.getName();
    }

    private final class CalculatorList {
        private final List<ContextCalculator<? super S>> calculators;
        private final List<StaticContextCalculator> staticCalculators;

        private volatile ContextCalculator<? super S>[] calculatorsArray;
        private volatile StaticContextCalculator[] staticCalculatorsArray;

        CalculatorList() {
            this.calculators = new ArrayList<>();
            this.staticCalculators = new ArrayList<>();
            bake();
        }

        @SuppressWarnings("unchecked")
        private void bake() {
            this.calculatorsArray = this.calculators.toArray(new ContextCalculator[0]);
            this.staticCalculatorsArray = this.staticCalculators.toArray(new StaticContextCalculator[0]);
        }

        public void add(ContextCalculator<? super S> calculator) {
            synchronized (this) {
                // calculators registered first should have priority (and be checked last.)
                this.calculators.add(0, calculator);

                if (calculator instanceof StaticContextCalculator) {
                    StaticContextCalculator staticCalculator = (StaticContextCalculator) calculator;
                    this.staticCalculators.add(0, staticCalculator);
                }

                bake();
            }
        }

        public void remove(ContextCalculator<? super S> calculator) {
            synchronized (this) {
                this.calculators.remove(calculator);
                if (calculator instanceof StaticContextCalculator) {
                    this.staticCalculators.remove(calculator);
                }

                bake();
            }
        }

        public ContextCalculator<? super S>[] calculators() {
            return this.calculatorsArray;
        }

        public StaticContextCalculator[] staticCalculators() {
            return this.staticCalculatorsArray;
        }
    }

}
