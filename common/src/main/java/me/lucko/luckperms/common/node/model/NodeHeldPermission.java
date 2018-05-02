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

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ContextSet;

import java.util.Optional;
import java.util.OptionalLong;

import javax.annotation.Nonnull;

public final class NodeHeldPermission<T extends Comparable<T>> implements HeldPermission<T> {
    public static <T extends Comparable<T>> NodeHeldPermission<T> of(T holder, NodeDataContainer node) {
        return of(holder, node.toNode());
    }

    public static <T extends Comparable<T>> NodeHeldPermission<T> of(T holder, Node node) {
        return new NodeHeldPermission<>(holder, node);
    }

    private final T holder;
    private final Node node;

    private NodeHeldPermission(T holder, Node node) {
        this.holder = holder;
        this.node = node;
    }

    @Nonnull
    @Override
    public String getPermission() {
        return this.node.getPermission();
    }

    @Override
    public boolean getValue() {
        return this.node.getValue();
    }

    @Nonnull
    @Override
    public Optional<String> getServer() {
        return this.node.getServer();
    }

    @Nonnull
    @Override
    public Optional<String> getWorld() {
        return this.node.getWorld();
    }

    @Override
    public OptionalLong getExpiry() {
        return this.node.isTemporary() ? OptionalLong.of(this.node.getExpiryUnixTime()) : OptionalLong.empty();
    }

    @Override
    public ContextSet getContexts() {
        return this.node.getContexts();
    }

    @Nonnull
    @Override
    public Node asNode() {
        return this.node;
    }

    @Nonnull
    @Override
    public T getHolder() {
        return this.holder;
    }

    public Node getNode() {
        return this.node;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof NodeHeldPermission)) return false;
        final NodeHeldPermission other = (NodeHeldPermission) o;
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
