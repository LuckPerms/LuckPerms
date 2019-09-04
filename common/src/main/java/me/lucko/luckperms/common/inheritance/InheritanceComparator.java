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

import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.GroupManager;

import java.util.Comparator;

/**
 * Determines the order of group inheritance in {@link PermissionHolder}.
 */
public class InheritanceComparator implements Comparator<PermissionHolder> {
    private static final Comparator<PermissionHolder> NULL_ORIGIN = new InheritanceComparator(null).reversed();

    public static Comparator<? super PermissionHolder> getFor(PermissionHolder origin) {
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
    public int compare(PermissionHolder o1, PermissionHolder o2) {
        // if both users, return 0
        // if one or the other is a user, give a higher priority to the user
        boolean o1IsUser = o1.getType() == HolderType.USER;
        boolean o2IsUser = o2.getType() == HolderType.USER;
        if (o1IsUser && o2IsUser) {
            return 0;
        } else if (o1IsUser) {
            return 1;
        } else if (o2IsUser) {
            return -1;
        }

        Group o1Group = (Group) o1;
        Group o2Group = (Group) o2;

        int result = Integer.compare(o1.getWeight().orElse(0), o2.getWeight().orElse(0));
        if (result != 0) {
            return result;
        }

        // failing differing group weights, check if one of the groups is a primary group
        if (this.origin != null) {
            return Boolean.compare(
                    o1Group.getName().equalsIgnoreCase(this.origin.getPrimaryGroup().getStoredValue().orElse(GroupManager.DEFAULT_GROUP_NAME)),
                    o2Group.getName().equalsIgnoreCase(this.origin.getPrimaryGroup().getStoredValue().orElse(GroupManager.DEFAULT_GROUP_NAME))
            );
        }

        // failing weight/primary group checks, fallback to the node ordering
        // this comparator is only ever used by Collections.sort - which is stable. the existing
        // ordering of the nodes will therefore apply.
        return 0;
    }
}
