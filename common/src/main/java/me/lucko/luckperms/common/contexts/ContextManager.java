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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.caching.MetaContexts;
import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.api.context.StaticContextCalculator;
import me.lucko.luckperms.common.buffers.ExpiringCache;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

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

    // caches the creation of cache instances. cache-ception.
    // we want to encourage re-use of these instances, it's faster that way
    private final LoadingCache<T, ContextsCache<T>> subjectCaches = Caffeine.newBuilder()
            .weakKeys()
            .build(key -> new ContextsCache<>(key, this));

    // caches static context lookups
    private final StaticLookupCache staticLookupCache = new StaticLookupCache();

    protected ContextManager(LuckPermsPlugin plugin, Class<T> subjectClass) {
        this.plugin = plugin;
        this.subjectClass = subjectClass;
    }

    /**
     * Gets the calculators registered on the platform
     *
     * @return the registered calculators
     */
    public List<ContextCalculator<? super T>> getCalculators() {
        return ImmutableList.copyOf(this.calculators);
    }

    /**
     * Gets the static calculators registered on the platform
     *
     * @return the registered static calculators
     */
    public List<StaticContextCalculator> getStaticCalculators() {
        return ImmutableList.copyOf(this.staticCalculators);
    }

    /**
     * Gets the class of the subject handled by this instance
     *
     * @return the subject class
     */
    public Class<T> getSubjectClass() {
        return this.subjectClass;
    }

    /**
     * Queries the ContextManager for current context values for the subject.
     *
     * @param subject the subject
     * @return the applicable context for the subject
     */
    public ImmutableContextSet getApplicableContext(T subject) {
        return getCacheFor(subject).getContextSet();
    }

    /**
      * Queries the ContextManager for current context values for the subject.
      *
      * @param subject the subject
      * @return the applicable context for the subject
      */
    public Contexts getApplicableContexts(T subject) {
        return getCacheFor(subject).getContexts();
    }

    /**
     * Gets the cache instance for the given subject.
     *
     * @param subject the subject
     * @return the cache
     */
    public ContextsCache<T> getCacheFor(T subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }
        return this.subjectCaches.get(subject);
    }

    /**
     * Gets the contexts from the static calculators in this manager.
     *
     * @return the current active static contexts
     */
    public ImmutableContextSet getStaticContext() {
        // this is actually already immutable, but the Contexts method signature returns the interface.
        // using the makeImmutable method is faster than casting
        return getStaticContexts().getContexts().makeImmutable();
    }

    /**
     * Gets the contexts from the static calculators in this manager.
     *
     * @return the current active static contexts
     */
    public Contexts getStaticContexts() {
        return this.staticLookupCache.get();
    }

    /**
     * Returns a string form of the managers static context
     *
     * <p>Returns an empty optional if the set is empty.</p>
     *
     * @return a string representation of {@link #getStaticContext()}
     */
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

    /**
     * Forms a {@link Contexts} instance from an {@link ImmutableContextSet}.
     *
     * @param contextSet the context set
     * @return a contexts instance
     */
    public Contexts formContexts(ImmutableContextSet contextSet) {
        return Contexts.of(contextSet, this.plugin.getConfiguration().get(ConfigKeys.LOOKUP_SETTINGS));
    }

    /**
     * Forms a "default" {@link MetaContexts} instance from {@link Contexts}.
     *
     * @param contexts the contexts
     * @return a contexts instance
     */
    public MetaContexts formMetaContexts(Contexts contexts) {
        return new MetaContexts(
                contexts,
                this.plugin.getConfiguration().get(ConfigKeys.PREFIX_FORMATTING_OPTIONS),
                this.plugin.getConfiguration().get(ConfigKeys.SUFFIX_FORMATTING_OPTIONS)
        );
    }

    /**
     * Registers a context calculator with the manager.
     *
     * @param calculator the calculator
     */
    public void registerCalculator(ContextCalculator<? super T> calculator) {
        // calculators registered first should have priority (and be checked last.)
        this.calculators.add(0, calculator);
    }

    /**
     * Registers a static context calculator with the manager.
     *
     * @param calculator the calculator
     */
    public void registerStaticCalculator(StaticContextCalculator calculator) {
        registerCalculator(calculator);
        this.staticCalculators.add(0, calculator);
    }

    /**
     * Invalidates the lookup cache for a given subject
     *
     * @param subject the subject
     */
    public void invalidateCache(T subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }

        ContextsCache<T> cache = this.subjectCaches.getIfPresent(subject);
        if (cache != null) {
            cache.invalidate();
        }
    }

    Contexts calculate(T subject) {
        MutableContextSet accumulator = MutableContextSet.create();

        for (ContextCalculator<? super T> calculator : ContextManager.this.calculators) {
            try {
                MutableContextSet ret = calculator.giveApplicableContext(subject, accumulator);
                //noinspection ConstantConditions
                if (ret == null) {
                    throw new IllegalStateException(calculator.getClass() + " returned a null context set");
                }
                accumulator = ret;
            } catch (Exception e) {
                ContextManager.this.plugin.getLogger().warn("An exception was thrown by " + getCalculatorClass(calculator) + " whilst calculating the context of subject " + subject);
                e.printStackTrace();
            }
        }

        return formContexts(subject, accumulator.makeImmutable());
    }

    private Contexts calculateStatic() {
        MutableContextSet accumulator = MutableContextSet.create();

        for (StaticContextCalculator calculator : this.staticCalculators) {
            try {
                MutableContextSet ret = calculator.giveApplicableContext(accumulator);
                //noinspection ConstantConditions
                if (ret == null) {
                    throw new IllegalStateException(calculator.getClass() + " returned a null context set");
                }
                accumulator = ret;
            } catch (Exception e) {
                this.plugin.getLogger().warn("An exception was thrown by " + getCalculatorClass(calculator) + " whilst calculating static contexts");
                e.printStackTrace();
            }
        }

        return formContexts(accumulator.makeImmutable());
    }

    /**
     * Forms a {@link Contexts} instance from an {@link ImmutableContextSet}.
     *
     * @param subject the subject
     * @param contextSet the context set
     * @return a contexts instance
     */
    public abstract Contexts formContexts(T subject, ImmutableContextSet contextSet);

    private final class StaticLookupCache extends ExpiringCache<Contexts> {
        StaticLookupCache() {
            super(50L, TimeUnit.MILLISECONDS);
        }

        @Nonnull
        @Override
        public Contexts supply() {
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
