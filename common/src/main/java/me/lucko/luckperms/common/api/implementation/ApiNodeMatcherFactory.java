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

import me.lucko.luckperms.common.node.matcher.StandardNodeMatchers;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.matcher.NodeMatcher;
import net.luckperms.api.node.matcher.NodeMatcherFactory;
import net.luckperms.api.node.types.MetaNode;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

public final class ApiNodeMatcherFactory implements NodeMatcherFactory {
    public static final ApiNodeMatcherFactory INSTANCE = new ApiNodeMatcherFactory();

    private ApiNodeMatcherFactory() {

    }

    @Override
    public @NonNull NodeMatcher<Node> key(@NonNull String key) {
        Objects.requireNonNull(key, "key");
        return StandardNodeMatchers.key(key);
    }

    @Override
    public <T extends Node> @NonNull NodeMatcher<T> key(@NonNull T node) {
        return StandardNodeMatchers.key(node);
    }

    @Override
    public @NonNull NodeMatcher<Node> keyStartsWith(@NonNull String startingWith) {
        Objects.requireNonNull(startingWith, "startingWith");
        return StandardNodeMatchers.keyStartsWith(startingWith);
    }

    @Override
    public <T extends Node> @NonNull NodeMatcher<T> equals(@NonNull T other, @NonNull NodeEqualityPredicate equalityPredicate) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(equalityPredicate, "equalityPredicate");
        return StandardNodeMatchers.equals(other, equalityPredicate);
    }

    @Override
    public @NonNull NodeMatcher<MetaNode> metaKey(@NonNull String metaKey) {
        Objects.requireNonNull(metaKey, "metaKey");
        return StandardNodeMatchers.metaKey(metaKey);
    }

    @Override
    public <T extends Node> @NonNull NodeMatcher<T> type(NodeType<? extends T> type) {
        Objects.requireNonNull(type, "type");
        return StandardNodeMatchers.type(type);
    }
}
