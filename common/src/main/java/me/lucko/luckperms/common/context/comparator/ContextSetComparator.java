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

package me.lucko.luckperms.common.context.comparator;

import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import net.luckperms.api.context.Context;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;

import java.util.Arrays;
import java.util.Comparator;

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

        // compare presence of any context (non-empty)
        int result = Boolean.compare(!o1.isEmpty(), !o2.isEmpty());
        if (result != 0) {
            return result;
        }

        // compare presence of a server context
        result = Boolean.compare(o1.containsKey(DefaultContextKeys.SERVER_KEY), o2.containsKey(DefaultContextKeys.SERVER_KEY));
        if (result != 0) {
            return result;
        }

        // compare presence of a world context
        result = Boolean.compare(o1.containsKey(DefaultContextKeys.WORLD_KEY), o2.containsKey(DefaultContextKeys.WORLD_KEY));
        if (result != 0) {
            return result;
        }

        // compare overall size
        result = Integer.compare(o1.size(), o2.size());
        if (result != 0) {
            return result;
        }

        // At this point, we don't really care about the order between the two sets.
        // However, we *have* to maintain transitivity in this comparator (despite how
        // expensive/complex it may be) as it is used in the PermissionHolder nodes treemap.

        // in order to have consistent ordering, we have to compare the content of the context sets.
        // to do this, obtain sorted array representations of each set, then compare which is greater
        Context[] o1Array = o1 instanceof ImmutableContextSetImpl ? ((ImmutableContextSetImpl) o1).toArray() : toArray(o1);
        Context[] o2Array = o2 instanceof ImmutableContextSetImpl ? ((ImmutableContextSetImpl) o2).toArray() : toArray(o2);

        for (int i = 0; i < o1Array.length; i++) {
            Context ent1 = o1Array[i];
            Context ent2 = o2Array[i];

            result = ContextComparator.INSTANCE.compare(ent1, ent2);
            if (result != 0) {
                return result;
            }
        }

        throw new AssertionError("sets are equal? " + o1 + " - " + o2);
    }

    private static Context[] toArray(ImmutableContextSet set) {
        Context[] array = set.toSet().toArray(new Context[0]);
        Arrays.sort(array, ContextComparator.INSTANCE);
        return array;
    }

}
