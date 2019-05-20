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

package me.lucko.luckperms.common.node;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.DefaultContextKeys;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.node.NodeBuilder;
import me.lucko.luckperms.api.node.ScopedNode;
import me.lucko.luckperms.api.node.metadata.NodeMetadata;
import me.lucko.luckperms.api.node.metadata.NodeMetadataKey;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractNodeBuilder<N extends ScopedNode<N, B>, B extends NodeBuilder<N, B>> implements NodeBuilder<N, B> {
    protected boolean value;
    protected long expireAt;
    protected ImmutableContextSet.Builder context;
    protected Map<NodeMetadataKey<?>, NodeMetadata> metadata;

    protected AbstractNodeBuilder() {
        this.value = true;
        this.expireAt = 0L;
        this.context = ImmutableContextSet.builder();
        this.metadata = new IdentityHashMap<>();
    }

    protected AbstractNodeBuilder(boolean value, long expireAt, ImmutableContextSet context, Map<NodeMetadataKey<?>, NodeMetadata> metadata) {
        this.value = value;
        this.expireAt = expireAt;
        this.context = ImmutableContextSet.builder().addAll(context);
        this.metadata = new IdentityHashMap<>(metadata);
    }

    @Override
    public @NonNull B value(boolean value) {
        this.value = value;
        return (B) this;
    }

    @Override
    public @NonNull B negated(boolean negated) {
        this.value = !negated;
        return (B) this;
    }

    @Override
    public @NonNull B expiry(long expiryUnixTimestamp) {
        this.expireAt = expiryUnixTimestamp;
        return (B) this;
    }

    @Override
    public @NonNull B clearExpiry() {
        this.expireAt = 0L;
        return (B) this;
    }

    @Override
    public @NonNull B context(@NonNull ContextSet contextSet) {
        Objects.requireNonNull(contextSet, "contextSet");
        this.context = ImmutableContextSet.builder().addAll(contextSet);
        return (B) this;
    }

    @Override
    public @NonNull B withContext(@NonNull String key, @NonNull String value) {
        // TODO reconsider a better place to insert / avoid this special case
        if ((key.equalsIgnoreCase(DefaultContextKeys.SERVER_KEY) || key.equalsIgnoreCase(DefaultContextKeys.WORLD_KEY)) && value.equalsIgnoreCase("global")) {
            return (B) this;
        }
        this.context.add(key, value);
        return (B) this;
    }

    @Override
    public @NonNull B withContext(@NonNull ContextSet contextSet) {
        this.context.addAll(contextSet);
        return (B) this;
    }

    @Override
    public @NonNull <T extends NodeMetadata> B withMetadata(@NonNull NodeMetadataKey<T> key, @Nullable T metadata) {
        Objects.requireNonNull(key, "key");
        if (metadata == null) {
            this.metadata.remove(key);
        } else {
            this.metadata.put(key, metadata);
        }
        return (B) this;
    }

}
