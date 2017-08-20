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

import me.lucko.luckperms.api.context.ContextSet;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An immutable permission node
 *
 * <p>Use {@link LuckPermsApi#buildNode(String)} to get an instance.</p>
 *
 * @since 2.6
 */
public interface Node extends Map.Entry<String, Boolean> {

    /**
     * Returns the actual permission string
     *
     * @return the actual permission node
     */
    @Nonnull
    String getPermission();

    /**
     * Gets what value the permission is set to. A negated node would return <code>false</code>.
     *
     * @return the permission's value
     */
    @Override
    @Nonnull
    Boolean getValue();

    /**
     * Returns the value of this node as a tristate
     *
     * @return the value of this node as a Tristate
     */
    @Nonnull
    default Tristate getTristate() {
        return Tristate.fromBoolean(getValue());
    }

    /**
     * Returns if the node is negated
     *
     * @return true if the node is negated
     */
    default boolean isNegated() {
        return !getValue();
    }

    /**
     * If this node is set to override explicitly.
     * This value does not persist across saves, and is therefore only useful for transient nodes
     *
     * @return true if this node is set to override explicitly
     */
    boolean isOverride();

    /**
     * Gets the server this node applies on, if the node is server specific
     *
     * @return an {@link Optional} containing the server, if one is defined
     */
    @Nonnull
    Optional<String> getServer();

    /**
     * Gets the world this node applies on, if the node is world specific
     *
     * @return an {@link Optional} containing the world, if one is defined
     */
    @Nonnull
    Optional<String> getWorld();

    /**
     * Returns if this node is server specific
     *
     * @return true if this node is server specific
     */
    boolean isServerSpecific();

    /**
     * Returns if this node is server specific
     *
     * @return true if this node is server specific
     */
    boolean isWorldSpecific();

    /**
     * Returns if this node applies globally, and has no specific context
     *
     * @return true if this node applies globally, and has no specific context
     * @since 3.1
     */
    boolean appliesGlobally();

    /**
     * Returns if this node has any specific context in order to apply.
     *
     * @return true if this node has specific context
     * @since 3.1
     */
    boolean hasSpecificContext();

    /**
     * Returns if this node is able to apply in the given context
     *
     * @param includeGlobal if global server values should apply
     * @param includeGlobalWorld if global world values should apply
     * @param server the server being checked against, or null
     * @param world the world being checked against, or null
     * @param context the context being checked against, or null
     * @param applyRegex if regex should be applied
     * @return true if the node should apply, otherwise false
     * @since 3.1
     */
    boolean shouldApply(boolean includeGlobal, boolean includeGlobalWorld, @Nullable String server, @Nullable String world, @Nullable ContextSet context, boolean applyRegex);

    /**
     * If this node should apply on a specific server
     *
     * @param server        the name of the server
     * @param includeGlobal if global permissions should apply
     * @param applyRegex    if regex should be applied
     * @return true if the node should apply
     */
    boolean shouldApplyOnServer(@Nullable String server, boolean includeGlobal, boolean applyRegex);

    /**
     * If this node should apply on a specific world
     *
     * @param world         the name of the world
     * @param includeGlobal if global permissions should apply
     * @param applyRegex    if regex should be applied
     * @return true if the node should apply
     */
    boolean shouldApplyOnWorld(@Nullable String world, boolean includeGlobal, boolean applyRegex);

    /**
     * If this node should apply in the given context
     *
     * @param context        the context key value pairs
     * @param worldAndServer if world and server contexts should be checked
     * @return true if the node should apply
     * @since 2.13
     */
    boolean shouldApplyWithContext(@Nonnull ContextSet context, boolean worldAndServer);

    /**
     * If this node should apply in the given context
     *
     * @param context the context key value pairs
     * @return true if the node should apply
     * @since 2.13
     */
    boolean shouldApplyWithContext(@Nonnull ContextSet context);

    /**
     * Similar to {@link #shouldApplyOnServer(String, boolean, boolean)}, except this method accepts a List
     *
     * @param servers       the list of servers
     * @param includeGlobal if global permissions should apply
     * @return true if the node should apply
     */
    boolean shouldApplyOnAnyServers(@Nonnull List<String> servers, boolean includeGlobal);

    /**
     * Similar to {@link #shouldApplyOnWorld(String, boolean, boolean)}, except this method accepts a List
     *
     * @param worlds        the list of world
     * @param includeGlobal if global permissions should apply
     * @return true if the node should apply
     */
    boolean shouldApplyOnAnyWorlds(@Nonnull List<String> worlds, boolean includeGlobal);

    /**
     * Resolves a list of wildcards that match this node
     *
     * @param possibleNodes a list of possible permission nodes
     * @return a list of permissions that match this wildcard
     */
    @Nonnull
    List<String> resolveWildcard(@Nonnull List<String> possibleNodes);

    /**
     * Resolves any shorthand parts of this node and returns the full list
     *
     * @return a list of full nodes
     */
    @Nonnull
    List<String> resolveShorthand();

    /**
     * Returns if this node will expire in the future
     *
     * @return true if this node will expire in the future
     */
    boolean isTemporary();

    /**
     * Returns if this node will not expire
     *
     * @return true if this node will not expire
     */
    default boolean isPermanent() {
        return !isTemporary();
    }

    /**
     * Returns a unix timestamp in seconds when this node will expire
     *
     * @return the time in Unix time when this node will expire
     * @throws IllegalStateException if the node is not temporary
     */
    long getExpiryUnixTime() throws IllegalStateException;

    /**
     * Returns the date when this node will expire
     *
     * @return the {@link Date} when this node will expire
     * @throws IllegalStateException if the node is not temporary
     */
    @Nonnull
    Date getExpiry() throws IllegalStateException;

    /**
     * Return the number of seconds until this permission will expire
     *
     * @return the number of seconds until this permission will expire
     * @throws IllegalStateException if the node is not temporary
     */
    long getSecondsTilExpiry() throws IllegalStateException;

    /**
     * Return true if the node has expired.
     * This also returns false if the node is not temporary
     *
     * @return true if this node has expired
     */
    boolean hasExpired();

    /**
     * Gets the extra contexts required for this node to apply
     *
     * @return the extra contexts required for this node to apply
     * @since 2.13
     */
    @Nonnull
    ContextSet getContexts();

    /**
     * The same as {@link #getContexts()}, but also includes values for "server" and "world" keys if present.
     *
     * @return the full contexts required for this node to apply
     * @since 3.1
     */
    @Nonnull
    ContextSet getFullContexts();

    /**
     * Converts this node into a serialized form
     *
     * @return a serialized node string
     * @deprecated because this serialized form is no longer used by the implementation.
     */
    @Deprecated
    @Nonnull
    String toSerializedNode();

    /**
     * Returns if this is a group node
     *
     * @return true if this is a group node
     */
    boolean isGroupNode();

    /**
     * Returns the name of the group
     *
     * @return the name of the group
     * @throws IllegalStateException if this is not a group node. See {@link #isGroupNode()}
     */
    @Nonnull
    String getGroupName() throws IllegalStateException;

    /**
     * Returns if this node is a wildcard node
     *
     * @return true if this node is a wildcard node
     */
    boolean isWildcard();

    /**
     * Gets the level of this wildcard, higher is more specific
     *
     * @return the wildcard level
     * @throws IllegalStateException if this is not a wildcard
     */
    int getWildcardLevel() throws IllegalStateException;

    /**
     * Returns if this node is a meta node
     *
     * @return true if this node is a meta node
     */
    boolean isMeta();

    /**
     * Gets the meta value from this node
     *
     * @return the meta value
     * @throws IllegalStateException if this node is not a meta node
     */
    @Nonnull
    Map.Entry<String, String> getMeta() throws IllegalStateException;

    /**
     * Returns if this node is a prefix node
     *
     * @return true if this node is a prefix node
     */
    boolean isPrefix();

    /**
     * Gets the prefix value from this node
     *
     * @return the prefix value
     * @throws IllegalStateException if this node is a not a prefix node
     */
    @Nonnull
    Map.Entry<Integer, String> getPrefix() throws IllegalStateException;

    /**
     * Returns if this node is a suffix node
     *
     * @return true if this node is a suffix node
     */
    boolean isSuffix();

    /**
     * Gets the suffix value from this node
     *
     * @return the suffix value
     * @throws IllegalStateException if this node is a not a suffix node
     */
    @Nonnull
    Map.Entry<Integer, String> getSuffix() throws IllegalStateException;

    /**
     * Checks if this Node is equal to another node
     *
     * @param obj the other node
     * @return true if this node is equal to the other provided
     * @see #equalsIgnoringValue(Node) for a less strict implementation of this method
     */
    @Override
    boolean equals(Object obj);

    /**
     * Similar to {@link Node#equals(Object)}, except doesn't take note of the value
     *
     * @param other the other node
     * @return true if the two nodes are almost equal
     */
    boolean equalsIgnoringValue(@Nonnull Node other);

    /**
     * Similar to {@link Node#equals(Object)}, except doesn't take note of the expiry time or value
     *
     * @param other the other node
     * @return true if the two nodes are almost equal
     */
    boolean almostEquals(@Nonnull Node other);

    /**
     * Similar to {@link Node#equals(Object)}, except doesn't take note of the value or if the node is temporary
     *
     * @param other the other node
     * @return true if the two nodes are almost equal
     * @since 2.8
     */
    boolean equalsIgnoringValueOrTemp(@Nonnull Node other);

    /**
     * Builds a Node instance
     */
    interface Builder {

        @Nonnull
        Builder setNegated(boolean negated);

        @Nonnull
        Builder setValue(boolean value);

        /**
         * Warning: this value does not persist, and disappears when the holder is re-loaded.
         * It is therefore only useful for transient nodes.
         */
        @Nonnull
        Builder setOverride(boolean override);

        @Nonnull
        Builder setExpiry(long expireAt);

        @Nonnull
        Builder setWorld(@Nonnull String world);

        @Nonnull
        Builder setServer(@Nonnull String server) throws IllegalArgumentException;

        @Nonnull
        Builder withExtraContext(@Nonnull String key, @Nonnull String value);

        @Nonnull
        Builder withExtraContext(@Nonnull Map<String, String> map);

        @Nonnull
        Builder withExtraContext(@Nonnull Set<Map.Entry<String, String>> context);

        @Nonnull
        Builder withExtraContext(@Nonnull Map.Entry<String, String> entry);

        @Nonnull
        Builder withExtraContext(@Nonnull ContextSet set);

        @Nonnull
        Node build();
    }

}
