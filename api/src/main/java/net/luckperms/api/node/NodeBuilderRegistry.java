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
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * A registry of methods for obtaining {@link NodeBuilder}s for the various
 * node types.
 */
@Internal
public interface NodeBuilderRegistry {

    /**
     * Gets a {@link NodeBuilder} applicable for the given key.
     *
     * <p>Prefer using the {@link Node#builder(String)} method.</p>
     *
     * @param key the key
     * @return the node builder
     * @see Node#builder(String)
     */
    @NonNull NodeBuilder<?, ?> forKey(String key);

    /**
     * Gets a {@link NodeBuilder} for {@link PermissionNode}s.
     *
     * <p>Prefer using the {@link PermissionNode#builder()} method.</p>
     *
     * @return the node builder
     * @see PermissionNode#builder()
     */
    PermissionNode.@NonNull Builder forPermission();

    /**
     * Gets a {@link NodeBuilder} for {@link RegexPermissionNode}s.
     *
     * <p>Prefer using the {@link RegexPermissionNode#builder()} method.</p>
     *
     * @return the node builder
     * @see RegexPermissionNode#builder()
     */
    RegexPermissionNode.@NonNull Builder forRegexPermission();

    /**
     * Gets a {@link NodeBuilder} for {@link InheritanceNode}s.
     *
     * <p>Prefer using the {@link InheritanceNode#builder()} method.</p>
     *
     * @return the node builder
     * @see InheritanceNode#builder()
     */
    InheritanceNode.@NonNull Builder forInheritance();

    /**
     * Gets a {@link NodeBuilder} for {@link PrefixNode}s.
     *
     * <p>Prefer using the {@link PrefixNode#builder()} method.</p>
     *
     * @return the node builder
     * @see PrefixNode#builder()
     */
    PrefixNode.@NonNull Builder forPrefix();

    /**
     * Gets a {@link NodeBuilder} for {@link SuffixNode}s.
     *
     * <p>Prefer using the {@link SuffixNode#builder()} method.</p>
     *
     * @return the node builder
     * @see SuffixNode#builder()
     */
    SuffixNode.@NonNull Builder forSuffix();

    /**
     * Gets a {@link NodeBuilder} for {@link MetaNode}s.
     *
     * <p>Prefer using the {@link MetaNode#builder()} method.</p>
     *
     * @return the node builder
     * @see MetaNode#builder()
     */
    MetaNode.@NonNull Builder forMeta();

    /**
     * Gets a {@link NodeBuilder} for {@link WeightNode}s.
     *
     * <p>Prefer using the {@link WeightNode#builder()} method.</p>
     *
     * @return the node builder
     * @see WeightNode#builder()
     */
    WeightNode.@NonNull Builder forWeight();

    /**
     * Gets a {@link NodeBuilder} for {@link DisplayNameNode}s.
     *
     * <p>Prefer using the {@link DisplayNameNode#builder()} method.</p>
     *
     * @return the node builder
     * @see DisplayNameNode#builder()
     */
    DisplayNameNode.@NonNull Builder forDisplayName();

}
