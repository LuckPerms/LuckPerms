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

package me.lucko.luckperms.common.context;

import net.luckperms.api.context.Context;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

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

        int result = Boolean.compare(o1.containsKey(DefaultContextKeys.SERVER_KEY), o2.containsKey(DefaultContextKeys.SERVER_KEY));
        if (result != 0) {
            return result;
        }

        result = Boolean.compare(o1.containsKey(DefaultContextKeys.WORLD_KEY), o2.containsKey(DefaultContextKeys.WORLD_KEY));
        if (result != 0) {
            return result;
        }

        result = Integer.compare(o1.size(), o2.size());
        if (result != 0) {
            return result;
        }

        // we *have* to maintain transitivity in this comparator. this may be expensive, but it's necessary, as this
        // comparator is used in the PermissionHolder nodes treemap

        // in order to have consistent ordering, we have to compare the content of the context sets by ordering the
        // elements and then comparing which set is greater.
        List<Context> o1Entries = new ArrayList<>(o1.toSet());
        List<Context> o2Entries = new ArrayList<>(o2.toSet());
        o1Entries.sort(CONTEXT_COMPARATOR);
        o2Entries.sort(CONTEXT_COMPARATOR);

        // size is definitely the same
        Iterator<Context> it1 = o1Entries.iterator();
        Iterator<Context> it2 = o2Entries.iterator();

        while (it1.hasNext()) {
            Context ent1 = it1.next();
            Context ent2 = it2.next();

            int ret = CONTEXT_COMPARATOR.compare(ent1, ent2);
            if (ret != 0) {
                return ret;
            }
        }

        throw new AssertionError("sets are equal? " + o1 + " - " + o2);
    }

    @SuppressWarnings("StringEquality")
    private static final Comparator<String> FAST_STRING_COMPARATOR = (o1, o2) -> o1 == o2 ? 0 : o1.compareTo(o2);

    private static final Comparator<Context> CONTEXT_COMPARATOR = (o1, o2) -> {
        if (o1 == o2) {
            return 0;
        }

        int ret = FAST_STRING_COMPARATOR.compare(o1.getKey(), o2.getKey());
        if (ret != 0) {
            return ret;
        }

        return FAST_STRING_COMPARATOR.compare(o1.getValue(), o2.getValue());
    };
}
