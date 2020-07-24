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

package me.lucko.luckperms.common.cacheddata.type;

import net.luckperms.api.metastacking.MetaStackDefinition;
import net.luckperms.api.metastacking.MetaStackElement;
import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.node.types.ChatMetaNode;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MetaStackAccumulator {
    private final MetaStackDefinition definition;
    private final List<Entry> entries;

    public MetaStackAccumulator(MetaStackDefinition definition, ChatMetaType targetType) {
        this.definition = definition;

        List<MetaStackElement> elements = definition.getElements();
        this.entries = new ArrayList<>(elements.size());
        for (MetaStackElement element : elements) {
            this.entries.add(new Entry(element, targetType));
        }
    }

    public void offer(ChatMetaNode<?, ?> node) {
        for (Entry entry : this.entries) {
            entry.offer(node);
        }
    }

    public String toFormattedString() {
        List<String> elements = new LinkedList<>();
        for (Entry entry : this.entries) {
            ChatMetaNode<?, ?> node = entry.getNode();
            if (node != null) {
                elements.add(node.getMetaValue());
            }
        }

        if (elements.isEmpty()) {
            return null;
        }

        this.definition.getDuplicateRemovalFunction().processDuplicates(elements);

        Iterator<String> it = elements.iterator();
        if (!it.hasNext()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(this.definition.getStartSpacer());

        // append first - we've checked hasNext already
        sb.append(it.next());

        // append rest
        while (it.hasNext()){
            sb.append(this.definition.getMiddleSpacer());
            sb.append(it.next());
        }

        sb.append(this.definition.getEndSpacer());

        return sb.toString();
    }

    private static final class Entry {
        private final MetaStackElement element;
        private final ChatMetaType type;

        private @Nullable ChatMetaNode<?, ?> current = null;

        Entry(MetaStackElement element, ChatMetaType type) {
            this.element = element;
            this.type = type;
        }

        public ChatMetaNode<?, ?> getNode() {
            return this.current;
        }

        public boolean offer(ChatMetaNode<?, ?> node) {
            if (this.element.shouldAccumulate(this.type, node, this.current)) {
                this.current = node;
                return true;
            }
            return false;
        }
    }
}
