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

package me.lucko.luckperms.api.context;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

abstract class AbstractContextSet implements ContextSet {

    protected abstract Multimap<String, String> backing();

    @Nonnull
    @Override
    @Deprecated
    public Map<String, String> toMap() {
        ImmutableMap.Builder<String, String> m = ImmutableMap.builder();
        for (Map.Entry<String, String> e : backing().entries()) {
            m.put(e.getKey(), e.getValue());
        }
        return m.build();
    }

    @Override
    public boolean containsKey(@Nonnull String key) {
        return backing().containsKey(sanitizeKey(key));
    }

    @Nonnull
    @Override
    public Set<String> getValues(@Nonnull String key) {
        Collection<String> values = backing().asMap().get(sanitizeKey(key));
        return values != null ? ImmutableSet.copyOf(values) : ImmutableSet.of();
    }

    @Override
    public boolean has(@Nonnull String key, @Nonnull String value) {
        return backing().containsEntry(sanitizeKey(key), sanitizeValue(value));
    }

    @Override
    public boolean hasIgnoreCase(@Nonnull String key, @Nonnull String value) {
        String v = sanitizeValue(value);

        Collection<String> values = backing().asMap().get(sanitizeKey(key));
        if (values == null || values.isEmpty()) {
            return false;
        }

        if (values.contains(v)) {
            return true;
        }

        for (String val : values) {
            if (val.equalsIgnoreCase(v)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return backing().isEmpty();
    }

    @Override
    public int size() {
        return backing().size();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ContextSet)) return false;
        final ContextSet other = (ContextSet) o;

        final Multimap<String, String> otherContexts;

        if (other instanceof AbstractContextSet) {
            otherContexts = ((AbstractContextSet) other).backing();
        } else {
            otherContexts = other.toMultimap();
        }

        return backing().equals(otherContexts);
    }

    @Override
    public int hashCode() {
        return backing().hashCode();
    }

    static String sanitizeKey(String key) {
        return checkNotNull(key, "key is null").toLowerCase().intern();
    }

    static String sanitizeValue(String value) {
        return checkNotNull(value, "value is null").intern();
    }

}
