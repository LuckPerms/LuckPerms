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

package me.lucko.luckperms.common.cacheddata.result;

import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.ChatMetaNode;
import net.luckperms.api.node.types.MetaNode;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the result of a meta lookup
 *
 * @param <N> the node type
 */
public final class StringResult<N extends Node> extends AbstractResult<String, N, StringResult<N>> {

    /** The result, nullable */
    private final String result;

    public StringResult(String result, N node, StringResult<N> overriddenResult) {
        super(node, overriddenResult);
        this.result = result;
    }

    @Override
    public @Nullable String result() {
        return this.result;
    }

    public StringResult<N> copy() {
        return new StringResult<>(this.result, this.node, this.overriddenResult);
    }

    @Override
    public String toString() {
        return "StringResult(" +
                "result=" + this.result + ", " +
                "node=" + this.node + ", " +
                "overriddenResult=" + this.overriddenResult + ')';
    }

    private static final StringResult<?> NULL_RESULT = new StringResult<>(null, null, null);

    @SuppressWarnings("unchecked")
    public static <N extends Node> StringResult<N> nullResult() {
        return (StringResult<N>) NULL_RESULT;
    }

    public static <N extends Node> StringResult<N> of(String result) {
        return new StringResult<>(result, null, null);
    }

    public static <N extends Node> StringResult<N> of(String result, N node) {
        return new StringResult<>(result, node, null);
    }

    public static StringResult<MetaNode> of(MetaNode node) {
        return new StringResult<>(node.getMetaValue(), node, null);
    }

    public static <N extends ChatMetaNode<?, ?>> StringResult<N> of(N node) {
        return new StringResult<>(node.getMetaValue(), node, null);
    }

}
