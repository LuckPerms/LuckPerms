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

package net.luckperms.api.model.data;

import net.luckperms.api.node.Node;

/**
 * Controls how the implementation should behave when new temporary nodes are set
 * that would otherwise conflict with existing entries.
 *
 * <p>The default behaviour of {@link NodeMap#add(Node)} is
 * to return a result of {@link DataMutateResult#FAIL_ALREADY_HAS} when an equivalent
 * node is found. This can be replicated using {@link #NONE}.</p>
 *
 * <p>However, the {@link NodeMap#add(Node, TemporaryNodeMergeStrategy)}
 * method allows this behaviour to be customized for temporary permissions.</p>
 */
public enum TemporaryNodeMergeStrategy {

    /**
     * Expiry durations will be added to the existing expiry time of a permission.
     */
    ADD_NEW_DURATION_TO_EXISTING,

    /**
     * Expiry durations will be replaced if the new duration is longer than the current one.
     */
    REPLACE_EXISTING_IF_DURATION_LONGER,

    /**
     * The operation will fail if an existing temporary node is present.
     */
    NONE

}
