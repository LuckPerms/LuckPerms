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
 * Accumulates {@link ChatMetaNode}s for a {@link MetaStackElement}
 * to provide the stack resolution.
 *
 * @param <N> the node type
 */
public interface ElementAccumulator<N extends ChatMetaNode<?, ?>> {

    /**
     * Creates a {@link ElementAccumulator} for the given {@link MetaStackElement}.
     *
     * @param element the element
     * @param type the type
     * @param <N> the node type
     * @return the new accumulator
     */
    static <N extends ChatMetaNode<?, ?>> ElementAccumulator<N> create(MetaStackElement element, ChatMetaType type) {
        // if the element is dynamic, use its provided impl
        if (element instanceof DynamicMetaStackElement) {
            return ((DynamicMetaStackElement) element).newAccumulator(type);
        }

        // otherwise, just use the standard accumulator
        return new Standard<>(element, type);
    }

    /**
     * Offers a node to the accumulator.
     *
     * @param node the node
     */
    void offer(@NonNull N node);

    /**
     * Exports the final result from the accumulator.
     *
     * @return the result node
     */
    @Nullable N result();

    final class Standard<N extends ChatMetaNode<?, ?>> implements ElementAccumulator<N> {
        private final MetaStackElement element;
        private final ChatMetaType type;

        private @Nullable N current = null;

        Standard(MetaStackElement element, ChatMetaType type) {
            this.element = element;
            this.type = type;
        }

        @Override
        public void offer(@NonNull N node) {
            if (this.element.shouldAccumulate(this.type, node, this.current)) {
                this.current = node;
            }
        }

        @Override
        public @Nullable N result() {
            return this.current;
        }
    }

}
