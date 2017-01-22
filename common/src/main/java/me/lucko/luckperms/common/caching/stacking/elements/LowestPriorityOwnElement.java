/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.common.caching.stacking.elements;

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.common.caching.stacking.MetaStackElement;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class LowestPriorityOwnElement implements MetaStackElement {
    private final boolean prefix;
    private Map.Entry<Integer, String> entry = null;

    @Override
    public Optional<Map.Entry<Integer, String>> getEntry() {
        return Optional.ofNullable(entry);
    }

    @Override
    public boolean accumulateNode(LocalizedNode node) {
        if (MetaStackElement.checkMetaType(prefix, node)) {
            return false;
        }

        if (MetaStackElement.checkOwnElement(node)) {
            return false;
        }

        Map.Entry<Integer, String> entry = prefix ? node.getPrefix() : node.getSuffix();
        if (LowestPriorityElement.compareEntries(this.entry, entry)) {
            return false;
        }

        this.entry = entry;
        return true;
    }

    @Override
    public MetaStackElement copy() {
        return new LowestPriorityOwnElement(prefix);
    }
}
