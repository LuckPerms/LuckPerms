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
import com.github.benmanes.caffeine.cache.RemovalListener;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public abstract class ContextManager<T> {

    private final List<ContextCalculator<T>> calculators = new CopyOnWriteArrayList<>();
    private final List<ContextCalculator<?>> staticCalculators = new CopyOnWriteArrayList<>();

    private final LoadingCache<T, ImmutableContextSet> activeContextCache = Caffeine.newBuilder()
            .weakKeys()
            .expireAfterWrite(50L, TimeUnit.MILLISECONDS)
            .removalListener((RemovalListener<T, ImmutableContextSet>) (t, contextSet, removalCause) -> invalidateContextsCache(t))
            .build(t -> calculateApplicableContext(t, MutableContextSet.create()).makeImmutable());

    private final LoadingCache<T, Contexts> contextsCache = Caffeine.newBuilder()
            .weakKeys()
            .build(t -> formContexts(t, getApplicableContext(t)));

    public ImmutableContextSet getApplicableContext(T subject) {
        return activeContextCache.get(subject);
    }

    public Contexts getApplicableContexts(T subject) {
        return contextsCache.get(subject);
    }

    public abstract Contexts formContexts(T t, ImmutableContextSet contextSet);

    private MutableContextSet calculateApplicableContext(T subject, MutableContextSet accumulator) {
        for (ContextCalculator<T> calculator : calculators) {
            try {
                calculator.giveApplicableContext(subject, accumulator);
            } catch (Exception e) {
                new RuntimeException("Exception thrown by ContextCalculator: " + calculator.getClass().getName(), e).printStackTrace();
            }

        }
        return accumulator;
    }

    private void invalidateContextsCache(T t) {
        contextsCache.invalidate(t);
    }

    public void registerCalculator(ContextCalculator<T> calculator) {
        registerCalculator(calculator, false);
    }

    public void registerCalculator(ContextCalculator<T> calculator, boolean isStatic) {
        // calculators registered first should have priority (and be checked last.)
        calculators.add(0, calculator);

        if (isStatic) {
            staticCalculators.add(0, calculator);
        }
    }

    public ImmutableContextSet getStaticContexts() {
        MutableContextSet accumulator = MutableContextSet.create();
        for (ContextCalculator<?> calculator : staticCalculators) {
            calculator.giveApplicableContext(null, accumulator);
        }
        return accumulator.makeImmutable();
    }

    public void invalidateCache(T subject){
        activeContextCache.invalidate(subject);
    }

    public int getCalculatorsSize() {
        return calculators.size();
    }
}
