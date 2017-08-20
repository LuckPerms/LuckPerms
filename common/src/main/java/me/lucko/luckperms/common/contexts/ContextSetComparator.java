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

package me.lucko.luckperms.common.contexts;

import me.lucko.luckperms.api.context.ImmutableContextSet;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

public class ContextSetComparator implements Comparator<ImmutableContextSet> {
    private static final Comparator<Map.Entry<String, String>> STRING_ENTRY_COMPARATOR = (o1, o2) -> {
        int ret = o1.getKey().compareTo(o2.getKey());
        if (ret != 0) {
            return ret;
        }

        return o1.getValue().compareTo(o2.getValue());
    };

    private static final ContextSetComparator INSTANCE = new ContextSetComparator();
    public static Comparator<ImmutableContextSet> get() {
        return INSTANCE;
    }

    public static Comparator<ImmutableContextSet> reverse() {
        return INSTANCE.reversed();
    }

    @Override
    public int compare(ImmutableContextSet o1, ImmutableContextSet o2) {
        if (o1.equals(o2)) {
            return 0;
        }

        boolean o1ServerSpecific = o1.containsKey("server");
        boolean o2ServerSpecific = o2.containsKey("server");
        if (o1ServerSpecific != o2ServerSpecific) {
            return o1ServerSpecific ? 1 : -1;
        }

        boolean o1WorldSpecific = o1.containsKey("world");
        boolean o2WorldSpecific = o2.containsKey("world");
        if (o1WorldSpecific != o2WorldSpecific) {
            return o1WorldSpecific ? 1 : -1;
        }

        int o1Size = o1.size();
        int o2Size = o2.size();
        if (o1Size != o2Size) {
            return o1Size > o2Size ? 1 : -1;
        }

        // we *have* to maintain transitivity in this comparator. this may be expensive, but it's necessary, as these
        // values are stored in a treemap.

        // in order to have consistent ordering, we have to compare the content of the context sets by ordering the
        // elements and then comparing which set is greater.
        TreeSet<Map.Entry<String, String>> o1Map = new TreeSet<>(STRING_ENTRY_COMPARATOR);
        TreeSet<Map.Entry<String, String>> o2Map = new TreeSet<>(STRING_ENTRY_COMPARATOR);

        o1Map.addAll(o1.toMultimap().entries());
        o2Map.addAll(o2.toMultimap().entries());

        int o1MapSize = o1Map.size();
        int o2MapSize = o2Map.size();
        if (o1MapSize != o2MapSize) {
            return o1MapSize > o2MapSize ? 1 : -1;
        }

        // size is definitely the same
        Iterator<Map.Entry<String, String>> it1 = o1Map.iterator();
        Iterator<Map.Entry<String, String>> it2 = o2Map.iterator();

        while (it1.hasNext()) {
            Map.Entry<String, String> ent1 = it1.next();
            Map.Entry<String, String> ent2 = it2.next();

            // compare these values.
            if (ent1.getKey().equals(ent2.getKey()) && ent1.getValue().equals(ent2.getValue())) {
                // identical entries. just move on
                continue;
            }

            // these values are at the same position in the ordered sets.
            // if ent1 is "greater" than ent2, then at this first position, o1 has a "greater" entry, and can therefore be considered
            // a greater set.
            return STRING_ENTRY_COMPARATOR.compare(ent1, ent2);
        }

        // shouldn't ever reach this point. ever.
        return 0;
    }
}
