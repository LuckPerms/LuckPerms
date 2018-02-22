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

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.graph.Graph;
import me.lucko.luckperms.common.graph.GraphTraversers;
import me.lucko.luckperms.common.graph.TraversalAlgorithm;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * A {@link Graph} which represents an "inheritance tree".
 */
public interface InheritanceGraph extends Graph<PermissionHolder> {

    /**
     * Returns an iterable which will traverse this inheritance graph using the
     * specified algorithm starting at the given node.
     *
     * @param algorithm the algorithm to use when traversing
     * @param startNode the start node in the inheritance graph
     * @return an iterable
     */
    default Iterable<PermissionHolder> traverse(TraversalAlgorithm algorithm, PermissionHolder startNode) {
        return GraphTraversers.traverseUsing(algorithm, this, startNode);
    }

    final class NonContextual implements InheritanceGraph {
        private final LuckPermsPlugin plugin;

        NonContextual(LuckPermsPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public Iterable<? extends PermissionHolder> successors(PermissionHolder holder) {
            Set<Group> successors = new TreeSet<>(holder.getInheritanceComparator());
            List<Node> nodes = holder.getOwnGroupNodes();
            for (Node n : nodes) {
                Group g = this.plugin.getGroupManager().getIfLoaded(n.getGroupName());
                if (g != null) {
                    successors.add(g);
                }
            }
            return successors;
        }
    }

    final class Contextual implements InheritanceGraph {
        private final LuckPermsPlugin plugin;

        /**
         * The contexts to resolve inheritance in.
         */
        private final Contexts context;

        Contextual(LuckPermsPlugin plugin, Contexts context) {
            this.plugin = plugin;
            this.context = context;
        }

        @Override
        public Iterable<? extends PermissionHolder> successors(PermissionHolder holder) {
            Set<Group> successors = new TreeSet<>(holder.getInheritanceComparator());
            List<Node> nodes = holder.getOwnGroupNodes(this.context.getContexts());
            for (Node n : nodes) {
                // effectively: if not (we're applying global groups or it's specific anyways)
                if (!((this.context.isApplyGlobalGroups() || n.isServerSpecific()) && (this.context.isApplyGlobalWorldGroups() || n.isWorldSpecific()))) {
                    continue;
                }

                Group g = this.plugin.getGroupManager().getIfLoaded(n.getGroupName());
                if (g != null) {
                    successors.add(g);
                }
            }
            return successors;
        }
    }

}
