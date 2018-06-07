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

package me.lucko.luckperms.common.node.comparator;

import me.lucko.luckperms.api.Node;

import java.util.Comparator;

public class NodeComparator implements Comparator<Node> {

    private static final Comparator<? super Node> INSTANCE = new NodeComparator();
    private static final Comparator<? super Node> REVERSE = INSTANCE.reversed();

    public static Comparator<? super Node> normal() {
        return INSTANCE;
    }

    public static Comparator<? super Node> reverse() {
        return REVERSE;
    }

    @Override
    public int compare(Node o1, Node o2) {
        if (o1.equals(o2)) {
            return 0;
        }

        int result = Boolean.compare(o1.isOverride(), o2.isOverride());
        if (result != 0) {
            return result;
        }

        result = Boolean.compare(o1.isTemporary(), o2.isTemporary());
        if (result != 0) {
            return result;
        }

        result = Boolean.compare(o1.isWildcard(), o2.isWildcard());
        if (result != 0) {
            return result;
        }

        if (o1.isTemporary()) {
            // note vvv
            result = -Long.compare(o1.getSecondsTilExpiry(), o2.getSecondsTilExpiry());
            if (result != 0) {
                return result;
            }
        }

        if (o1.isWildcard()) {
            result = Integer.compare(o1.getWildcardLevel(), o2.getWildcardLevel());
            if (result != 0) {
                return result;
            }
        }

        // note vvv
        result = -o1.getPermission().compareTo(o2.getPermission());
        if (result != 0) {
            return result;
        }

        // note vvv - we want false to have priority
        result = -Boolean.compare(o1.getValue(), o2.getValue());
        if (result != 0) {
            return result;
        }

        throw new AssertionError("nodes are equal? " + o1 + " - " + o2);
    }
}
