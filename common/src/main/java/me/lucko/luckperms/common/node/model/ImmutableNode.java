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

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.StandardNodeEquality;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.api.nodetype.NodeType;
import me.lucko.luckperms.api.nodetype.NodeTypeKey;
import me.lucko.luckperms.api.nodetype.types.RegexType;
import me.lucko.luckperms.common.node.factory.NodeBuilder;
import me.lucko.luckperms.common.node.utils.ShorthandParser;
import me.lucko.luckperms.common.processors.WildcardProcessor;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkState;

/**
 * An immutable implementation of {@link Node}.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class ImmutableNode implements Node {

    /**
     * The character which separates each part of a permission node
     */
    public static final char NODE_SEPARATOR = '.';

    /**
     * The numeric value of {@link #NODE_SEPARATOR}
     */
    public static final int NODE_SEPARATOR_CODE = Character.getNumericValue(NODE_SEPARATOR);

    // node attributes

    private final String permission;
    private final boolean value;
    private boolean override;

    @Nullable
    private final String server;
    @Nullable
    private final String world;

    private final long expireAt; // 0L for no expiry
    private final ImmutableContextSet contexts;
    private final ImmutableContextSet fullContexts;



    // cached state
    private final Optional<String> optServer;
    private final Optional<String> optWorld;

    // this class is immutable, so we can cache the hashcode calculation
    private final int hashCode;

    private final int wildcardLevel;
    private final Map<NodeTypeKey<?>, NodeType> resolvedTypes;
    private final List<String> resolvedShorthand;

    /**
     * Make an immutable node instance
     *
     * @param permission the actual permission node
     * @param value      the value (if it's *not* negated)
     * @param expireAt   the time when the node will expire
     * @param server     the server this node applies on
     * @param world      the world this node applies on
     * @param contexts   any additional contexts applying to this node
     */
    public ImmutableNode(String permission, boolean value, boolean override, long expireAt, String server, String world, ContextSet contexts) {
        if (permission == null || permission.isEmpty()) {
            throw new IllegalArgumentException("Empty permission");
        }

        // standardize server/world values.
        server = standardizeServerWorld(server);
        world = standardizeServerWorld(world);

        // define core attributes
        this.permission = permission.intern();
        this.value = value;
        this.override = override;
        this.expireAt = expireAt;
        this.server = internString(server);
        this.world = internString(world);
        this.contexts = contexts == null ? ContextSet.empty() : contexts.makeImmutable();

        // define cached state
        this.wildcardLevel = this.permission.endsWith(WildcardProcessor.WILDCARD_SUFFIX) ? this.permission.chars().filter(num -> num == NODE_SEPARATOR_CODE).sum() : -1;
        this.resolvedTypes = NodeTypes.parseTypes(this.permission);
        this.resolvedShorthand = this.resolvedTypes.containsKey(RegexType.KEY) ? ImmutableList.of() : ImmutableList.copyOf(ShorthandParser.parseShorthand(getPermission()));
        this.optServer = Optional.ofNullable(this.server);
        this.optWorld = Optional.ofNullable(this.world);

        // calculate the "full" context set
        if (this.server != null || this.world != null) {
            MutableContextSet fullContexts = this.contexts.mutableCopy();
            if (this.server != null) {
                fullContexts.add(Contexts.SERVER_KEY, this.server);
            }
            if (this.world != null) {
                fullContexts.add(Contexts.WORLD_KEY, this.world);
            }

            this.fullContexts = fullContexts.makeImmutable();
        } else {
            this.fullContexts = this.contexts;
        }

        this.hashCode = calculateHashCode();
    }

    @Override
    public Builder toBuilder() {
        return new NodeBuilder(this);
    }

    @Nonnull
    @Override
    public String getPermission() {
        return this.permission;
    }

    @Override
    public boolean getValue() {
        return this.value;
    }

    @Override
    public boolean isOverride() {
        return this.override;
    }

    @Nonnull
    @Override
    public Optional<String> getServer() {
        return this.optServer;
    }

    @Nonnull
    @Override
    public Optional<String> getWorld() {
        return this.optWorld;
    }

    @Override
    public boolean isServerSpecific() {
        return this.server != null;
    }

    @Override
    public boolean isWorldSpecific() {
        return this.world != null;
    }

    @Nonnull
    @Override
    public ImmutableContextSet getContexts() {
        return this.contexts;
    }

    @Nonnull
    @Override
    public ImmutableContextSet getFullContexts() {
        return this.fullContexts;
    }

    @Override
    public boolean appliesGlobally() {
        return this.server == null && this.world == null && this.contexts.isEmpty();
    }

    @Override
    public boolean hasSpecificContext() {
        return this.server != null || this.world != null || !this.contexts.isEmpty();
    }

    @Override
    public boolean shouldApplyWithContext(@Nonnull ContextSet contextSet) {
        return getFullContexts().isSatisfiedBy(contextSet);
    }

    @Override
    public boolean isTemporary() {
        return this.expireAt != 0L;
    }

    @Override
    public long getExpiryUnixTime() {
        checkState(isTemporary(), "Node does not have an expiry time.");
        return this.expireAt;
    }

    @Nonnull
    @Override
    public Date getExpiry() {
        checkState(isTemporary(), "Node does not have an expiry time.");
        return new Date(this.expireAt * 1000L);
    }

    @Override
    public long getSecondsTilExpiry() {
        checkState(isTemporary(), "Node does not have an expiry time.");
        return this.expireAt - System.currentTimeMillis() / 1000L;
    }

    @Override
    public boolean hasExpired() {
        return isTemporary() && this.expireAt < System.currentTimeMillis() / 1000L;
    }

    @Override
    public boolean isWildcard() {
        return this.wildcardLevel != -1;
    }

    @Override
    public int getWildcardLevel() {
        checkState(isWildcard(), "Node is not a wildcard");
        return this.wildcardLevel;
    }

    @Override
    public boolean hasTypeData() {
        return !this.resolvedTypes.isEmpty();
    }

    @Override
    public <T extends NodeType> Optional<T> getTypeData(NodeTypeKey<T> key) {
        Objects.requireNonNull(key, "key");

        //noinspection unchecked
        T result = (T) this.resolvedTypes.get(key);
        return Optional.ofNullable(result);
    }

    @Nonnull
    @Override
    public List<String> resolveShorthand() {
        return this.resolvedShorthand;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Node)) return false;

        Node other = (Node) o;
        while (other instanceof ForwardingNode) {
            other = ((ForwardingNode) other).delegate();
        }
        return other instanceof ImmutableNode && Equality.EXACT.areEqual(this, (ImmutableNode) other);
    }

    @Override
    public boolean standardEquals(Node o, StandardNodeEquality equalityPredicate) {
        while (o instanceof ForwardingNode) {
            o = ((ForwardingNode) o).delegate();
        }
        if (!(o instanceof ImmutableNode)) {
            return false;
        }
        ImmutableNode other = (ImmutableNode) o;
        switch (equalityPredicate) {
            case EXACT:
                return Equality.EXACT.areEqual(this, other);
            case IGNORE_VALUE:
                return Equality.IGNORE_VALUE.areEqual(this, other);
            case IGNORE_EXPIRY_TIME:
                return Equality.IGNORE_EXPIRY_TIME.areEqual(this, other);
            case IGNORE_EXPIRY_TIME_AND_VALUE:
                return Equality.IGNORE_EXPIRY_TIME_AND_VALUE.areEqual(this, other);
            case IGNORE_VALUE_OR_IF_TEMPORARY:
                return Equality.IGNORE_VALUE_OR_IF_TEMPORARY.areEqual(this, other);
            default:
                throw new AssertionError();
        }
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    private int calculateHashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.permission.hashCode();
        result = result * PRIME + (this.value ? 79 : 97);
        result = result * PRIME + (this.override ? 79 : 97);
        result = result * PRIME + (this.server == null ? 43 : this.server.hashCode());
        result = result * PRIME + (this.world == null ? 43 : this.world.hashCode());
        result = result * PRIME + (int) (this.expireAt >>> 32 ^ this.expireAt);
        result = result * PRIME + this.contexts.hashCode();
        return result;
    }

    @SuppressWarnings("StringEquality")
    private enum Equality {
        EXACT {
            @Override
            public boolean areEqual(@Nonnull ImmutableNode o1, @Nonnull ImmutableNode o2) {
                return o1 == o2 ||
                        o1.permission == o2.permission &&
                        o1.value == o2.value &&
                        o1.override == o2.override &&
                        (o1.server == null ? o2.server == null : o1.server.equals(o2.server)) &&
                        (o1.world == null ? o2.world == null : o1.world.equals(o2.world)) &&
                        o1.expireAt == o2.expireAt &&
                        o1.getContexts().equals(o2.getContexts());
            }
        },
        IGNORE_VALUE {
            @Override
            public boolean areEqual(@Nonnull ImmutableNode o1, @Nonnull ImmutableNode o2) {
                return o1 == o2 ||
                        o1.permission == o2.permission &&
                        o1.override == o2.override &&
                        (o1.server == null ? o2.server == null : o1.server.equals(o2.server)) &&
                        (o1.world == null ? o2.world == null : o1.world.equals(o2.world)) &&
                        o1.expireAt == o2.expireAt &&
                        o1.getContexts().equals(o2.getContexts());
            }
        },
        IGNORE_EXPIRY_TIME {
            @Override
            public boolean areEqual(@Nonnull ImmutableNode o1, @Nonnull ImmutableNode o2) {
                return o1 == o2 ||
                        o1.permission == o2.permission &&
                        o1.value == o2.value &&
                        o1.override == o2.override &&
                        (o1.server == null ? o2.server == null : o1.server.equals(o2.server)) &&
                        (o1.world == null ? o2.world == null : o1.world.equals(o2.world)) &&
                        o1.isTemporary() == o2.isTemporary() &&
                        o1.getContexts().equals(o2.getContexts());
            }
        },
        IGNORE_EXPIRY_TIME_AND_VALUE {
            @Override
            public boolean areEqual(@Nonnull ImmutableNode o1, @Nonnull ImmutableNode o2) {
                return o1 == o2 ||
                        o1.permission == o2.permission &&
                        o1.override == o2.override &&
                        (o1.server == null ? o2.server == null : o1.server.equals(o2.server)) &&
                        (o1.world == null ? o2.world == null : o1.world.equals(o2.world)) &&
                        o1.isTemporary() == o2.isTemporary() &&
                        o1.getContexts().equals(o2.getContexts());
            }
        },
        IGNORE_VALUE_OR_IF_TEMPORARY {
            @Override
            public boolean areEqual(@Nonnull ImmutableNode o1, @Nonnull ImmutableNode o2) {
                return o1 == o2 ||
                        o1.permission == o2.permission &&
                        o1.override == o2.override &&
                        (o1.server == null ? o2.server == null : o1.server.equals(o2.server)) &&
                        (o1.world == null ? o2.world == null : o1.world.equals(o2.world)) &&
                        o1.getContexts().equals(o2.getContexts());
            }
        };

        public abstract boolean areEqual(@Nonnull ImmutableNode o1, @Nonnull ImmutableNode o2);
    }

    private static String internString(String s) {
        return s == null ? null : s.intern();
    }

    private static String standardizeServerWorld(String s) {
        if (s != null) {
            s = s.toLowerCase();

            if (s.equals("global") || s.isEmpty()) {
                s = null;
            }
        }

        return s;
    }

    @Override
    public String toString() {
        return "ImmutableNode(" +
                "permission=" + this.permission + ", " +
                "value=" + this.value + ", " +
                "override=" + this.override + ", " +
                "server=" + this.getServer() + ", " +
                "world=" + this.getWorld() + ", " +
                "expireAt=" + this.expireAt + ", " +
                "contexts=" + this.contexts + ")";
    }
}
