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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import me.lucko.luckperms.common.node.utils.ShorthandParser;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeBuilder;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.ScopedNode;
import net.luckperms.api.node.metadata.NodeMetadataKey;
import net.luckperms.api.node.types.PermissionNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractNode<N extends ScopedNode<N, B>, B extends NodeBuilder<N, B>> implements ScopedNode<N, B> {

    /**
     * The character which separates each part of a permission node
     */
    public static final char NODE_SEPARATOR = '.';
    public static final String NODE_SEPARATOR_STRING = String.valueOf(NODE_SEPARATOR);

    // node attributes
    protected final String key;
    protected final boolean value;
    protected final long expireAt; // 0L for no expiry
    protected final ImmutableContextSet contexts;
    protected final Map<NodeMetadataKey<?>, Object> metadata;

    private final List<String> resolvedShorthand;

    // this class is immutable, so we can cache the hashcode calculation
    private final int hashCode;

    protected AbstractNode(String key, boolean value, long expireAt, ImmutableContextSet contexts, Map<NodeMetadataKey<?>, Object> metadata) {
        this.key = key;
        this.value = value;
        this.expireAt = expireAt;
        this.contexts = contexts;
        this.metadata = ImmutableMap.copyOf(metadata);

        this.resolvedShorthand = this instanceof PermissionNode ? ImmutableList.copyOf(ShorthandParser.expandShorthand(this.key)) : ImmutableList.of();

        this.hashCode = calculateHashCode();
    }

    @Override
    public @NonNull String getKey() {
        return this.key;
    }

    @Override
    public boolean getValue() {
        return this.value;
    }

    @Override
    public @NonNull ImmutableContextSet getContexts() {
        return this.contexts;
    }

    @Override
    public <T> Optional<T> getMetadata(@NonNull NodeMetadataKey<T> key) {
        //noinspection unchecked
        T value = (T) this.metadata.get(key);
        return Optional.ofNullable(value);
    }

    @Override
    public boolean hasExpiry() {
        return this.expireAt != 0L;
    }

    @Override
    public @Nullable Instant getExpiry() {
        return hasExpiry() ? Instant.ofEpochSecond(this.expireAt) : null;
    }

    @Override
    public boolean hasExpired() {
        Instant expiry = getExpiry();
        return expiry != null && expiry.isBefore(Instant.now());
    }

    @Override
    public @Nullable Duration getExpiryDuration() {
        Instant expiry = getExpiry();
        if (expiry == null) {
            return null;
        }
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return Duration.between(now, expiry);
    }

    @Override
    public @NonNull Collection<String> resolveShorthand() {
        return this.resolvedShorthand;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Node)) return false;
        return NodeEquality.KEY_VALUE_EXPIRY_CONTEXTS.equals(this, (AbstractNode<?, ?>) o);
    }

    @Override
    public boolean equals(@NonNull Node o, @NonNull NodeEqualityPredicate equalityPredicate) {
        AbstractNode<?, ?> other = (AbstractNode<?, ?>) o;
        NodeEquality nodeEquality = NodeEquality.of(equalityPredicate);
        if (nodeEquality != null) {
            return nodeEquality.equals(this, other);
        } else {
            return equalityPredicate.areEqual(this, o);
        }
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    private int calculateHashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.key.hashCode();
        result = result * PRIME + (this.value ? 79 : 97);
        result = result * PRIME + (int) (this.expireAt >>> 32 ^ this.expireAt);
        result = result * PRIME + this.contexts.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ImmutableNode(" +
                "key=" + this.key + ", " +
                "value=" + this.value + ", " +
                "expireAt=" + this.expireAt + ", " +
                "contexts=" + this.contexts + ")";
    }
}
