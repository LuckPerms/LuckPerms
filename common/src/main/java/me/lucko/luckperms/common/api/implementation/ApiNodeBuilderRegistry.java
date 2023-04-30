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

package me.lucko.luckperms.common.api.implementation;

import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.node.types.DisplayName;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.node.types.Permission;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.RegexPermission;
import me.lucko.luckperms.common.node.types.Suffix;
import me.lucko.luckperms.common.node.types.Weight;
import net.luckperms.api.node.NodeBuilder;
import net.luckperms.api.node.NodeBuilderRegistry;
import net.luckperms.api.node.types.DisplayNameNode;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.RegexPermissionNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.node.types.WeightNode;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class ApiNodeBuilderRegistry implements NodeBuilderRegistry {
    public static final ApiNodeBuilderRegistry INSTANCE = new ApiNodeBuilderRegistry();

    private ApiNodeBuilderRegistry() {

    }

    @Override
    public @NonNull NodeBuilder<?, ?> forKey(String key) {
        return NodeBuilders.determineMostApplicable(key);
    }

    @Override
    public PermissionNode.@NonNull Builder forPermission() {
        return Permission.builder();
    }

    @Override
    public RegexPermissionNode.@NonNull Builder forRegexPermission() {
        return RegexPermission.builder();
    }

    @Override
    public InheritanceNode.@NonNull Builder forInheritance() {
        return Inheritance.builder();
    }

    @Override
    public PrefixNode.@NonNull Builder forPrefix() {
        return Prefix.builder();
    }

    @Override
    public SuffixNode.@NonNull Builder forSuffix() {
        return Suffix.builder();
    }

    @Override
    public MetaNode.@NonNull Builder forMeta() {
        return Meta.builder();
    }

    @Override
    public WeightNode.@NonNull Builder forWeight() {
        return Weight.builder();
    }

    @Override
    public DisplayNameNode.@NonNull Builder forDisplayName() {
        return DisplayName.builder();
    }
}
