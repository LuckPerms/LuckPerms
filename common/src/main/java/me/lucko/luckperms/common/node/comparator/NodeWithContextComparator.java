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

import me.lucko.luckperms.api.node.Node;
import me.lucko.luckperms.common.context.ContextSetComparator;

import java.util.Comparator;

/**
 * Compares permission nodes based upon their supposed "priority".
 */
public class NodeWithContextComparator implements Comparator<Node> {
    private NodeWithContextComparator() {}

    private static final Comparator<? super Node> INSTANCE = new NodeWithContextComparator();
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

        int result = ContextSetComparator.normal().compare(
                o1.getContexts(),
                o2.getContexts()
        );

        if (result != 0) {
            return result;
        }

        return NodeComparator.normal().compare(o1, o2);
    }

}
