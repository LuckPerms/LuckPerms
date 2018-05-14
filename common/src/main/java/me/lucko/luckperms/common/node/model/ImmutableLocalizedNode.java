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

import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;

import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * Holds a Node and where it was inherited from. All calls are passed onto the contained Node instance.
 */
public final class ImmutableLocalizedNode extends ForwardingNode implements LocalizedNode {
    public static ImmutableLocalizedNode of(Node node, String location) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(location, "location");

        return new ImmutableLocalizedNode(node, location);
    }

    private final Node node;
    private final String location;

    private ImmutableLocalizedNode(Node node, String location) {
        this.node = node;
        this.location = location;
    }

    @Override
    public Node delegate() {
        return this.node;
    }

    @Nonnull
    @Override
    public Node getNode() {
        return this.node;
    }

    @Nonnull
    @Override
    public String getLocation() {
        return this.location;
    }

    @Override
    public String toString() {
        return "ImmutableLocalizedNode(node=" + this.getNode() + ", location=" + this.getLocation() + ")";
    }
}
