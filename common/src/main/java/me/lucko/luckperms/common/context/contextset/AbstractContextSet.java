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

package me.lucko.luckperms.common.context.contextset;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;

import net.luckperms.api.context.ContextSet;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

abstract class AbstractContextSet implements ContextSet {

    protected abstract SetMultimap<String, String> backing();

    protected abstract void copyTo(SetMultimap<String, String> other);

    @Override
    public boolean containsKey(@NonNull String key) {
        return backing().containsKey(sanitizeKey(key));
    }

    @Override
    public @NonNull Set<String> getValues(@NonNull String key) {
        Collection<String> values = backing().asMap().get(sanitizeKey(key));
        return values != null ? ImmutableSet.copyOf(values) : ImmutableSet.of();
    }

    @Override
    public boolean contains(@NonNull String key, @NonNull String value) {
        return backing().containsEntry(sanitizeKey(key), sanitizeValue(value));
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
    public int hashCode() {
        return backing().hashCode();
    }

    static String sanitizeKey(String key) {
        Objects.requireNonNull(key, "key is null");
        if (stringIsEmpty(key)) {
            throw new IllegalArgumentException("key is (effectively) empty");
        }
        return key.toLowerCase();
    }

    static String sanitizeValue(String value) {
        Objects.requireNonNull(value, "value is null");
        if (stringIsEmpty(value)) {
            throw new IllegalArgumentException("value is (effectively) empty");
        }
        return value.toLowerCase();
    }

    private static boolean stringIsEmpty(String s) {
        if (s.isEmpty()) {
            return true;
        }
        for (char c : s.toCharArray()) {
            if (c != ' ') {
                return false;
            }
        }
        return true;
    }

}
