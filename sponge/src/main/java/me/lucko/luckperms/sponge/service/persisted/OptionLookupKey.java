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

package me.lucko.luckperms.sponge.service.persisted;

import me.lucko.luckperms.api.context.ImmutableContextSet;

public final class OptionLookupKey {

    public static OptionLookupKey of(String key, ImmutableContextSet contexts) {
        return new OptionLookupKey(key, contexts);
    }

    private final String key;
    private final ImmutableContextSet contexts;

    private OptionLookupKey(String key, ImmutableContextSet contexts) {
        this.key = key;
        this.contexts = contexts;
    }

    public String getKey() {
        return this.key;
    }

    public ImmutableContextSet getContexts() {
        return this.contexts;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof OptionLookupKey)) return false;
        final OptionLookupKey other = (OptionLookupKey) o;

        return this.getKey().equals(other.getKey()) && this.getContexts().equals(other.getContexts());
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getKey().hashCode();
        result = result * PRIME + this.getContexts().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "OptionLookupKey(key=" + this.getKey() + ", contexts=" + this.getContexts() + ")";
    }
}