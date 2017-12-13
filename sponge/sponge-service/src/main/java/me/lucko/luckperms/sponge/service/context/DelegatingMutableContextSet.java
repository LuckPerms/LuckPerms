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

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.api.context.MutableContextSet;

import org.spongepowered.api.service.context.Context;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Implements a {@link Set} of {@link Context}s, delegating all calls to a {@link MutableContextSet}.
 */
@RequiredArgsConstructor
public class DelegatingMutableContextSet extends AbstractDelegatingContextSet {
    private final MutableContextSet delegate;

    @Override
    public MutableContextSet getDelegate() {
        return delegate;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof Context) {
            Context context = (Context) o;
            return delegate.has(context);
        }
        return false;
    }

    @Override
    public Iterator<Context> iterator() {
        return new ContextSetIterator();
    }

    @Override
    public boolean add(Context context) {
        if (context == null) {
            throw new NullPointerException("context");
        }

        boolean has = delegate.has(context);
        delegate.add(context);
        return !has;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof Context) {
            Context context = (Context) o;
            boolean had = delegate.has(context);
            delegate.remove(context.getKey(), context.getValue());
            return had;
        }

        return false;
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    private final class ContextSetIterator implements Iterator<Context> {
        private final Iterator<Map.Entry<String, String>> it = delegate.toSet().iterator();
        private Context current;

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Context next() {
            Map.Entry<String, String> next = it.next();

            // track the iterators cursor to handle #remove calls
            current = new Context(next.getKey(), next.getValue());
            return current;
        }

        @Override
        public void remove() {
            Context c = current;
            if (c == null) {
                throw new IllegalStateException();
            }
            current = null;

            // delegate the removal call to the MutableContextSet, as the iterator returned by
            // toSet().iterator() is immutable
            delegate.remove(c.getKey(), c.getValue());
        }
    }
}
