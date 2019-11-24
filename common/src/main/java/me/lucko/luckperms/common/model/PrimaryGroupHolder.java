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

package me.lucko.luckperms.common.model;

import me.lucko.luckperms.common.cache.LoadingMap;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.inheritance.InheritanceGraph;
import me.lucko.luckperms.common.model.manager.group.GroupManager;

import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Calculates and caches a User's "primary group"
 */
public interface PrimaryGroupHolder {

    /**
     * Gets the name of the primary group, or null.
     *
     * @return the name of the primary group, or null.
     */
    String getValue();

    /**
     * Gets the primary group which is stored against the user's data.
     *
     * @return the stored value
     */
    Optional<String> getStoredValue();

    /**
     * Sets the primary group which is stored against the user's data.
     *
     * @param value the new stored value
     */
    void setStoredValue(String value);


    /**
     * Simple implementation which just holds a stored value.
     */
    class Stored implements PrimaryGroupHolder {
        protected final User user;
        private String value = null;

        public Stored(User user) {
            this.user = Objects.requireNonNull(user, "user");
        }

        @Override
        public String getValue() {
            return this.value;
        }

        @Override
        public Optional<String> getStoredValue() {
            return Optional.ofNullable(this.value);
        }

        @Override
        public void setStoredValue(String value) {
            if (value == null || value.isEmpty()) {
                this.value = null;
            } else {
                this.value = value.toLowerCase();
            }
        }
    }

    /**
     * Abstract implementation of {@link PrimaryGroupHolder} which caches all lookups by context.
     */
    abstract class AbstractContextual extends Stored {
        private final Map<QueryOptions, Optional<String>> cache = LoadingMap.of(this::calculateValue);

        AbstractContextual(User user) {
            super(user);
        }

        protected abstract @NonNull Optional<String> calculateValue(QueryOptions queryOptions);

        public void invalidateCache() {
            this.cache.clear();
        }

        @Override
        public final String getValue() {
            QueryOptions queryOptions = this.user.getPlugin().getQueryOptionsForUser(this.user).orElse(null);
            if (queryOptions == null) {
                queryOptions = this.user.getPlugin().getContextManager().getStaticQueryOptions();
            }

            return Objects.requireNonNull(this.cache.get(queryOptions))
                    .orElseGet(() -> getStoredValue().orElse(GroupManager.DEFAULT_GROUP_NAME));
        }

    }

    class AllParentsByWeight extends AbstractContextual {
        public AllParentsByWeight(User user) {
            super(user);
        }

        @Override
        protected @NonNull Optional<String> calculateValue(QueryOptions queryOptions) {
            InheritanceGraph graph = this.user.getPlugin().getInheritanceHandler().getGraph(queryOptions);

            // fully traverse the graph, obtain a list of permission holders the user inherits from in weight order.
            Iterable<PermissionHolder> traversal = graph.traverse(this.user.getPlugin().getConfiguration().get(ConfigKeys.INHERITANCE_TRAVERSAL_ALGORITHM), true, this.user);

            // return the name of the first found group
            for (PermissionHolder holder : traversal) {
                if (holder instanceof Group) {
                    return Optional.of(((Group) holder).getName());
                }
            }
            return Optional.empty();
        }
    }

    class ParentsByWeight extends AbstractContextual {
        public ParentsByWeight(User user) {
            super(user);
        }

        @Override
        protected @NonNull Optional<String> calculateValue(QueryOptions queryOptions) {
            Set<Group> groups = new LinkedHashSet<>();
            for (InheritanceNode node : this.user.getOwnInheritanceNodes(queryOptions)) {
                Group group = this.user.getPlugin().getGroupManager().getIfLoaded(node.getGroupName());
                if (group != null) {
                    groups.add(group);
                }
            }

            Group bestGroup = null;
            int best = 0;

            for (Group g : groups) {
                int weight = g.getWeight().orElse(0);
                if (bestGroup == null || weight > best) {
                    bestGroup = g;
                    best = weight;
                }
            }

            return bestGroup == null ? Optional.empty() : Optional.of(bestGroup.getName());
        }
    }
}
