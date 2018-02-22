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

package me.lucko.luckperms.api;

import javax.annotation.Nonnull;

/**
 * Standard {@link NodeEqualityPredicate}s.
 *
 * @since 4.1
 */
public enum StandardNodeEquality implements NodeEqualityPredicate {

    /**
     * Represents an exact match.
     *
     * <p>All attributes of the nodes must match for them to be considered
     * equal.</p>
     */
    EXACT,

    /**
     * All attributes must match, except for
     * {@link Node#getValuePrimitive() value}, which is ignored.
     */
    IGNORE_VALUE,

    /**
     * All attributes must match, except for the
     * {@link Node#getExpiry() expiry time}, which is ignored.
     *
     * <p>Note that with this setting, whether a node is temporary or not is
     * still considered.</p>
     */
    IGNORE_EXPIRY_TIME,

    /**
     * All attributes must match, except for
     * {@link Node#getValuePrimitive() value} and the
     * {@link Node#getExpiry() expiry time}, which are ignored.
     */
    IGNORE_EXPIRY_TIME_AND_VALUE,

    /**
     * All attributes must match, except for
     * {@link Node#getValuePrimitive() value} and the if the node is
     * {@link Node#isTemporary() temporary}, which are ignored.
     */
    IGNORE_VALUE_OR_IF_TEMPORARY;

    @Override
    public boolean areEqual(@Nonnull Node o1, @Nonnull Node o2) {
        return o1.standardEquals(o2, this);
    }
}
