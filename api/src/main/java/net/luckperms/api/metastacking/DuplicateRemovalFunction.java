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

package net.luckperms.api.metastacking;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Functional interface which removes duplicate entries from a list.
 *
 * <p>Used by LuckPerms to remove duplicate entries from a MetaStack.</p>
 */
public interface DuplicateRemovalFunction {

    /**
     * Removes duplicates from the given list, according to the behaviour
     * of the function.
     *
     * @param list the entries
     * @param <T> the type of entries
     */
    <T> void processDuplicates(@NonNull List<T> list);

    /**
     * A {@link DuplicateRemovalFunction} that does not remove duplicates.
     */
    DuplicateRemovalFunction RETAIN_ALL = new DuplicateRemovalFunction() {
        @Override
        public <T> void processDuplicates(@NonNull List<T> list) {

        }

        @Override
        public String toString() {
            return "DuplicateRemovalFunction#RETAIN_ALL";
        }
    };

    /**
     * A {@link DuplicateRemovalFunction} that retains only the first occurrence.
     */
    DuplicateRemovalFunction FIRST_ONLY = new DuplicateRemovalFunction() {
        @SuppressWarnings("Java8CollectionRemoveIf")
        @Override
        public <T> void processDuplicates(@NonNull List<T> list) {
            Set<T> seen = new HashSet<>(list.size());
            for (ListIterator<T> it = list.listIterator(); it.hasNext(); ) {
                T next = it.next();
                if (!seen.add(next)) {
                    it.remove();
                }
            }
        }

        @Override
        public String toString() {
            return "DuplicateRemovalFunction#FIRST_ONLY";
        }
    };

    /**
     * A {@link DuplicateRemovalFunction} that retains only the last occurrence.
     */
    DuplicateRemovalFunction LAST_ONLY = new DuplicateRemovalFunction() {
        @Override
        public <T> void processDuplicates(@NonNull List<T> list) {
            Set<T> seen = new HashSet<>(list.size());
            for (ListIterator<T> it = list.listIterator(list.size()); it.hasPrevious(); ) {
                T next = it.previous();
                if (!seen.add(next)) {
                    it.remove();
                }
            }
        }

        @Override
        public String toString() {
            return "DuplicateRemovalFunction#LAST_ONLY";
        }
    };

}
