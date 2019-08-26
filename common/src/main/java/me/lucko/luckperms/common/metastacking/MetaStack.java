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

package me.lucko.luckperms.common.metastacking;

import net.luckperms.api.metastacking.MetaStackDefinition;
import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.node.types.ChatMetaNode;

/**
 * A live stack of {@link MetaStackEntry} instances, formed from a
 * {@link MetaStackDefinition}.
 *
 * This class is used to construct a formatted string, by accumulating
 * nodes to each element.
 */
public interface MetaStack {

    /**
     * Gets the definition this stack is based upon
     *
     * @return the definition of the stack
     */
    MetaStackDefinition getDefinition();

    /**
     * Gets the target of this stack
     *
     * @return the stack target
     */
    ChatMetaType getTargetType();

    /**
     * Returns a formatted string, as defined by the {@link MetaStackDefinition},
     * using the accumulated elements in the stack.
     *
     * @return the string output
     */
    String toFormattedString();

    /**
     * Tries to accumulate the given node to all elements in the stack.
     *
     * @param node the node to accumulate
     */
    void accumulateToAll(ChatMetaNode<?, ?> node);

}
