/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an immutable node object
 * <p> Use {@link LuckPermsApi#buildNode(String)} to get an instance.
 * @since 1.6
 */
public interface Node extends Map.Entry<String, Boolean> {

    /**
     * @return the actual permission node
     */
    String getPermission();

    /**
     * Get what value the permission is set to. A negated node would return <code>false</code>.
     * @return the permission's value
     */
    @Override
    Boolean getValue();

    /**
     * @return the value of this node as a Tristate
     */
    Tristate getTristate();

    /**
     * @return true if the node is negated
     */
    boolean isNegated();

    /**
     * If this node is set to override explicitly.
     * This value does not persist across saves, and is therefore only useful for transient nodes
     * @return true if this node is set to override explicitly
     */
    boolean isOverride();

    /**
     * Gets the server this node applies on, if the node is server specific
     * @return an {@link Optional} containing the server, if one is defined
     */
    Optional<String> getServer();

    /**
     * Gets the world this node applies on, if the node is world specific
     * @return an {@link Optional} containing the world, if one is defined
     */
    Optional<String> getWorld();

    /**
     * @return true if this node is server specific
     */
    boolean isServerSpecific();

    /**
     * @return true if this node is server specific
     */
    boolean isWorldSpecific();

    /**
     * If this node should apply on a specific server
     * @param server the name of the server
     * @param includeGlobal if global permissions should apply
     * @param applyRegex if regex should be applied
     * @return true if the node should apply
     */
    boolean shouldApplyOnServer(String server, boolean includeGlobal, boolean applyRegex);

    /**
     * If this node should apply on a specific world
     * @param world the name of the world
     * @param includeGlobal if global permissions should apply
     * @param applyRegex if regex should be applied
     * @return true if the node should apply
     */
    boolean shouldApplyOnWorld(String world, boolean includeGlobal, boolean applyRegex);

    /**
     * If this node should apply given the specific context
     * @param context the context key value pairs
     * @return true if the node should apply
     */
    boolean shouldApplyWithContext(Map<String, String> context);

    /**
     * Similar to {@link #shouldApplyOnServer(String, boolean, boolean)}, except this method accepts a List
     * @param servers the list of servers
     * @param includeGlobal if global permissions should apply
     * @return true if the node should apply
     */
    boolean shouldApplyOnAnyServers(List<String> servers, boolean includeGlobal);

    /**
     * Similar to {@link #shouldApplyOnWorld(String, boolean, boolean)}, except this method accepts a List
     * @param worlds the list of world
     * @param includeGlobal if global permissions should apply
     * @return true if the node should apply
     */
    boolean shouldApplyOnAnyWorlds(List<String> worlds, boolean includeGlobal);

    /**
     * Resolves a list of wildcards that match this node
     * @param possibleNodes a list of possible permission nodes
     * @return a list of permissions that match this wildcard
     */
    List<String> resolveWildcard(List<String> possibleNodes);

    /**
     * Resolves any shorthand parts of this node and returns the full list
     * @return a list of full nodes
     */
    List<String> resolveShorthand();

    /**
     * @return true if this node will expire in the future
     */
    boolean isTemporary();

    /**
     * @return true if this node will not expire
     */
    boolean isPermanent();

    /**
     * @return the time in Unix time when this node will expire
     */
    long getExpiryUnixTime();

    /**
     * @return the {@link Date} when this node will expire
     */
    Date getExpiry();

    /**
     * @return the number of seconds until this permission will expire
     */
    long getSecondsTilExpiry();

    /**
     * @return true if this node has expired
     */
    boolean hasExpired();

    /**
     * @return the extra contexts required for this node to apply
     */
    Map<String, String> getExtraContexts();

    /**
     * Converts this node into a serialized form
     * @return a serialized node string
     */
    String toSerializedNode();

    /**
     * @return true if this is a group node
     */
    boolean isGroupNode();

    /**
     * @return the name of the group
     * @throws IllegalStateException if this is not a group node. See {@link #isGroupNode()}
     */
    String getGroupName();

    /**
     * @return true is this node is a wildcard node
     */
    boolean isWildcard();

    /**
     * Gets the level of this wildcard, higher is more specific
     * @return the wildcard level
     * @throws IllegalStateException if this is not a wildcard
     */
    int getWildcardLevel();

    /**
     * @return true if this node is a meta node
     */
    boolean isMeta();

    /**
     * Gets the meta value from this node
     * @return the meta value
     * @throws IllegalStateException if this node is not a meta node
     */
    Map.Entry<String, String> getMeta();

    /**
     * @return true if this node is a prefix node
     */
    boolean isPrefix();

    /**
     * Gets the prefix value from this node
     * @return the prefix value
     * @throws IllegalStateException if this node is a not a prefix node
     */
    Map.Entry<Integer, String> getPrefix();

    /**
     * @return true if this node is a suffix node
     */
    boolean isSuffix();

    /**
     * Gets the suffix value from this node
     * @return the suffix value
     * @throws IllegalStateException if this node is a not a suffix node
     */
    Map.Entry<Integer, String> getSuffix();

    /**
     * Similar to {@link #equals(Object)}, except doesn't take note of the value
     * @param node the other node
     * @return true if the two nodes are almost equal
     */
    boolean equalsIgnoringValue(Node node);

    /**
     * Similar to {@link #equals(Object)}, except doesn't take note of the expiry time or value
     * @param node the other node
     * @return true if the two nodes are almost equal
     */
    boolean almostEquals(Node node);

    interface Builder {
        Builder setNegated(boolean negated);
        Builder setValue(boolean value);

        /**
         * Warning: this value does not persist, and disappears when the holder is re-loaded.
         * It is therefore only useful for transient nodes.
         */
        Builder setOverride(boolean override);

        Builder setExpiry(long expireAt);
        Builder setWorld(String world);
        Builder setServer(String server) throws IllegalArgumentException;
        Builder withExtraContext(String key, String value);
        Builder withExtraContext(Map<String, String> map);
        Builder withExtraContext(Map.Entry<String, String> entry);
        Node build();
    }

}
