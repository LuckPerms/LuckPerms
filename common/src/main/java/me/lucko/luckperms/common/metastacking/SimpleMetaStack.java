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

import lombok.AccessLevel;
import lombok.Getter;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.metastacking.MetaStackDefinition;
import me.lucko.luckperms.common.utils.ImmutableCollectors;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class SimpleMetaStack implements MetaStack {

    private final MetaStackDefinition definition;
    private final ChatMetaType targetType;

    @Getter(AccessLevel.NONE)
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
        List<MetaStackEntry> ret = new ArrayList<>(entries);
        ret.removeIf(m -> !m.getCurrentValue().isPresent());

        if (ret.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(definition.getStartSpacer());
        for (int i = 0; i < ret.size(); i++) {
            if (i != 0) {
                sb.append(definition.getMiddleSpacer());
            }

            MetaStackEntry e = ret.get(i);
            sb.append(e.getCurrentValue().get().getValue());
        }
        sb.append(definition.getEndSpacer());

        return sb.toString();
    }

    @Override
    public void accumulateToAll(LocalizedNode node) {
        entries.forEach(e -> e.accumulateNode(node));
    }
}
