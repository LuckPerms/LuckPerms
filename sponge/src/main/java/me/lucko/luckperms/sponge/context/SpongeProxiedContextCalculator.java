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

package me.lucko.luckperms.sponge.context;

import me.lucko.luckperms.api.context.ContextConsumer;
import me.lucko.luckperms.common.context.ProxiedContextCalculator;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class SpongeProxiedContextCalculator implements ProxiedContextCalculator<Subject> {
    private final ContextCalculator<Subject> delegate;

    public SpongeProxiedContextCalculator(ContextCalculator<Subject> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void giveApplicableContext(@NonNull Subject subject, @NonNull ContextConsumer consumer) {
        this.delegate.accumulateContexts(subject, new ProxiedContextSet(consumer));
    }

    @Override
    public Object getDelegate() {
        return this.delegate;
    }

    private static final class ProxiedContextSet implements Set<Context> {
        private final ContextConsumer consumer;

        private ProxiedContextSet(ContextConsumer consumer) {
            this.consumer = consumer;
        }

        @Override
        public boolean add(Context context) {
            if (context.getKey().isEmpty() || context.getValue().isEmpty()) {
                return false;
            }
            this.consumer.accept(context.getKey(), context.getValue());
            return true;
        }

        @Override
        public boolean addAll(@NonNull Collection<? extends Context> c) {
            for (Context context : c) {
                add(context);
            }
            return true;
        }

        @Override public int size() { throw new UnsupportedOperationException(); }
        @Override public boolean isEmpty() { throw new UnsupportedOperationException(); }
        @Override public boolean contains(Object o) { throw new UnsupportedOperationException(); }
        @Override public @NonNull Iterator<Context> iterator() { throw new UnsupportedOperationException(); }
        @Override public @NonNull Object[] toArray() { throw new UnsupportedOperationException(); }
        @Override public @NonNull <T> T[] toArray(T[] a) { throw new UnsupportedOperationException(); }
        @Override public boolean remove(Object o) { throw new UnsupportedOperationException(); }
        @Override public boolean containsAll(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public boolean retainAll(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public boolean removeAll(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public void clear() { throw new UnsupportedOperationException(); }
    }

}
