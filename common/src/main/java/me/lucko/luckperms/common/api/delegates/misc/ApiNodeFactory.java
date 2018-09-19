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

package me.lucko.luckperms.common.api.delegates.misc;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.api.delegates.model.ApiGroup;
import me.lucko.luckperms.common.node.factory.NodeFactory;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

public final class ApiNodeFactory implements me.lucko.luckperms.api.NodeFactory {
    public static final ApiNodeFactory INSTANCE = new ApiNodeFactory();

    private ApiNodeFactory() {

    }

    @Override
    public Node.@NonNull Builder newBuilder(@NonNull String permission) {
        Objects.requireNonNull(permission, "permission");
        return NodeFactory.builder(permission);
    }

    @Override
    public Node.@NonNull Builder newBuilderFromExisting(@NonNull Node other) {
        return Objects.requireNonNull(other, "other").toBuilder();
    }

    @Override
    public Node.@NonNull Builder makeGroupNode(@NonNull Group group) {
        Objects.requireNonNull(group, "group");
        return NodeFactory.buildGroupNode(ApiGroup.cast(group));
    }

    @Override
    public Node.@NonNull Builder makeGroupNode(@NonNull String groupName) {
        Objects.requireNonNull(groupName, "groupName");
        return NodeFactory.buildGroupNode(groupName);
    }

    @Override
    public Node.@NonNull Builder makeMetaNode(@NonNull String key, @NonNull String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        return NodeFactory.buildMetaNode(key, value);
    }

    @Override
    public Node.@NonNull Builder makeChatMetaNode(@NonNull ChatMetaType type, int priority, @NonNull String value) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        return NodeFactory.buildChatMetaNode(type, priority, value);
    }

    @Override
    public Node.@NonNull Builder makePrefixNode(int priority, @NonNull String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        return NodeFactory.buildPrefixNode(priority, prefix);
    }

    @Override
    public Node.@NonNull Builder makeSuffixNode(int priority, @NonNull String suffix) {
        Objects.requireNonNull(suffix, "suffix");
        return NodeFactory.buildSuffixNode(priority, suffix);
    }
}
