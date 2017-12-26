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

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.context.ImmutableContextSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ContextSetComparator implements Comparator<ImmutableContextSet> {

    private static final Comparator<ImmutableContextSet> INSTANCE = new ContextSetComparator();
    private static final Comparator<ImmutableContextSet> REVERSE = INSTANCE.reversed();

    public static Comparator<ImmutableContextSet> normal() {
        return INSTANCE;
    }

    public static Comparator<ImmutableContextSet> reverse() {
        return REVERSE;
    }

    @Override
    public int compare(ImmutableContextSet o1, ImmutableContextSet o2) {
        if (o1.equals(o2)) {
            return 0;
        }

        boolean o1ServerSpecific = o1.containsKey(Contexts.SERVER_KEY);
        boolean o2ServerSpecific = o2.containsKey(Contexts.SERVER_KEY);
        if (o1ServerSpecific != o2ServerSpecific) {
            return o1ServerSpecific ? 1 : -1;
        }

        boolean o1WorldSpecific = o1.containsKey(Contexts.WORLD_KEY);
        boolean o2WorldSpecific = o2.containsKey(Contexts.WORLD_KEY);
        if (o1WorldSpecific != o2WorldSpecific) {
            return o1WorldSpecific ? 1 : -1;
        }

        int o1Size = o1.size();
        int o2Size = o2.size();
        if (o1Size != o2Size) {
            return o1Size > o2Size ? 1 : -1;
        }

        // we *have* to maintain transitivity in this comparator. this may be expensive, but it's necessary, as this
        // comparator is used in the PermissionHolder nodes treemap

        // in order to have consistent ordering, we have to compare the content of the context sets by ordering the
        // elements and then comparing which set is greater.
        List<Map.Entry<String, String>> o1Map = new ArrayList<>(o1.toSet());
        List<Map.Entry<String, String>> o2Map = new ArrayList<>(o2.toSet());
        o1Map.sort(STRING_ENTRY_COMPARATOR);
        o2Map.sort(STRING_ENTRY_COMPARATOR);

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
            //noinspection StringEquality - strings are intern'd
            if (ent1.getKey() == ent2.getKey() && ent1.getValue() == ent2.getValue()) {
                // identical entries. just move on
                continue;
            }

            // these entries are at the same position in the ordered sets.
            // if ent1 is "greater" than ent2, then at this first position, o1 has a "greater" entry, and can therefore be considered
            // a greater set, and vice versa
            return STRING_ENTRY_COMPARATOR.compare(ent1, ent2);
        }

        // shouldn't ever reach this point.
        return 0;
    }

    private static final Comparator<String> FAST_STRING_COMPARATOR = (o1, o2) -> {
        //noinspection StringEquality
        return o1 == o2 ? 0 : o1.compareTo(o2);
    };

    private static final Comparator<Map.Entry<String, String>> STRING_ENTRY_COMPARATOR = (o1, o2) -> {
        int ret = FAST_STRING_COMPARATOR.compare(o1.getKey(), o2.getKey());
        if (ret != 0) {
            return ret;
        }

        return FAST_STRING_COMPARATOR.compare(o1.getValue(), o2.getValue());
    };
}
