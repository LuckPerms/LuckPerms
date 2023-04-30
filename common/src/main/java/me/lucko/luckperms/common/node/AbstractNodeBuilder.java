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

import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.node.NodeBuilder;
import net.luckperms.api.node.ScopedNode;
import net.luckperms.api.node.metadata.NodeMetadataKey;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unchecked")
public abstract class AbstractNodeBuilder<N extends ScopedNode<N, B>, B extends NodeBuilder<N, B>> implements NodeBuilder<N, B> {
    protected boolean value;
    protected long expireAt;
    protected ImmutableContextSet.Builder context;
    protected final Map<NodeMetadataKey<?>, Object> metadata;

    protected AbstractNodeBuilder(boolean value, long expireAt, ImmutableContextSet context, Map<NodeMetadataKey<?>, Object> metadata) {
        this.value = value;
        this.expireAt = expireAt;
        this.context = new ImmutableContextSetImpl.BuilderImpl().addAll(context);
        this.metadata = new HashMap<>(metadata);
    }

    protected AbstractNodeBuilder() {
        this(true, 0L, ImmutableContextSetImpl.EMPTY, Collections.emptyMap());
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
    public @NonNull B expiry(long expiryEpochSeconds) {
        this.expireAt = expiryEpochSeconds;
        return (B) this;
    }

    @Override
    public @NonNull B expiry(@Nullable TemporalAccessor expiry) {
        if (expiry == null) {
            return clearExpiry();
        }

        this.expireAt = expiry.getLong(ChronoField.INSTANT_SECONDS);
        return (B) this;
    }

    @Override
    public @NonNull B expiry(@Nullable TemporalAmount duration) {
        if (duration == null) {
            return clearExpiry();
        }

        this.expireAt = Instant.now().truncatedTo(ChronoUnit.SECONDS).plus(duration).getEpochSecond();
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
        this.context = new ImmutableContextSetImpl.BuilderImpl().addAll(contextSet);
        return (B) this;
    }

    @Override
    public @NonNull B withContext(@NonNull String key, @NonNull String value) {
        this.context.add(key, value);
        return (B) this;
    }

    @Override
    public @NonNull B withContext(@NonNull ContextSet contextSet) {
        this.context.addAll(contextSet);
        return (B) this;
    }

    @Override
    public <T> @NonNull B withMetadata(@NonNull NodeMetadataKey<T> key, @Nullable T metadata) {
        Objects.requireNonNull(key, "key");
        if (metadata == null) {
            this.metadata.remove(key);
        } else {
            this.metadata.put(key, metadata);
        }
        return (B) this;
    }

    protected static void ensureDefined(Object value, String description) {
        if (value == null) {
            throw new IllegalStateException(description + " has not been defined");
        }
    }

}
