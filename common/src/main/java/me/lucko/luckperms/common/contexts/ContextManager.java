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

import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class ContextManager<T> {

    private final List<ContextCalculator<T>> calculators = new CopyOnWriteArrayList<>();
    private final List<ContextCalculator<?>> staticCalculators = new CopyOnWriteArrayList<>();

    private final LoadingCache<T, ContextSet> cache = Caffeine.newBuilder()
            .weakKeys()
            .expireAfterWrite(50L, TimeUnit.MILLISECONDS)
            .build(t -> calculateApplicableContext(t, MutableContextSet.create()).makeImmutable());

    private MutableContextSet calculateApplicableContext(T subject, MutableContextSet accumulator) {
        for (ContextCalculator<T> calculator : calculators) {
            calculator.giveApplicableContext(subject, accumulator);
        }
        return accumulator;
    }

    public ContextSet getApplicableContext(T subject) {
        return cache.get(subject);
    }

    public void registerCalculator(ContextCalculator<T> calculator) {
        // calculators registered first should have priority (and be checked last.)
        calculators.add(0, calculator);
    }

    public void registerStaticCalculator(ContextCalculator<?> calculator) {
        staticCalculators.add(0, calculator);
    }

    public ContextSet getStaticContexts() {
        MutableContextSet accumulator = MutableContextSet.create();
        for (ContextCalculator<?> calculator : staticCalculators) {
            calculator.giveApplicableContext(null, accumulator);
        }
        return accumulator.makeImmutable();
    }

    public int getCalculatorsSize() {
        return calculators.size();
    }
}
