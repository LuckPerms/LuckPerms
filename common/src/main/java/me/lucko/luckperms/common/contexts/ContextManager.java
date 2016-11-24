/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.lucko.luckperms.api.context.ContextListener;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.IContextCalculator;
import me.lucko.luckperms.api.context.MutableContextSet;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class ContextManager<T> {

    private final List<IContextCalculator<T>> calculators = new CopyOnWriteArrayList<>();
    private final List<ContextListener<T>> listeners = new CopyOnWriteArrayList<>();

    private final LoadingCache<T, ContextSet> cache = CacheBuilder.newBuilder()
            .weakKeys()
            .expireAfterWrite(50L, TimeUnit.MILLISECONDS)
            .build(new CacheLoader<T, ContextSet>() {
                @Override
                public ContextSet load(T t) {
                    return calculateApplicableContext(t, MutableContextSet.empty()).makeImmutable();
                }
            });

    private MutableContextSet calculateApplicableContext(T subject, MutableContextSet accumulator) {
        for (IContextCalculator<T> calculator : calculators) {
            calculator.giveApplicableContext(subject, accumulator);
        }
        return accumulator;
    }

    public ContextSet getApplicableContext(T subject) {
        return cache.getUnchecked(subject);
    }

    public void registerCalculator(IContextCalculator<T> calculator) {
        listeners.forEach(calculator::addListener);
        calculators.add(calculator);
    }

    public void registerListener(ContextListener<T> listener) {
        for (IContextCalculator<T> calculator : calculators) {
            calculator.addListener(listener);
        }

        listeners.add(listener);
    }

    public int getCalculatorsSize() {
        return calculators.size();
    }
}
