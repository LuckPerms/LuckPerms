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

package me.lucko.luckperms.common.node;

import lombok.experimental.UtilityClass;

import me.lucko.luckperms.api.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@UtilityClass
public class NodeTools {

    public static <T extends Node> void removeAlmostEqual(Iterator<T> it) {
        List<T> alreadyIn = new ArrayList<>();

        iter:
        while (it.hasNext()) {
            T next = it.next();
            for (T n : alreadyIn) {
                if (next.almostEquals(n)) {
                    it.remove();
                    continue iter;
                }
            }

            alreadyIn.add(next);
        }
    }

    public static <T extends Node> void removeIgnoreValue(Iterator<T> it) {
        List<T> alreadyIn = new ArrayList<>();

        iter:
        while (it.hasNext()) {
            T next = it.next();
            for (T n : alreadyIn) {
                if (next.equalsIgnoringValue(n)) {
                    it.remove();
                    continue iter;
                }
            }

            alreadyIn.add(next);
        }
    }

    public static <T extends Node> void removeIgnoreValueOrTemp(Iterator<T> it) {
        List<T> alreadyIn = new ArrayList<>();

        iter:
        while (it.hasNext()) {
            T next = it.next();
            for (T n : alreadyIn) {
                if (next.equalsIgnoringValueOrTemp(n)) {
                    it.remove();
                    continue iter;
                }
            }

            alreadyIn.add(next);
        }
    }

    public static <T extends Node> void removeSamePermission(Iterator<T> it) {
        Set<String> alreadyIn = new HashSet<>();

        while (it.hasNext()) {
            T next = it.next();

            if (!alreadyIn.add(next.getPermission())) {
                it.remove();
            }
        }
    }
}
