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

package net.luckperms.api.cacheddata;

import net.luckperms.api.node.Node;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the result of a cached data lookup.
 *
 * <p>You can find "the holder that has the node that caused this result"
 * using the following code:</p>
 * <p></p>
 * <blockquote>
 * <pre>
 * public static {@link net.luckperms.api.model.PermissionHolder.Identifier} holderThatHasTheNodeThatCausedTheResult(Result&lt;?, ?&gt; result) {
 *     {@link Node} node = result.node();
 *     if (node == null) {
 *         return null;
 *     }
 *     {@link net.luckperms.api.node.metadata.types.InheritanceOriginMetadata} origin = node.getMetadata(InheritanceOriginMetadata.KEY).orElse(null);
 *     if (origin == null) {
 *         return null;
 *     }
 *     return origin.getOrigin();
 * }
 * </pre>
 *
 * <p>Combined with the node itself, this is all the information needed to determine
 * the root cause of the result.</p>
 * </blockquote>
 *
 * <p>The nullability of {@link #result()} is purposely undefined to allow the
 * flexibility for methods using {@link Result} to declare it. In general, if the {@code T} type
 * has a nullable/undefined value, then the return of {@link #result()} will be non-null,
 * and if not, it will be nullable.</p>
 *
 * @param <T> the result type
 * @param <N> the node type
 * @since 5.4
 */
public interface Result<T, N extends Node> {

    /**
     * Gets the underlying result.
     *
     * @return the underlying result
     */
    T result();

    /**
     * Gets the node that caused the result.
     *
     * @return the causing node
     */
    @Nullable N node();

}
