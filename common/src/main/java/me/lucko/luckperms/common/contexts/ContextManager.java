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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Manages {@link ContextCalculator}s, and calculates applicable contexts for a
 * given type.
 *
 * @param <T> the calculator type
 */
public abstract class ContextManager<T> {

    private final List<ContextCalculator<T>> calculators = new CopyOnWriteArrayList<>();
    private final List<ContextCalculator<?>> staticCalculators = new CopyOnWriteArrayList<>();

    // caches context lookups
    private final LoadingCache<T, Contexts> lookupCache = Caffeine.newBuilder()
            .weakKeys()
            .expireAfterWrite(50L, TimeUnit.MILLISECONDS) // expire roughly every tick
            .build(subject -> {
                MutableContextSet accumulator = MutableContextSet.create();
                calculateApplicableContext(subject, accumulator);

                ImmutableContextSet ret = accumulator.makeImmutable();
                return formContexts(subject, ret);
            });


    /**
     * Queries the ContextManager for current context values for the subject.
     *
     * @param subject the subject
     * @return the applicable context for the subject
     */
    public ImmutableContextSet getApplicableContext(@NonNull T subject) {
        // this is actually already immutable, but the Contexts method signature returns the interface.
        return  getApplicableContexts(subject).getContexts().makeImmutable();
    }

    /**
     * Queries the ContextManager for current context values for the subject.
     *
     * @param subject the subject
     * @return the applicable context for the subject
     */
    public Contexts getApplicableContexts(@NonNull T subject) {
        return lookupCache.get(subject);
    }

    /**
     * Forms a {@link Contexts} instance from an {@link ImmutableContextSet}.
     *
     * @param subject the subject
     * @param contextSet the context set
     * @return a contexts instance
     */
    public abstract Contexts formContexts(T subject, ImmutableContextSet contextSet);

    /**
     * Registers a context calculator with the manager.
     *
     * @param calculator the calculator
     */
    public void registerCalculator(ContextCalculator<T> calculator) {
        registerCalculator(calculator, false);
    }

    /**
     * Registers a context calculator with the manager.
     *
     * @param calculator the calculator
     * @param isStatic if the calculator is static. (if it allows a null subject parameter)
     */
    public void registerCalculator(ContextCalculator<T> calculator, boolean isStatic) {
        // calculators registered first should have priority (and be checked last.)
        calculators.add(0, calculator);

        if (isStatic) {
            staticCalculators.add(0, calculator);
        }
    }

    /**
     * Gets the contexts from the static calculators in this manager.
     *
     * @return the current active static contexts
     */
    public ImmutableContextSet getStaticContexts() {
        MutableContextSet accumulator = MutableContextSet.create();
        for (ContextCalculator<?> calculator : staticCalculators) {
            calculator.giveApplicableContext(null, accumulator);
        }
        return accumulator.makeImmutable();
    }

    /**
     * Invalidates the lookup cache for a given subject
     *
     * @param subject the subject
     */
    public void invalidateCache(@NonNull T subject){
        lookupCache.invalidate(subject);
    }

    /**
     * Gets the number of calculators registered with the manager.
     *
     * @return the number of calculators registered
     */
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

}
