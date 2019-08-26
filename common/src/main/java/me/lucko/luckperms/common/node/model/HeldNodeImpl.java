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

package me.lucko.luckperms.common.node.model;

import net.luckperms.api.node.HeldNode;
import net.luckperms.api.node.Node;

import org.checkerframework.checker.nullness.qual.NonNull;

public final class HeldNodeImpl<T extends Comparable<T>> implements HeldNode<T> {

    public static <T extends Comparable<T>> HeldNodeImpl<T> of(T holder, Node node) {
        return new HeldNodeImpl<>(holder, node);
    }

    private final T holder;
    private final Node node;

    private HeldNodeImpl(T holder, Node node) {
        this.holder = holder;
        this.node = node;
    }

    @Override
    public @NonNull Node getNode() {
        return this.node;
    }

    @Override
    public @NonNull T getHolder() {
        return this.holder;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof HeldNodeImpl)) return false;
        final HeldNodeImpl other = (HeldNodeImpl) o;
        return this.getHolder().equals(other.getHolder()) && this.getNode().equals(other.getNode());
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getHolder().hashCode();
        result = result * PRIME + this.getNode().hashCode();
        return result;
    }
}
