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

package net.luckperms.api.context;

import net.luckperms.api.query.OptionKey;

/**
 * Mode for determining whether a {@link ContextSet} satisfies another.
 *
 * @since 5.2
 * @see ContextSet#isSatisfiedBy(ContextSet, ContextSatisfyMode)
 */
public enum ContextSatisfyMode {

    /**
     * Mode where a {@link ContextSet} A will be satisfied by another set B, if at least one of the
     * key-value entries per key in A are also in B.
     *
     * <p>For example, given <code>A = {server=survival, world=overworld, world=nether}</code>,
     * another set {@code X} will satisfy {@code A} if {@code X} contains
     * <code>server=survival AND (world=overworld OR world=nether)</code>.</p>
     */
    AT_LEAST_ONE_VALUE_PER_KEY,

    /**
     * Mode where a {@link ContextSet} A will be satisfied by another set B, if all key-value
     * entries in A are also in B.
     *
     * <p>For example, given <code>A = {server=survival, world=overworld, world=nether}</code>,
     * another set {@code X} will satisfy {@code A} if {@code X} contains
     * <code>server=survival AND world=overworld AND world=nether</code>.</p>
     */
    ALL_VALUES_PER_KEY;

    /**
     * The {@link OptionKey} for {@link ContextSatisfyMode}.
     */
    public static final OptionKey<ContextSatisfyMode> KEY = OptionKey.of("contextsatisfymode", ContextSatisfyMode.class);

}
