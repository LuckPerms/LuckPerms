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

package me.lucko.luckperms.common.node.utils;

import me.lucko.luckperms.api.node.Node;
import me.lucko.luckperms.api.node.NodeEqualityPredicate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class NodeTools {
    private NodeTools() {}

    public static <T extends Node> void removeEqual(Iterator<T> it, NodeEqualityPredicate equalityPredicate) {
        List<T> alreadyIn = new ArrayList<>();

        iterate:
        while (it.hasNext()) {
            T next = it.next();

            for (T other : alreadyIn) {
                if (next.equals(other, equalityPredicate)) {
                    it.remove();
                    continue iterate;
                }
            }

            alreadyIn.add(next);
        }
    }

    public static <T extends Node> void removeSamePermission(Iterator<T> it) {
        Set<String> alreadyIn = new HashSet<>();
        while (it.hasNext()) {
            T next = it.next();
            if (!alreadyIn.add(next.getKey())) {
                it.remove();
            }
        }
    }

}
