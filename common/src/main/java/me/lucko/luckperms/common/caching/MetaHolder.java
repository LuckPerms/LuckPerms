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

package me.lucko.luckperms.common.caching;

import lombok.Getter;
import lombok.ToString;
import me.lucko.luckperms.api.Node;

import java.util.*;

/**
 * Holds temporary mutable meta whilst this object is passed up the inheritance tree to accumulate meta from parents
 */
@Getter
@ToString
public class MetaHolder {

    private final Map<String, String> meta = new HashMap<>();
    private final SortedMap<Integer, String> prefixes = new TreeMap<>(Comparator.reverseOrder());
    private final SortedMap<Integer, String> suffixes = new TreeMap<>(Comparator.reverseOrder());

    public void accumulateNode(Node n) {
        if (n.isMeta()) {
            Map.Entry<String, String> entry = n.getMeta();
            if (!meta.containsKey(entry.getKey())) {
                meta.put(entry.getKey(), entry.getValue());
            }
        }

        if (n.isPrefix()) {
            Map.Entry<Integer, String> value = n.getPrefix();
            if (!prefixes.containsKey(value.getKey())) {
                prefixes.put(value.getKey(), value.getValue());
            }
        }

        if (n.isSuffix()) {
            Map.Entry<Integer, String> value = n.getSuffix();
            if (!suffixes.containsKey(value.getKey())) {
                suffixes.put(value.getKey(), value.getValue());
            }
        }
    }

}
