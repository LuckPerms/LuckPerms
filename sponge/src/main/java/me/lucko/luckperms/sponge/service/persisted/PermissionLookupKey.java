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

public final class PermissionLookupKey {

    public static PermissionLookupKey of(String node, ImmutableContextSet contexts) {
        return new PermissionLookupKey(node, contexts);
    }

    private final String node;
    private final ImmutableContextSet contexts;

    private PermissionLookupKey(String node, ImmutableContextSet contexts) {
        this.node = node;
        this.contexts = contexts;
    }

    public String getNode() {
        return this.node;
    }

    public ImmutableContextSet getContexts() {
        return this.contexts;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof PermissionLookupKey)) return false;
        final PermissionLookupKey other = (PermissionLookupKey) o;

        return this.getNode().equals(other.getNode()) && this.getContexts().equals(other.getContexts());
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getNode().hashCode();
        result = result * PRIME + this.getContexts().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PermissionLookupKey(node=" + this.getNode() + ", contexts=" + this.getContexts() + ")";
    }
}
