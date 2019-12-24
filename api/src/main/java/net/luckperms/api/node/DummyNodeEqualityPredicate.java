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

package net.luckperms.api.node;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Dummy implementation of {@link NodeEqualityPredicate}, used for the given constant
 * implementations.
 *
 * <p>The implementation rule of not calling {@link Node#equals(Node, NodeEqualityPredicate)} is
 * intentionally disregarded by this dummy implementation. The equals method has a special case for
 * the dummy instances, preventing a stack overflow.</p>
 */
final class DummyNodeEqualityPredicate implements NodeEqualityPredicate {
    private final String name;

    DummyNodeEqualityPredicate(String name) {
        this.name = name;
    }

    @Override
    public boolean areEqual(@NonNull Node o1, @NonNull Node o2) {
        return o1.equals(o2, this);
    }

    @Override
    public String toString() {
        return "NodeEqualityPredicate#" + this.name;
    }
}
