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

import net.luckperms.api.metastacking.MetaStackElement;
import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.node.types.ChatMetaNode;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Special extension of {@link MetaStackElement} that provides its own {@link ElementAccumulator}
 * instead of using the {@link #shouldAccumulate(ChatMetaType, ChatMetaNode, ChatMetaNode)} method.
 */
public interface DynamicMetaStackElement extends MetaStackElement {

    @Deprecated
    @Override
    default boolean shouldAccumulate(@NonNull ChatMetaType type, @NonNull ChatMetaNode<?, ?> node, @Nullable ChatMetaNode<?, ?> current) {
        throw new UnsupportedOperationException("DynamicMetaStackElements do not support this method.");
    }

    /**
     * Creates a new {@link ElementAccumulator} for this element.
     *
     * @param type the type
     * @param <N> the node type
     * @return a new element accumulator
     */
    <N extends ChatMetaNode<?, ?>> ElementAccumulator<N> newAccumulator(ChatMetaType type);

}
