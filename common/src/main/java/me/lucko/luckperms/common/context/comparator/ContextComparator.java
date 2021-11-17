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

import net.luckperms.api.context.Context;

import java.util.Comparator;

public class ContextComparator implements Comparator<Context> {

    public static final ContextComparator INSTANCE = new ContextComparator(false);

    public static final ContextComparator ONLY_KEY = new ContextComparator(true);

    private final boolean onlyKeys;

    public ContextComparator(boolean onlyKeys) {
        this.onlyKeys = onlyKeys;
    }

    @Override
    public int compare(Context o1, Context o2) {
        if (o1 == o2) {
            return 0;
        }

        int i = compareStringsFast(o1.getKey(), o2.getKey());
        if (i != 0) {
            return i;
        }

        if (this.onlyKeys) {
            return 0;
        }

        return compareStringsFast(o1.getValue(), o2.getValue());
    }

    public int compare(Context o1, String o2Key, String o2Value) {
        int i = compareStringsFast(o1.getKey(), o2Key);
        if (i != 0) {
            return i;
        }

        return compareStringsFast(o1.getValue(), o2Value);
    }

    @SuppressWarnings("StringEquality")
    private static int compareStringsFast(String o1, String o2) {
        return o1 == o2 ? 0 : o1.compareTo(o2);
    }
}
