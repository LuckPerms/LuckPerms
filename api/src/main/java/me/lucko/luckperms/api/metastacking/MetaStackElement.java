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

package me.lucko.luckperms.api.metastacking;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Represents an element within a {@link MetaStackDefinition}.
 *
 * <p>The element itself does not contain any mutable state.</p>
 *
 * @since 3.2
 */
@Immutable
public interface MetaStackElement {

    /**
     * Returns if the given node should be accumulated onto the stack.
     *
     * <p>The element being considered can be retrieved using {@link ChatMetaType#getEntry(Node)}.</p>
     *
     * @param node the node being considered
     * @param type the type of entry being accumulated
     * @param current the current value being used. If this returns true, the current value will be replaced by this entry
     * @return true if the node should be accumulated into this element, replacing the current value
     */
    boolean shouldAccumulate(@Nonnull LocalizedNode node, @Nonnull ChatMetaType type, @Nullable Map.Entry<Integer, String> current);

}
