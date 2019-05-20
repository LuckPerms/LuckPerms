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

import me.lucko.luckperms.api.node.NodeBuilder;
import me.lucko.luckperms.api.node.NodeBuilderRegistry;
import me.lucko.luckperms.api.node.types.DisplayNameNode;
import me.lucko.luckperms.api.node.types.InheritanceNode;
import me.lucko.luckperms.api.node.types.MetaNode;
import me.lucko.luckperms.api.node.types.PermissionNode;
import me.lucko.luckperms.api.node.types.PrefixNode;
import me.lucko.luckperms.api.node.types.RegexPermissionNode;
import me.lucko.luckperms.api.node.types.SuffixNode;
import me.lucko.luckperms.api.node.types.WeightNode;
import me.lucko.luckperms.common.node.factory.NodeFactory;
import me.lucko.luckperms.common.node.types.DisplayName;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.node.types.Permission;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.RegexPermission;
import me.lucko.luckperms.common.node.types.Suffix;
import me.lucko.luckperms.common.node.types.Weight;

import org.checkerframework.checker.nullness.qual.NonNull;

public final class ApiNodeBuilderRegistry implements NodeBuilderRegistry {
    public static final ApiNodeBuilderRegistry INSTANCE = new ApiNodeBuilderRegistry();

    private ApiNodeBuilderRegistry() {

    }

    @Override
    public @NonNull NodeBuilder<?, ?> forKey(String key) {
        return NodeFactory.builder(key);
    }

    @Override
    public PermissionNode.@NonNull Builder forPermission() {
        return new Permission.Builder();
    }

    @Override
    public RegexPermissionNode.@NonNull Builder forRegexPermission() {
        return new RegexPermission.Builder();
    }

    @Override
    public InheritanceNode.@NonNull Builder forInheritance() {
        return new Inheritance.Builder();
    }

    @Override
    public PrefixNode.@NonNull Builder forPrefix() {
        return new Prefix.Builder();
    }

    @Override
    public SuffixNode.@NonNull Builder forSuffix() {
        return new Suffix.Builder();
    }

    @Override
    public MetaNode.@NonNull Builder forMeta() {
        return new Meta.Builder();
    }

    @Override
    public WeightNode.@NonNull Builder forWeight() {
        return new Weight.Builder();
    }

    @Override
    public DisplayNameNode.@NonNull Builder forDisplayName() {
        return new DisplayName.Builder();
    }
}
