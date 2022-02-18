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

package me.lucko.luckperms.common.util;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Records a log of the changes that occur as a result
 * of mutations (add or remove operations).
 *
 * @param <T> the value type
 */
public class Difference<T> {
    private final LinkedHashSet<Change<T>> changes = new LinkedHashSet<>();

    /**
     * Gets the recorded changes.
     *
     * @return the changes
     */
    public Set<Change<T>> getChanges() {
        return this.changes;
    }

    /**
     * Gets if no changes have been recorded.
     *
     * @return if no changes have been recorded
     */
    public boolean isEmpty() {
        return this.changes.isEmpty();
    }

    /**
     * Gets the recorded changes of a given type
     *
     * @param type the type of change
     * @return the changes
     */
    public Set<T> getChanges(ChangeType type) {
        Set<T> changes = new LinkedHashSet<>(this.changes.size());
        for (Change<T> change : this.changes) {
            if (change.type() == type) {
                changes.add(change.value());
            }
        }
        return changes;
    }

    /**
     * Gets the values that have been added.
     *
     * @return the added values
     */
    public Set<T> getAdded() {
        return getChanges(ChangeType.ADD);
    }

    /**
     * Gets the values that have been removed.
     *
     * @return the removed values
     */
    public Set<T> getRemoved() {
        return getChanges(ChangeType.REMOVE);
    }

    /**
     * Clears all recorded changes.
     */
    public void clear() {
        this.changes.clear();
    }

    private void recordChange(Change<T> change) {
        // This method is the magic of this class.
        // When tracking, we want to ignore changes that cancel each other out, and only
        // keep track of the net difference.
        // e.g. adding then removing the same value = zero net change, so ignore it.

        if (this.changes.remove(change.inverse())) {
            return;
        }
        this.changes.add(change);
    }

    /**
     * Records a change.
     *
     * @param type the type of change
     * @param value the changed value
     */
    public void recordChange(ChangeType type, T value) {
        recordChange(new Change<>(type, value));
    }

    /**
     * Records some changes.
     *
     * @param type the type of change
     * @param values the changed values
     */
    public void recordChanges(ChangeType type, Iterable<T> values) {
        for (T value : values) {
            recordChange(new Change<>(type, value));
        }
    }

    /**
     * Merges the recorded differences in {@code other} into this.
     *
     * @param other the other differences
     * @return this
     */
    public Difference<T> mergeFrom(Difference<T> other) {
        for (Change<T> change : other.changes) {
            recordChange(change);
        }
        return this;
    }

    @Override
    public String toString() {
        return "Difference{" + this.changes + '}';
    }

    /**
     * A single change recorded in the {@link Difference} tracker.
     *
     * @param <T> the value type
     */
    public static final class Change<T> {
        private final ChangeType type;
        private final T value;

        public Change(ChangeType type, T value) {
            this.type = type;
            this.value = value;
        }

        public ChangeType type() {
            return this.type;
        }

        public T value() {
            return this.value;
        }

        public Change<T> inverse() {
            return new Change<>(this.type.inverse(), this.value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Change<?> change = (Change<?>) o;
            return this.type == change.type && this.value.equals(change.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.type, this.value);
        }

        @Override
        public String toString() {
            return "(" + this.type + ": " + this.value + ')';
        }
    }

    /**
     * The type of change.
     */
    public enum ChangeType {
        ADD, REMOVE;

        public ChangeType inverse() {
            return this == ADD ? REMOVE : ADD;
        }
    }

}
