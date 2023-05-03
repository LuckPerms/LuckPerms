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

package me.lucko.luckperms.common.node.types;

import me.lucko.luckperms.common.node.AbstractNode;
import me.lucko.luckperms.common.node.AbstractNodeBuilder;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.node.metadata.NodeMetadataKey;
import net.luckperms.api.node.types.WeightNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;
import java.util.Map;

public class Weight extends AbstractNode<WeightNode, WeightNode.Builder> implements WeightNode {
    public static final String NODE_KEY = "weight";
    public static final String NODE_MARKER = NODE_KEY + ".";

    public static String key(int weight) {
        return NODE_MARKER + weight;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(int weight) {
        return builder().weight(weight);
    }

    private final int weight;

    public Weight(int weight, boolean value, long expireAt, ImmutableContextSet contexts, Map<NodeMetadataKey<?>, Object> metadata) {
        super(key(weight), value, expireAt, contexts, metadata);
        this.weight = weight;
    }

    @Override
    public int getWeight() {
        return this.weight;
    }

    @Override
    public @NonNull Builder toBuilder() {
        return new Builder(this.weight, this.value, this.expireAt, this.contexts, this.metadata);
    }

    public static @Nullable Builder parse(String key) {
        if (!key.toLowerCase(Locale.ROOT).startsWith(NODE_MARKER)) {
            return null;
        }

        try {
            return builder()
                    .weight(Integer.parseInt(key.substring(NODE_MARKER.length())));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static final class Builder extends AbstractNodeBuilder<WeightNode, WeightNode.Builder> implements WeightNode.Builder {

        private Integer weight;

        private Builder() {
            this.weight = null;
        }

        public Builder(int weight, boolean value, long expireAt, ImmutableContextSet context, Map<NodeMetadataKey<?>, Object> metadata) {
            super(value, expireAt, context, metadata);
            this.weight = weight;
        }

        @Override
        public @NonNull Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        @Override
        public @NonNull Weight build() {
            ensureDefined(this.weight, "weight");
            return new Weight(this.weight, this.value, this.expireAt, this.context.build(), this.metadata);
        }
    }
}
