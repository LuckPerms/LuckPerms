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

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

final class SimpleMetaStackEntry implements MetaStackEntry {
    private final MetaStack parentStack;
    private final MetaStackElement element;
    private final ChatMetaType type;

    private @Nullable ChatMetaNode<?, ?> current = null;

    public SimpleMetaStackEntry(MetaStack parentStack, MetaStackElement element, ChatMetaType type) {
        this.parentStack = parentStack;
        this.element = element;
        this.type = type;
    }

    @Override
    public Optional<ChatMetaNode<?, ?>> getCurrentValue() {
        return Optional.ofNullable(this.current);
    }

    @Override
    public boolean accumulateNode(ChatMetaNode<?, ?> node) {
        if (this.element.shouldAccumulate(this.type, node, this.current)) {
            this.current = node;
            return true;
        }
        return false;
    }

    @Override
    public MetaStack getParentStack() {
        return this.parentStack;
    }

    @Override
    public MetaStackElement getElement() {
        return this.element;
    }

    public ChatMetaType getType() {
        return this.type;
    }
}
