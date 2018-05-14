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

import me.lucko.luckperms.api.Node;

import java.util.Objects;

/**
 * Holds a Node and plus an owning object. All calls are passed onto the contained Node instance.
 */
public final class ImmutableTransientNode<O> extends ForwardingNode implements Node {
    public static <O> ImmutableTransientNode<O> of(Node node, O owner) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(owner, "owner");
        return new ImmutableTransientNode<>(node, owner);
    }

    private final Node node;
    private final O owner;

    private ImmutableTransientNode(Node node, O owner) {
        this.node = node;
        this.owner = owner;
    }

    @Override
    public Node delegate() {
        return this.node;
    }

    public Node getNode() {
        return this.node;
    }

    public O getOwner() {
        return this.owner;
    }

    @Override
    public String toString() {
        return "ImmutableTransientNode(node=" + this.getNode() + ", owner=" + this.getOwner() + ")";
    }
}
