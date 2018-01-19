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

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.metastacking.MetaStackElement;

import java.util.Map;
import java.util.Optional;

final class SimpleMetaStackEntry implements MetaStackEntry {

    private final MetaStack parentStack;
    private final MetaStackElement element;
    private final ChatMetaType type;

    private Map.Entry<Integer, String> current = null;

    public SimpleMetaStackEntry(MetaStack parentStack, MetaStackElement element, ChatMetaType type) {
        this.parentStack = parentStack;
        this.element = element;
        this.type = type;
    }

    @Override
    public Optional<Map.Entry<Integer, String>> getCurrentValue() {
        return Optional.ofNullable(this.current);
    }

    @Override
    public boolean accumulateNode(LocalizedNode node) {
        if (this.element.shouldAccumulate(node, this.type, this.current)) {
            this.current = this.type.getEntry(node);
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

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof SimpleMetaStackEntry)) return false;
        final SimpleMetaStackEntry that = (SimpleMetaStackEntry) o;

        return this.getElement().equals(that.getElement()) &&
                this.getType() == that.getType() &&
                this.current.equals(that.current);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getElement().hashCode();
        result = result * PRIME + this.getType().hashCode();
        result = result * PRIME + this.current.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SimpleMetaStackEntry(parentStack=" + this.getParentStack() + ", element=" + this.getElement() + ", type=" + this.getType() + ", current=" + this.current + ")";
    }
}
