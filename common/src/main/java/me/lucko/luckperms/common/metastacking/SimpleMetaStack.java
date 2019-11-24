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

import me.lucko.luckperms.common.util.ImmutableCollectors;

import net.luckperms.api.metastacking.MetaStackDefinition;
import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.node.types.ChatMetaNode;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class SimpleMetaStack implements MetaStack {

    private final MetaStackDefinition definition;
    private final ChatMetaType targetType;

    private final List<MetaStackEntry> entries;

    public SimpleMetaStack(MetaStackDefinition definition, ChatMetaType targetType) {
        this.definition = definition;
        this.targetType = targetType;
        this.entries = definition.getElements().stream()
                .map(element -> new SimpleMetaStackEntry(this, element, targetType))
                .collect(ImmutableCollectors.toList());
    }

    @Override
    public String toFormattedString() {
        List<String> elements = this.entries.stream()
                .map(MetaStackEntry::getCurrentValue)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ChatMetaNode::getMetaValue)
                .collect(Collectors.toCollection(LinkedList::new));

        if (elements.isEmpty()) {
            return null;
        }

        this.definition.getDuplicateRemovalFunction().processDuplicates(elements);

        StringBuilder sb = new StringBuilder();
        sb.append(this.definition.getStartSpacer());
        for (int i = 0; i < elements.size(); i++) {
            if (i != 0) {
                sb.append(this.definition.getMiddleSpacer());
            }
            sb.append(elements.get(i));
        }
        sb.append(this.definition.getEndSpacer());

        return sb.toString();
    }

    @Override
    public void accumulateToAll(ChatMetaNode<?, ?> node) {
        this.entries.forEach(e -> e.accumulateNode(node));
    }

    @Override
    public MetaStackDefinition getDefinition() {
        return this.definition;
    }

    @Override
    public ChatMetaType getTargetType() {
        return this.targetType;
    }
}
