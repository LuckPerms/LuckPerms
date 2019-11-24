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

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

final class SimpleNodeType<T extends Node> implements NodeType<T> {
    private final String name;
    private final Predicate<Node> matches;
    private final Function<Node, T> cast;

    SimpleNodeType(String name, Predicate<Node> matches, Function<Node, T> cast) {
        this.name = name;
        this.matches = matches;
        this.cast = cast;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public boolean matches(Node node) {
        Objects.requireNonNull(node, "node");
        return this.matches.test(node);
    }

    @Override
    public T cast(Node node) {
        if (!matches(node)) {
            throw new IllegalArgumentException("Node " + node.getClass() + " does not match " + this.name);
        }
        return this.cast.apply(node);
    }

    @Override
    public String toString() {
        return name();
    }
}
