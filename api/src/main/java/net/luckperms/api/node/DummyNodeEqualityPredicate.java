package net.luckperms.api.node;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Dummy implementation of {@link NodeEqualityPredicate}, used for the given constant
 * implementations.
 *
 * <p>The implementation rule of not calling {@link Node#equals(Node, NodeEqualityPredicate)} is
 * intentionally disregarded by this dummy implementation. The equals method has a special case for
 * the dummy instances, preventing a stack overflow.</p>
 */
final class DummyNodeEqualityPredicate implements NodeEqualityPredicate {
    private final String name;

    DummyNodeEqualityPredicate(String name) {
        this.name = name;
    }

    @Override
    public boolean areEqual(@NonNull Node o1, @NonNull Node o2) {
        return o1.equals(o2, this);
    }

    @Override
    public String toString() {
        return "NodeEqualityPredicate#" + this.name;
    }
}
