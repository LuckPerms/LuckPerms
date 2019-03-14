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

package me.lucko.luckperms.common.inheritance;

import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.comparator.NodeWithContextComparator;
import me.lucko.luckperms.common.node.factory.NodeFactory;

import java.util.Comparator;

/**
 * Determines the order of group inheritance in {@link PermissionHolder}.
 */
public class InheritanceComparator implements Comparator<ResolvedGroup> {
    private static final Comparator<ResolvedGroup> NULL_ORIGIN = new InheritanceComparator(null).reversed();

    public static Comparator<ResolvedGroup> getFor(PermissionHolder origin) {
        if (origin.getType() == HolderType.USER) {
            return new InheritanceComparator(((User) origin)).reversed();
        }
        return NULL_ORIGIN;
    }

    private final User origin;

    private InheritanceComparator(User origin) {
        this.origin = origin;
    }

    @Override
    public int compare(ResolvedGroup o1, ResolvedGroup o2) {
        int result = Integer.compare(o1.group().getWeight().orElse(0), o2.group().getWeight().orElse(0));
        if (result != 0) {
            return result;
        }

        // failing differing group weights, check if one of the groups is a primary group
        if (this.origin != null) {
            result = Boolean.compare(
                    o1.group().getName().equalsIgnoreCase(this.origin.getPrimaryGroup().getStoredValue().orElse(NodeFactory.DEFAULT_GROUP_NAME)),
                    o2.group().getName().equalsIgnoreCase(this.origin.getPrimaryGroup().getStoredValue().orElse(NodeFactory.DEFAULT_GROUP_NAME))
            );

            if (result != 0) {
                return result;
            }
        }

        // failing weight checks, fallback to which group applies in more specific context
        return NodeWithContextComparator.normal().compare(o1.node(), o2.node());
    }
}
