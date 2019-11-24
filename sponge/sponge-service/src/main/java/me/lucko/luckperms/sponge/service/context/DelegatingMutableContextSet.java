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

import me.lucko.luckperms.common.context.contextset.ContextImpl;

import net.luckperms.api.context.MutableContextSet;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.service.context.Context;

import java.util.Iterator;
import java.util.Set;

/**
 * Implements a {@link Set} of {@link Context}s, delegating all calls to a {@link MutableContextSet}.
 */
public class DelegatingMutableContextSet extends AbstractDelegatingContextSet {
    private final MutableContextSet delegate;

    public DelegatingMutableContextSet(MutableContextSet delegate) {
        this.delegate = delegate;
    }

    @Override
    public MutableContextSet getDelegate() {
        return this.delegate;
    }

    @Override
    public @NonNull Iterator<Context> iterator() {
        return new ContextSetIterator();
    }

    @Override
    public boolean add(Context context) {
        if (context == null) {
            throw new NullPointerException("context");
        }
        if (context.getKey().isEmpty() || context.getValue().isEmpty()) {
            return false;
        }

        boolean has = this.delegate.contains(context.getKey(), context.getValue());
        this.delegate.add(new ContextImpl(context.getKey(), context.getValue()));
        return !has;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof Context) {
            Context context = (Context) o;
            if (context.getKey().isEmpty() || context.getValue().isEmpty()) {
                return false;
            }
            boolean had = this.delegate.contains(context.getKey(), context.getValue());
            this.delegate.remove(context.getKey(), context.getValue());
            return had;
        }

        return false;
    }

    @Override
    public void clear() {
        this.delegate.clear();
    }

    @Override
    public String toString() {
        return "DelegatingMutableContextSet(delegate=" + this.getDelegate() + ")";
    }

    private final class ContextSetIterator implements Iterator<Context> {
        private final Iterator<net.luckperms.api.context.Context> it = DelegatingMutableContextSet.this.delegate.iterator();
        private Context current;

        @Override
        public boolean hasNext() {
            return this.it.hasNext();
        }

        @Override
        public Context next() {
            net.luckperms.api.context.Context next = this.it.next();

            // track the iterators cursor to handle #remove calls
            this.current = new Context(next.getKey(), next.getValue());
            return this.current;
        }

        @Override
        public void remove() {
            Context c = this.current;
            if (c == null) {
                throw new IllegalStateException();
            }
            this.current = null;

            // delegate the removal call to the MutableContextSet, as the iterator returned by
            // toSet().iterator() is immutable
            DelegatingMutableContextSet.this.delegate.remove(c.getKey(), c.getValue());
        }
    }
}
