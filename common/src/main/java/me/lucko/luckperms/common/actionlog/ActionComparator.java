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

package me.lucko.luckperms.common.actionlog;

import net.luckperms.api.actionlog.Action;

import java.util.Comparator;
import java.util.Optional;

final class ActionComparator implements Comparator<Action> {
    public static final Comparator<Action> INSTANCE = new ActionComparator();

    private ActionComparator() {

    }

    @Override
    public int compare(Action o1, Action o2) {
        int cmp = o1.getTimestamp().compareTo(o2.getTimestamp());
        if (cmp != 0) {
            return cmp;
        }

        Action.Source o1Source = o1.getSource();
        Action.Source o2Source = o2.getSource();

        cmp = o1Source.getUniqueId().compareTo(o2Source.getUniqueId());
        if (cmp != 0) {
            return cmp;
        }

        cmp = o1Source.getName().compareTo(o2Source.getName());
        if (cmp != 0) {
            return cmp;
        }

        Action.Target o1Target = o1.getTarget();
        Action.Target o2Target = o2.getTarget();

        cmp = o1Target.getType().compareTo(o2Target.getType());
        if (cmp != 0) {
            return cmp;
        }

        cmp = o1Target.getType().compareTo(o2Target.getType());
        if (cmp != 0) {
            return cmp;
        }

        cmp = compareOptionals(o1Target.getUniqueId(), o2Target.getUniqueId());
        if (cmp != 0) {
            return cmp;
        }

        cmp = o1Source.getName().compareTo(o2Source.getName());
        if (cmp != 0) {
            return cmp;
        }

        return o1.getDescription().compareTo(o2.getDescription());
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalIsPresent"})
    private static  <T extends Comparable<T>> int compareOptionals(Optional<T> a, Optional<T> b) {
        if (!a.isPresent()) {
            return b.isPresent() ? -1 : 0;
        } else if (!b.isPresent()) {
            return 1;
        } else {
            return a.get().compareTo(b.get());
        }
    }
}
