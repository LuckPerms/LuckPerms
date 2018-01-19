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

package me.lucko.luckperms.sponge.service.context;

import me.lucko.luckperms.api.context.ImmutableContextSet;

import org.spongepowered.api.service.context.Context;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * Implements a {@link Set} of {@link Context}s, delegating all calls to a {@link ImmutableContextSet}.
 */
public class DelegatingImmutableContextSet extends AbstractDelegatingContextSet {
    private final ImmutableContextSet delegate;

    public DelegatingImmutableContextSet(ImmutableContextSet delegate) {
        this.delegate = delegate;
    }

    @Override
    public ImmutableContextSet getDelegate() {
        return this.delegate;
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof Context) {
            Context context = (Context) o;
            return this.delegate.has(context);
        }
        return false;
    }

    @Nonnull
    @Override
    public Iterator<Context> iterator() {
        return new ContextSetIterator();
    }

    @Override
    public boolean add(Context context) {
        throw new UnsupportedOperationException("context set is immutable");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("context set is immutable");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("context set is immutable");
    }

    @Override
    public String toString() {
        return "DelegatingImmutableContextSet(delegate=" + this.getDelegate() + ")";
    }

    private final class ContextSetIterator implements Iterator<Context> {
        private final Iterator<Map.Entry<String, String>> it = DelegatingImmutableContextSet.this.delegate.toSet().iterator();

        @Override
        public boolean hasNext() {
            return this.it.hasNext();
        }

        @Override
        public Context next() {
            Map.Entry<String, String> next = this.it.next();
            return new Context(next.getKey(), next.getValue());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("context set is immutable");
        }
    }
}
