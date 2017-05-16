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

/**
 * Builds {@link Node} instances
 *
 * @since 2.17
 */
public interface NodeFactory {

    /**
     * Creates a node from a serialised node string
     *
     * @param serialisedPermission the serialised permission string
     * @param value                the value of the node
     * @return a node instance
     * @throws NullPointerException if the permission is null
     * @deprecated since this format isn't used internally for permissions anymore
     * @see Node#toSerializedNode()
     */
    @Deprecated
    Node fromSerialisedNode(String serialisedPermission, boolean value);


    /**
     * Creates a new node builder from a given base permission string
     *
     * @param permission the permission
     * @return a node builder instance
     * @throws NullPointerException if the permission is null
     */
    Node.Builder newBuilder(String permission);

    /**
     * Creates a node builder instance from an existing node
     *
     * @param other the other node
     * @return a node builder instance
     * @throws NullPointerException if the other node is null
     */
    Node.Builder newBuilderFromExisting(Node other);

    /**
     * Creates a node builder from a serialised node string
     *
     * @param serialisedPermission the serialised permission string
     * @param value                the value of the node
     * @return a node builder instance
     * @throws NullPointerException if the permission is null
     * @deprecated since this format isn't used internally for permissions anymore
     * @see Node#toSerializedNode()
     */
    @Deprecated
    Node.Builder newBuilderFromSerialisedNode(String serialisedPermission, boolean value);


    /**
     * Creates a node builder from a group
     *
     * @param group the group
     * @return a node builder instance
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @since 3.1
     */
    Node.Builder makeGroupNode(Group group);

    /**
     * Creates a node builder from a key value pair
     *
     * @param key   the key
     * @param value the value
     * @return a node builder instance
     * @throws NullPointerException if the key or value is null
     */
    Node.Builder makeMetaNode(String key, String value);

    /**
     * Creates a node builder for the given chat meta type
     *
     * @param priority the priority
     * @param value the value for the prefix/suffix
     * @return a node builder instance
     * @throws NullPointerException if the type or value is null
     * @since 3.2
     */
    Node.Builder makeChatMetaNode(ChatMetaType type, int priority, String value);

    /**
     * Creates a node builder from a prefix string and priority
     *
     * @param priority the priority
     * @param prefix   the prefix string
     * @return a node builder instance
     * @throws NullPointerException if the prefix is null
     */
    Node.Builder makePrefixNode(int priority, String prefix);

    /**
     * Creates a node builder from a prefix string and priority
     *
     * @param priority the priority
     * @param suffix   the suffix string
     * @return a node builder instance
     * @throws NullPointerException if the suffix is null
     */
    Node.Builder makeSuffixNode(int priority, String suffix);

}
