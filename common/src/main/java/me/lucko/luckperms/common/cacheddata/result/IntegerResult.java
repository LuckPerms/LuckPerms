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

import net.luckperms.api.cacheddata.Result;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.WeightNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the result of an integer meta lookup
 *
 * @param <N> the node type
 */
public final class IntegerResult<N extends Node> implements Result<Integer, N> {

    /** The result */
    private final int result;
    /** The node that caused the result */
    private final N node;
    /** A reference to another result that this one overrides */
    private IntegerResult<N> overriddenResult;

    public IntegerResult(int result, N node, IntegerResult<N> overriddenResult) {
        this.result = result;
        this.node = node;
        this.overriddenResult = overriddenResult;
    }

    @Override
    @Deprecated // use intResult()
    public @NonNull Integer result() {
        return this.result;
    }

    public int intResult() {
        return this.result;
    }

    public StringResult<N> asStringResult() {
        if (isNull()) {
            return StringResult.nullResult();
        } else {
            StringResult<N> result = StringResult.of(Integer.toString(this.result), this.node);
            if (this.overriddenResult != null) {
                result.setOverriddenResult(this.overriddenResult.asStringResult());
            }
            return result;
        }
    }

    @Override
    public @Nullable N node() {
        return this.node;
    }

    public @Nullable IntegerResult<N> overriddenResult() {
        return this.overriddenResult;
    }

    public void setOverriddenResult(IntegerResult<N> overriddenResult) {
        this.overriddenResult = overriddenResult;
    }

    public boolean isNull() {
        return this == NULL_RESULT;
    }

    public IntegerResult<N> copy() {
        return new IntegerResult<>(this.result, this.node, this.overriddenResult);
    }

    @Override
    public String toString() {
        return "IntegerResult(" +
                "result=" + this.result + ", " +
                "node=" + this.node + ", " +
                "overriddenResult=" + this.overriddenResult + ')';
    }

    private static final IntegerResult<?> NULL_RESULT = new IntegerResult<>(0, null, null);

    @SuppressWarnings("unchecked")
    public static <N extends Node> IntegerResult<N> nullResult() {
        return (IntegerResult<N>) NULL_RESULT;
    }

    public static <N extends Node> IntegerResult<N> of(int result) {
        return new IntegerResult<>(result, null, null);
    }

    public static <N extends Node> IntegerResult<N> of(int result, N node) {
        return new IntegerResult<>(result, node, null);
    }

    public static IntegerResult<WeightNode> of(WeightNode node) {
        return new IntegerResult<>(node.getWeight(), node, null);
    }

}
