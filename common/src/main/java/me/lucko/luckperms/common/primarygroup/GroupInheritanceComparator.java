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

package me.lucko.luckperms.common.primarygroup;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeFactory;

import java.util.Comparator;

/**
 * Determines the order of group inheritance in {@link PermissionHolder}.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GroupInheritanceComparator implements Comparator<Group> {
    private static final Comparator<Group> NULL_ORIGIN = new GroupInheritanceComparator(null);

    public static Comparator<Group> getFor(PermissionHolder origin) {
        if (origin.getType().isUser()) {
            return new GroupInheritanceComparator(((User) origin));
        }
        return NULL_ORIGIN;
    }

    private final User origin;

    @Override
    public int compare(Group o1, Group o2) {
        int ret = Integer.compare(o1.getWeight().orElse(0), o2.getWeight().orElse(0));
        if (ret != 0) {
            // note negated value - we want higher weights first!
            return -ret;
        }

        // failing differing group weights, check if one of the groups is a primary group
        if (origin != null) {
            boolean o1Primary = o1.getName().equalsIgnoreCase(origin.getPrimaryGroup().getStoredValue().orElse(NodeFactory.DEFAULT_GROUP_NAME));
            boolean o2Primary = o2.getName().equalsIgnoreCase(origin.getPrimaryGroup().getStoredValue().orElse(NodeFactory.DEFAULT_GROUP_NAME));

            // one of them is a primary group, and therefore has priority
            if (o1Primary != o2Primary) {
                // we want the primary group to come first
                return o1Primary ? -1 : 1;
            }
        }

        // fallback to string based comparison
        return o1.getName().compareTo(o2.getName());
    }
}
