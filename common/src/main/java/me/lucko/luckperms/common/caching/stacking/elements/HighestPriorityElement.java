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

package me.lucko.luckperms.common.caching.stacking.elements;

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.common.caching.stacking.MetaStackElement;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class HighestPriorityElement implements MetaStackElement {

    /**
     * Returns true if the current node has the greater priority
     * @param current the current entry
     * @param newEntry the new entry
     * @return true if the accumulation should return
     */
    public static boolean compareEntries(Map.Entry<Integer, String> current, Map.Entry<Integer, String> newEntry) {
        return current != null && current.getKey() >= newEntry.getKey();
    }

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

        Map.Entry<Integer, String> entry = prefix ? node.getPrefix() : node.getSuffix();
        if (compareEntries(this.entry, entry)) {
            return false;
        }

        this.entry = entry;
        return true;
    }

    @Override
    public MetaStackElement copy() {
        return new HighestPriorityElement(prefix);
    }
}
