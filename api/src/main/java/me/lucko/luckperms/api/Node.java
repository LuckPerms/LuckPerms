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

package me.lucko.luckperms.api;

import com.google.common.base.Preconditions;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.nodetype.NodeType;
import me.lucko.luckperms.api.nodetype.NodeTypeKey;
import me.lucko.luckperms.api.nodetype.types.DisplayNameType;
import me.lucko.luckperms.api.nodetype.types.InheritanceType;
import me.lucko.luckperms.api.nodetype.types.MetaType;
import me.lucko.luckperms.api.nodetype.types.PrefixType;
import me.lucko.luckperms.api.nodetype.types.SuffixType;
import me.lucko.luckperms.api.nodetype.types.WeightType;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Represents a LuckPerms "node".
 *
 * <p>The {@link Node} class encapsulates more than just permission assignments.
 * Nodes are used to store data about inherited groups, as well as assigned
 * prefixes, suffixes and meta values.</p>
 *
 * <p>Combining these various states into one object (a "node") means that a
 * holder only has to have one type of data set (a set of nodes) in order to
 * take on various properties.</p>
 *
 * <p>It is recommended that users of the API make use of {@link Stream}s
 * to manipulate data and obtain the required information.</p>
 *
 * <p>This interface provides a number of methods to read the attributes of the
 * node, as well as methods to query and extract additional state and properties
 * from these settings.</p>
 *
 * <p>Nodes have the following attributes:</p>
 * <p></p>
 * <ul>
 *     <li>{@link #getPermission() permission} - the actual permission string</li>
 *     <li>{@link #getValue() value} - the value of the node (false for negated)</li>
 *     <li>{@link #isOverride() override} - if the node is marked as having special priority over other nodes</li>
 *     <li>{@link #getServer() server} - the specific server where this node should apply</li>
 *     <li>{@link #getWorld() world} - the specific world where this node should apply</li>
 *     <li>{@link #getContexts() context} - the additional contexts required for this node to apply </li>
 *     <li>{@link #getExpiry() expiry} - the time when this node should expire</li>
 * </ul>
 *
 * <p>The 'permission' property of a {@link Node} is also used in some cases to represent state
 * beyond a granted permission. This state is encapsulated by extra {@link NodeType} data which
 * can be obtained from this instance using {@link #getTypeData(NodeTypeKey)}.</p>
 *
 * <p>Type data is mapped by {@link NodeTypeKey}s, which are usually stored as static members of the
 * corresponding {@link NodeType} class under the <code>KEY</code> field.</p>
 *
 * <p>The current types are:</p>
 * <p></p>
 * <ul>
 *     <li>normal - just a regular permission</li>
 *     <li>{@link InheritanceType} - an "inheritance node" marks that the holder should inherit data from another group</li>
 *     <li>{@link PrefixType} - represents an assigned prefix</li>
 *     <li>{@link SuffixType} - represents an assigned suffix</li>
 *     <li>{@link MetaType} - represents an assigned meta option</li>
 *     <li>{@link WeightType} - marks the weight of the object holding this node</li>
 *     <li>{@link DisplayNameType} - marks the display name of the object holding this node</li>
 * </ul>
 *
 * <p>The core node state must be immutable in all implementations.</p>
 *
 * @see NodeFactory for obtaining and constructing instances.
 * @since 2.6
 */
@Immutable
public interface Node {

    /**
     * Gets the permission string this node encapsulates.
     *
     * <p>The exact value of this string may vary for nodes which aren't regular
     * permission settings.</p>
     *
     * @return the actual permission node
     */
    @Nonnull
    String getPermission();

    /**
     * Gets the value of the node.
     *
     * <p>A negated setting would result in a value of <code>false</code>.</p>
     *
     * @return the nodes value
     */
    boolean getValue();

    /**
     * Gets the value of this node as a {@link Tristate}.
     *
     * @return the value of this node as a Tristate
     */
    @Nonnull
    default Tristate getTristate() {
        return Tristate.fromBoolean(getValue());
    }

    /**
     * Gets if the node is negated.
     *
     * <p>This is the inverse of the {@link #getValue() value}.</p>
     *
     * @return true if the node is negated
     */
    default boolean isNegated() {
        return !getValue();
    }

    /**
     * Gets if this node is set to override explicitly.
     *
     * <p>This value does not persist across saves, and is therefore only
     * useful for transient nodes.</p>
     *
     * @return true if this node is set to override explicitly
     */
    boolean isOverride();

    /**
     * Gets the server this node applies on, if the node is server specific.
     *
     * @return an {@link Optional} containing the server, if one is defined
     */
    @Nonnull
    Optional<String> getServer();

    /**
     * Gets the world this node applies on, if the node is world specific.
     *
     * @return an {@link Optional} containing the world, if one is defined
     */
    @Nonnull
    Optional<String> getWorld();

    /**
     * Gets if this node is server specific.
     *
     * @return true if this node is server specific
     */
    boolean isServerSpecific();

    /**
     * Gets if this node is server specific.
     *
     * @return true if this node is server specific
     */
    boolean isWorldSpecific();

    /**
     * Gets if this node applies globally, and therefore has no specific context.
     *
     * @return true if this node applies globally, and has no specific context
     * @since 3.1
     */
    boolean appliesGlobally();

    /**
     * Gets if this node has any specific context in order for it to apply.
     *
     * @return true if this node has specific context
     * @since 3.1
     */
    boolean hasSpecificContext();

    /**
     * Gets if this node should apply in the given context
     *
     * @param contextSet the context set
     * @return true if the node should apply
     * @since 2.13
     */
    boolean shouldApplyWithContext(@Nonnull ContextSet contextSet);

    /**
     * Resolves any shorthand parts of this node and returns the full list of
     * resolved nodes.
     *
     * <p>The list will not contain the exact permission itself.</p>
     *
     * @return a list of full nodes
     */
    @Nonnull
    List<String> resolveShorthand();

    /**
     * Gets if this node is assigned temporarily.
     *
     * @return true if this node will expire in the future
     */
    boolean isTemporary();

    /**
     * Gets if this node is permanent (will not expire).
     *
     * @return true if this node will not expire
     */
    default boolean isPermanent() {
        return !isTemporary();
    }

    /**
     * Gets the unix timestamp (in seconds) when this node will expire.
     *
     * @return the time in Unix time when this node will expire
     * @throws IllegalStateException if the node is not temporary
     */
    long getExpiryUnixTime() throws IllegalStateException;

    /**
     * Gets the date when this node will expire.
     *
     * @return the {@link Date} when this node will expire
     * @throws IllegalStateException if the node is not temporary
     */
    @Nonnull
    Date getExpiry() throws IllegalStateException;

    /**
     * Gets the number of seconds until this permission will expire.
     *
     * <p>Will return a negative value if the node has already expired.</p>
     *
     * @return the number of seconds until this permission will expire
     * @throws IllegalStateException if the node is not temporary
     */
    long getSecondsTilExpiry() throws IllegalStateException;

    /**
     * Gets if the node has expired.
     *
     * <p>This returns false if the node is not temporary.</p>
     *
     * @return true if this node has expired
     */
    boolean hasExpired();

    /**
     * Gets the extra contexts required for this node to apply.
     *
     * @return the extra contexts required for this node to apply
     * @since 2.13
     */
    @Nonnull
    ContextSet getContexts();

    /**
     * The same as {@link #getContexts()}, but also includes context pairs for
     * "server" and "world" keys if present.
     *
     * @return the full contexts required for this node to apply
     * @since 3.1
     * @see Contexts#SERVER_KEY
     * @see Contexts#WORLD_KEY
     */
    @Nonnull
    ContextSet getFullContexts();

    /**
     * Gets if this node is a wildcard permission.
     *
     * @return true if this node is a wildcard permission
     */
    boolean isWildcard();

    /**
     * Gets the level of this wildcard.
     *
     * <p>The node <code>luckperms.*</code> has a wildcard level of 1.</p>
     * <p>The node <code>luckperms.user.permission.*</code> has a wildcard level of 3.</p>
     *
     * <p>Nodes with a higher wildcard level are more specific and have priority over
     * less specific nodes (nodes with a lower wildcard level).</p>
     *
     * @return the wildcard level
     * @throws IllegalStateException if this is not a wildcard
     */
    int getWildcardLevel() throws IllegalStateException;

    /**
     * Gets if this node has any extra {@link NodeType} data attached to it.
     *
     * @return if this node has any type data
     * @since 4.2
     */
    boolean hasTypeData();

    /**
     * Gets the type data corresponding to the given <code>key</code>, if present.
     *
     * @param key the key
     * @param <T> the {@link NodeType} type
     * @return the data, if present
     * @since 4.2
     */
    <T extends NodeType> Optional<T> getTypeData(NodeTypeKey<T> key);

    /**
     * Gets the type data corresponding to the given <code>key</code>, throwing an exception
     * if no data is present.
     *
     * @param key the key
     * @param <T> the {@link NodeType} type
     * @return the data
     * @throws IllegalStateException if data isn't present
     * @since 4.2
     */
    default <T extends NodeType> T typeData(NodeTypeKey<T> key) throws IllegalStateException {
        return getTypeData(key)
                .orElseThrow(() ->
                        new IllegalStateException("Node '" + getPermission() + "' does not have the '" + key.getTypeName() + "' type.")
                );
    }

    /**
     * Gets if this node has {@link InheritanceType} type data.
     *
     * @return true if this is a inheritance (group) node.
     */
    default boolean isGroupNode() {
        return getTypeData(InheritanceType.KEY).isPresent();
    }

    /**
     * Gets the name of the inherited group if this node has {@link InheritanceType} type data,
     * throwing an exception if the data is not present.
     *
     * @return the name of the group
     * @throws IllegalStateException if this node doesn't have {@link InheritanceType} data
     */
    @Nonnull
    default String getGroupName() throws IllegalStateException {
        return typeData(InheritanceType.KEY).getGroupName();
    }

    /**
     * Gets if this node has {@link MetaType} type data.
     *
     * @return true if this is a meta node.
     */
    default boolean isMeta() {
        return getTypeData(MetaType.KEY).isPresent();
    }

    /**
     * Gets the meta entry if this node has {@link MetaType} type data,
     * throwing an exception if the data is not present.
     *
     * @return the meta entry
     * @throws IllegalStateException if this node doesn't have {@link MetaType} data
     */
    @Nonnull
    default Map.Entry<String, String> getMeta() throws IllegalStateException {
        return typeData(MetaType.KEY);
    }

    /**
     * Gets if this node has {@link PrefixType} type data.
     *
     * @return true if this node is a prefix node
     */
    default boolean isPrefix() {
        return getTypeData(PrefixType.KEY).isPresent();
    }

    /**
     * Gets the prefix entry if this node has {@link PrefixType} type data,
     * throwing an exception if the data is not present.
     *
     * @return the meta entry
     * @throws IllegalStateException if this node doesn't have {@link PrefixType} data
     */
    @Nonnull
    default Map.Entry<Integer, String> getPrefix() throws IllegalStateException {
        return typeData(PrefixType.KEY).getAsEntry();
    }

    /**
     * Gets if this node has {@link SuffixType} type data.
     *
     * @return true if this node is a suffix node
     */
    default boolean isSuffix() {
        return getTypeData(SuffixType.KEY).isPresent();
    }

    /**
     * Gets the suffix entry if this node has {@link SuffixType} type data,
     * throwing an exception if the data is not present.
     *
     * @return the meta entry
     * @throws IllegalStateException if this node doesn't have {@link SuffixType} data
     */
    @Nonnull
    default Map.Entry<Integer, String> getSuffix() throws IllegalStateException {
        return typeData(SuffixType.KEY).getAsEntry();
    }

    /**
     * Gets if this Node is equal to another node.
     *
     * @param obj the other node
     * @return true if this node is equal to the other provided
     * @see StandardNodeEquality#EXACT
     */
    @Override
    boolean equals(Object obj);

    /**
     * Gets if this Node is equal to another node as defined by the given
     * {@link StandardNodeEquality} predicate.
     *
     * @param other the other node
     * @param equalityPredicate the predicate
     * @return true if this node is considered equal
     * @since 4.1
     */
    boolean standardEquals(Node other, StandardNodeEquality equalityPredicate);

    /**
     * Gets if this Node is equal to another node as defined by the given
     * {@link NodeEqualityPredicate}.
     *
     * @param other the other node
     * @param equalityPredicate the predicate
     * @return true if this node is considered equal
     * @since 4.1
     */
    default boolean equals(Node other, NodeEqualityPredicate equalityPredicate) {
        return equalityPredicate.areEqual(this, other);
    }

    /**
     * Similar to {@link Node#equals(Object)}, except doesn't take note of the
     * value.
     *
     * @param other the other node
     * @return true if the two nodes are almost equal
     * @deprecated in favour of {@link #equals(Node, NodeEqualityPredicate)}
     * @see StandardNodeEquality#IGNORE_VALUE
     */
    @Deprecated
    default boolean equalsIgnoringValue(@Nonnull Node other) {
        return equals(other, StandardNodeEquality.IGNORE_VALUE);
    }

    /**
     * Similar to {@link Node#equals(Object)}, except doesn't take note of the
     * expiry time or value.
     *
     * @param other the other node
     * @return true if the two nodes are almost equal
     * @deprecated in favour of {@link #equals(Node, NodeEqualityPredicate)}
     * @see StandardNodeEquality#IGNORE_EXPIRY_TIME_AND_VALUE
     */
    @Deprecated
    default boolean almostEquals(@Nonnull Node other) {
        return equals(other, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE);
    }

    /**
     * Similar to {@link Node#equals(Object)}, except doesn't take note of the
     * value or if the node is temporary.
     *
     * @param other the other node
     * @return true if the two nodes are almost equal
     * @since 2.8
     * @deprecated in favour of {@link #equals(Node, NodeEqualityPredicate)}
     * @see StandardNodeEquality#IGNORE_VALUE_OR_IF_TEMPORARY
     */
    @Deprecated
    default boolean equalsIgnoringValueOrTemp(@Nonnull Node other) {
        return equals(other, StandardNodeEquality.IGNORE_VALUE_OR_IF_TEMPORARY);
    }

    /**
     * Constructs a new builder initially containing the current properties of
     * this node.
     *
     * @return a new builder
     * @since 4.1
     */
    Builder toBuilder();

    /**
     * Builds a Node instance
     */
    interface Builder {

        /**
         * Copies the attributes from the given node and applies them to this
         * builder.
         *
         * <p>Note that this copies all attributes <strong>except</strong> the
         * permission itself.</p>
         *
         * @param node the node to copy from
         * @return the builder
         * @since 4.2
         */
        Builder copyFrom(@Nonnull Node node);

        /**
         * Sets the value of negated for the node.
         *
         * @param negated the value
         * @return the builder
         * @see Node#isNegated()
         */
        @Nonnull
        Builder setNegated(boolean negated);

        /**
         * Sets the value of the node.
         *
         * @param value the value
         * @return the builder
         * @see Node#getValue()
         */
        @Nonnull
        Builder setValue(boolean value);

        /**
         * Sets the override property for the node.
         *
         * <p>Warning: this value does not persist, and disappears when the holder is re-loaded.
         * It is therefore only useful for transient nodes.</p>
         *
         * @param override the override state
         * @return the builder
         * @see Node#isOverride()
         */
        @Nonnull
        Builder setOverride(boolean override);

        /**
         * Sets the time when the node should expire.
         *
         * <p>The parameter passed to this method must be the unix timestamp
         * (in seconds) when the node should expire.</p>
         *
         * @param expiryUnixTimestamp the expiry timestamp (unix seconds)
         * @return the builder
         * @see Node#getExpiryUnixTime()
         */
        @Nonnull
        Builder setExpiry(long expiryUnixTimestamp);

        /**
         * Sets the time when the node should expire.
         *
         * <p>The expiry timestamp is calculated relative to the current
         * system time.</p>
         *
         * @param duration how long the node should be added for
         * @param unit the unit <code>duration</code> is measured in
         * @return the builder
         * @since 4.2
         */
        @Nonnull
        default Builder setExpiry(long duration, TimeUnit unit) {
            Preconditions.checkArgument(duration > 0, "duration must be positive");
            long seconds = Objects.requireNonNull(unit, "unit").toSeconds(duration);
            long timeNow = System.currentTimeMillis() / 1000L;
            return setExpiry(timeNow + seconds);
        }

        /**
         * Marks that the node being built should never expire.
         *
         * @return the builder
         * @since 4.2
         */
        @Nonnull
        Builder clearExpiry();

        /**
         * Sets the world value for the node.
         *
         * @param world the world value
         * @return the builder
         * @see Node#getWorld()
         */
        @Nonnull
        Builder setWorld(@Nullable String world);

        /**
         * Sets the server value for the node.
         *
         * @param server the world value
         * @return the builder
         * @see Node#getServer()
         */
        @Nonnull
        Builder setServer(@Nullable String server);

        /**
         * Appends an extra context onto the node.
         *
         * @param key the context key
         * @param value the context value
         * @return the builder
         * @see ContextSet
         * @see Node#getContexts()
         */
        @Nonnull
        Builder withExtraContext(@Nonnull String key, @Nonnull String value);

        /**
         * Appends extra contexts onto the node.
         *
         * @param map a map of contexts
         * @return the builder
         * @see ContextSet
         * @see Node#getContexts()
         */
        @Nonnull
        Builder withExtraContext(@Nonnull Map<String, String> map);

        /**
         * Appends extra contexts onto the node.
         *
         * @param context a set of contexts
         * @return the builder
         * @see ContextSet
         * @see Node#getContexts()
         */
        @Nonnull
        Builder withExtraContext(@Nonnull Set<Map.Entry<String, String>> context);

        /**
         * Appends an extra context onto the node.
         *
         * @param entry the context
         * @return the builder
         * @see ContextSet
         * @see Node#getContexts()
         */
        @Nonnull
        Builder withExtraContext(@Nonnull Map.Entry<String, String> entry);

        /**
         * Appends extra contexts onto the node.
         *
         * @param contextSet a context set
         * @return the builder
         * @see ContextSet
         * @see Node#getContexts()
         */
        @Nonnull
        Builder withExtraContext(@Nonnull ContextSet contextSet);

        /**
         * Sets the extra contexts for the node.
         *
         * @param contextSet a context set
         * @return the builder
         * @see ContextSet
         * @see Node#getContexts()
         * @since 4.2
         */
        @Nonnull
        Builder setExtraContext(@Nonnull ContextSet contextSet);

        /**
         * Creates a {@link Node} instance from the builder.
         *
         * @return a new node instance
         */
        @Nonnull
        Node build();
    }

}
