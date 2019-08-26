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

package net.luckperms.api.node;

import net.luckperms.api.node.types.DisplayNameNode;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.RegexPermissionNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.node.types.WeightNode;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A registry of methods for obtaining {@link NodeBuilder}s for the various
 * node types.
 */
public interface NodeBuilderRegistry {

    /**
     * Gets a {@link NodeBuilder} applicable for the given key.
     *
     * @param key the key
     * @return the node builder
     */
    @NonNull NodeBuilder<?, ?> forKey(String key);

    /**
     * Gets a {@link NodeBuilder} for {@link PermissionNode}s.
     *
     * @return the node builder
     */
    PermissionNode.@NonNull Builder forPermission();

    /**
     * Gets a {@link NodeBuilder} for {@link RegexPermissionNode}s.
     *
     * @return the node builder
     */
    RegexPermissionNode.@NonNull Builder forRegexPermission();

    /**
     * Gets a {@link NodeBuilder} for {@link InheritanceNode}s.
     *
     * @return the node builder
     */
    InheritanceNode.@NonNull Builder forInheritance();

    /**
     * Gets a {@link NodeBuilder} for {@link PrefixNode}s.
     *
     * @return the node builder
     */
    PrefixNode.@NonNull Builder forPrefix();

    /**
     * Gets a {@link NodeBuilder} for {@link SuffixNode}s.
     *
     * @return the node builder
     */
    SuffixNode.@NonNull Builder forSuffix();

    /**
     * Gets a {@link NodeBuilder} for {@link MetaNode}s.
     *
     * @return the node builder
     */
    MetaNode.@NonNull Builder forMeta();

    /**
     * Gets a {@link NodeBuilder} for {@link WeightNode}s.
     *
     * @return the node builder
     */
    WeightNode.@NonNull Builder forWeight();

    /**
     * Gets a {@link NodeBuilder} for {@link DisplayNameNode}s.
     *
     * @return the node builder
     */
    DisplayNameNode.@NonNull Builder forDisplayName();

}
