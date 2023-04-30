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

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.inheritance.InheritanceGraph;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;

import java.util.LinkedHashSet;
import java.util.Locale;
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
     * @param queryOptions the query options to lookup with
     * @return the name of the primary group, or null.
     */
    String calculateValue(QueryOptions queryOptions);

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
        public String calculateValue(QueryOptions queryOptions) {
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
                this.value = value.toLowerCase(Locale.ROOT);
            }
        }
    }


    class AllParentsByWeight extends Stored {
        public AllParentsByWeight(User user) {
            super(user);
        }

        @Override
        public String calculateValue(QueryOptions queryOptions) {
            InheritanceGraph graph = this.user.getPlugin().getInheritanceGraphFactory().getGraph(queryOptions);

            // fully traverse the graph, obtain a list of permission holders the user inherits from in weight order.
            Iterable<PermissionHolder> traversal = graph.traverse(this.user.getPlugin().getConfiguration().get(ConfigKeys.INHERITANCE_TRAVERSAL_ALGORITHM), true, this.user);

            // return the name of the first found group
            for (PermissionHolder holder : traversal) {
                if (holder instanceof Group) {
                    return ((Group) holder).getName();
                }
            }

            // fallback to stored
            return super.calculateValue(queryOptions);
        }
    }

    class ParentsByWeight extends Stored {
        public ParentsByWeight(User user) {
            super(user);
        }

        @Override
        public String calculateValue(QueryOptions queryOptions) {
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

            return bestGroup == null ? super.calculateValue(queryOptions) : bestGroup.getName();
        }
    }
}
