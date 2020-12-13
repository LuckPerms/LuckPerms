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

package me.lucko.luckperms.common.model.nodemap;

import net.luckperms.api.node.Node;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Records a log of the changes that occur as a result of a {@link NodeMap} mutation(s).
 */
public class MutateResult {
    private final LinkedHashSet<Change> changes = new LinkedHashSet<>();

    public Set<Change> getChanges() {
        return this.changes;
    }

    public Set<Node> getChanges(ChangeType type) {
        Set<Node> changes = new LinkedHashSet<>(this.changes.size());
        for (Change change : this.changes) {
            if (change.getType() == type) {
                changes.add(change.getNode());
            }
        }
        return changes;
    }

    void clear() {
        this.changes.clear();
    }

    public boolean isEmpty() {
        return this.changes.isEmpty();
    }

    public Set<Node> getAdded() {
        return getChanges(ChangeType.ADD);
    }

    public Set<Node> getRemoved() {
        return getChanges(ChangeType.REMOVE);
    }

    private void recordChange(Change change) {
        // This method is the magic of this class.
        // When tracking, we want to ignore changes that cancel each other out, and only
        // keep track of the net difference.
        // e.g. adding then removing the same node = zero net change, so ignore it.

        if (this.changes.remove(change.inverse())) {
            return;
        }
        this.changes.add(change);
    }

    public void recordChange(ChangeType type, Node node) {
        recordChange(new Change(type, node));
    }

    public void recordChanges(ChangeType type, Iterable<Node> nodes) {
        for (Node node : nodes) {
            recordChange(new Change(type, node));
        }
    }

    public MutateResult mergeFrom(MutateResult other) {
        for (Change change : other.changes) {
            recordChange(change);
        }
        return this;
    }

    @Override
    public String toString() {
        return "MutateResult{changes=" + this.changes + '}';
    }

    public static final class Change {
        private final ChangeType type;
        private final Node node;

        public Change(ChangeType type, Node node) {
            this.type = type;
            this.node = node;
        }

        public ChangeType getType() {
            return this.type;
        }

        public Node getNode() {
            return this.node;
        }

        public Change inverse() {
            return new Change(this.type.inverse(), this.node);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Change change = (Change) o;
            return this.type == change.type && this.node.equals(change.node);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.type, this.node);
        }

        @Override
        public String toString() {
            return "Change{type=" + this.type + ", node=" + this.node + '}';
        }
    }

    public enum ChangeType {
        ADD, REMOVE;

        public ChangeType inverse() {
            return this == ADD ? REMOVE : ADD;
        }
    }

}
