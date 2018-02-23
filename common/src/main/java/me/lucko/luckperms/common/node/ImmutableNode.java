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

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.StandardNodeEquality;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.processors.WildcardProcessor;
import me.lucko.luckperms.common.utils.DateUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkState;

/**
 * An immutable implementation of {@link Node}.
 */
public final class ImmutableNode implements Node {

    /**
     * The character which separates each part of a permission node
     */
    public static final char NODE_SEPARATOR = '.';
    public static final int NODE_SEPARATOR_CODE = Character.getNumericValue('.');

    private final String permission;

    private final boolean value;

    private boolean override;

    // nullable
    private final String server;
    // nullable
    private final String world;

    // 0L for no expiry
    private final long expireAt;

    private final ImmutableContextSet contexts;

    private final ImmutableContextSet fullContexts;

    /*
     * CACHED STATE
     *
     * These values are based upon the node state above, and are stored here
     * to make node comparison and manipulation faster.
     *
     * This increases the memory footprint of this class by a bit, but it is
     * worth it for the gain in speed.
     *
     * The methods on this class are called v. frequently.
     */

    // storing optionals as a field type is usually a bad idea, however, the
    // #getServer and #getWorld methods are called frequently when comparing nodes.
    // without caching these values, it creates quite a bit of object churn
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<String> optServer;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<String> optWorld;

    private final int hashCode;

    // all nullable
    private String groupName;
    private final int wildcardLevel;
    private Map.Entry<String, String> meta;
    private Map.Entry<Integer, String> prefix;
    private Map.Entry<Integer, String> suffix;

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
    @SuppressWarnings("deprecation")
    ImmutableNode(String permission, boolean value, boolean override, long expireAt, String server, String world, ContextSet contexts) {
        if (permission == null || permission.isEmpty()) {
            throw new IllegalArgumentException("Empty permission");
        }

        // standardize server/world values.
        server = standardizeServerWorld(server);
        world = standardizeServerWorld(world);

        // define core attributes
        this.permission = LegacyNodeFactory.unescapeDelimiters(permission, LegacyNodeFactory.PERMISSION_DELIMITERS).intern();
        this.value = value;
        this.override = override;
        this.expireAt = expireAt;
        this.server = internString(LegacyNodeFactory.unescapeDelimiters(server, LegacyNodeFactory.SERVER_WORLD_DELIMITERS));
        this.world = internString(LegacyNodeFactory.unescapeDelimiters(world, LegacyNodeFactory.SERVER_WORLD_DELIMITERS));
        this.contexts = contexts == null ? ContextSet.empty() : contexts.makeImmutable();

        // define cached state
        this.groupName = NodeFactory.parseGroupNode(this.permission);
        this.wildcardLevel = this.permission.endsWith(WildcardProcessor.WILDCARD_SUFFIX) ? this.permission.chars().filter(num -> num == NODE_SEPARATOR_CODE).sum() : -1;
        this.meta = NodeFactory.parseMetaNode(this.permission);
        this.prefix = NodeFactory.parsePrefixNode(this.permission);
        this.suffix = NodeFactory.parseSuffixNode(this.permission);
        this.resolvedShorthand = ImmutableList.copyOf(ShorthandParser.parseShorthand(getPermission()));
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
    public boolean getValuePrimitive() {
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
        return this.expireAt - DateUtil.unixSecondsNow();
    }

    @Override
    public boolean hasExpired() {
        return isTemporary() && this.expireAt < DateUtil.unixSecondsNow();
    }

    @Override
    public boolean isGroupNode() {
        return this.groupName != null;
    }

    @Nonnull
    @Override
    public String getGroupName() {
        checkState(isGroupNode(), "Node is not a group node");
        return this.groupName;
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
    public boolean isMeta() {
        return this.meta != null;
    }

    @Nonnull
    @Override
    public Map.Entry<String, String> getMeta() {
        checkState(isMeta(), "Node is not a meta node");
        return this.meta;
    }

    @Override
    public boolean isPrefix() {
        return this.prefix != null;
    }

    @Nonnull
    @Override
    public Map.Entry<Integer, String> getPrefix() {
        checkState(isPrefix(), "Node is not a prefix node");
        return this.prefix;
    }

    @Override
    public boolean isSuffix() {
        return this.suffix != null;
    }

    @Nonnull
    @Override
    public Map.Entry<Integer, String> getSuffix() {
        checkState(isSuffix(), "Node is not a suffix node");
        return this.suffix;
    }

    @Override
    public boolean shouldApplyWithContext(@Nonnull ContextSet context) {
        return getFullContexts().isSatisfiedBy(context, false);
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
