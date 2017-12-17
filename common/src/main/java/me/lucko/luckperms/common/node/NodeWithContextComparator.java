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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.utils.CollationKeyCache;

import java.util.Comparator;

/**
 * Compares permission nodes based upon their supposed "priority".
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NodeWithContextComparator implements Comparator<LocalizedNode> {

    private static final Comparator<LocalizedNode> INSTANCE = new NodeWithContextComparator();
    private static final Comparator<LocalizedNode> REVERSE = INSTANCE.reversed();

    public static Comparator<LocalizedNode> normal() {
        return INSTANCE;
    }

    public static Comparator<LocalizedNode> reverse() {
        return REVERSE;
    }

    @Override
    public int compare(LocalizedNode one, LocalizedNode two) {
        Node o1 = one.getNode();
        Node o2 = two.getNode();

        if (o1.equals(o2)) {
            return 0;
        }

        if (o1.isOverride() != o2.isOverride()) {
            return o1.isOverride() ? 1 : -1;
        }

        if (o1.isServerSpecific() != o2.isServerSpecific()) {
            return o1.isServerSpecific() ? 1 : -1;
        }

        if (o1.isWorldSpecific() != o2.isWorldSpecific()) {
            return o1.isWorldSpecific() ? 1 : -1;
        }

        if (o1.getContexts().size() != o2.getContexts().size()) {
            return o1.getContexts().size() > o2.getContexts().size() ? 1 : -1;
        }

        if (o1.isTemporary() != o2.isTemporary()) {
            return o1.isTemporary() ? 1 : -1;
        }

        if (o1.isWildcard() != o2.isWildcard()) {
            return o1.isWildcard() ? 1 : -1;
        }

        if (o1.isTemporary()) {
            return o1.getSecondsTilExpiry() < o2.getSecondsTilExpiry() ? 1 : -1;
        }

        if (o1.isWildcard()) {
            return o1.getWildcardLevel() > o2.getWildcardLevel() ? 1 : -1;
        }

        return CollationKeyCache.compareStrings(o1.getPermission(), o2.getPermission()) == 1 ? -1 : 1;
    }

}
