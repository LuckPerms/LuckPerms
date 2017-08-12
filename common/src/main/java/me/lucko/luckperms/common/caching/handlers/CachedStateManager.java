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

package me.lucko.luckperms.common.caching.handlers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import me.lucko.luckperms.common.references.HolderReference;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages the cached state of all permission holders
 */
public class CachedStateManager {

    // Group --> Groups/Users that inherit from that group. (reverse relationship)
    private final Multimap<HolderReference, HolderReference> map = HashMultimap.create();
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Gets a set of holder names that inherit permissions (either directly or via other groups)
     * from the given holder name
     *
     * @param holder the holder name to query for
     * @return a set of inherited groups
     */
    public Set<HolderReference> getInheritances(HolderReference holder) {
        Set<HolderReference> set = new HashSet<>();
        set.add(holder);

        lock.lock();
        try {
            while (true) {
                Set<HolderReference> clone = new HashSet<>(set);

                boolean work = false;

                for (HolderReference s : clone) {
                    if (set.addAll(map.get(s))) {
                        work = true;
                    }
                }

                if (!work) {
                    break;
                }
            }
        } finally {
            lock.unlock();
        }

        set.remove(holder);
        return set;
    }

    /**
     * Registers a holder and the groups they inherit from within this map.
     *
     * @param holder the holder to add
     * @param inheritedGroups a list of groups the holder inherits from
     */
    public void putAll(HolderReference holder, Set<HolderReference> inheritedGroups) {
        lock.lock();
        try {
            map.entries().removeIf(entry -> entry.getValue().equals(holder));

            for (HolderReference child : inheritedGroups) {
                map.put(child, holder);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clears defined inheritances for the given holder name.
     *
     * @param holder the holder name to clear
     */
    public void clear(HolderReference holder) {
        lock.lock();
        try {
            map.entries().removeIf(entry -> entry.getValue().equals(holder));
        } finally {
            lock.unlock();
        }
    }

}
