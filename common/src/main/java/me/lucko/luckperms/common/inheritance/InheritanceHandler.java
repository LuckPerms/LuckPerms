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
import me.lucko.luckperms.api.LookupSetting;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Provides {@link InheritanceGraph}s.
 */
public class InheritanceHandler {
    private final LuckPermsPlugin plugin;

    /**
     * An inheritance graph which doesn't consider contexts
     */
    private final InheritanceGraph nonContextualGraph;

    // some cached contextual graphs for common Contexts
    private final InheritanceGraph allowAllContextualGraph;
    private final InheritanceGraph globalContextualGraph;

    public InheritanceHandler(LuckPermsPlugin plugin) {
        this.plugin = plugin;
        this.nonContextualGraph = new NonContextualGraph(plugin);
        this.allowAllContextualGraph = new ContextualGraph(plugin, Contexts.allowAll());
        this.globalContextualGraph = new ContextualGraph(plugin, Contexts.global());
    }

    public InheritanceGraph getGraph() {
        return this.nonContextualGraph;
    }

    public InheritanceGraph getGraph(Contexts contexts) {
        if (contexts == Contexts.allowAll()) {
            return this.allowAllContextualGraph;
        }
        if (contexts == Contexts.global()) {
            return this.globalContextualGraph;
        }

        return new ContextualGraph(this.plugin, contexts);
    }

    private static final class NonContextualGraph implements InheritanceGraph {
        private final LuckPermsPlugin plugin;

        NonContextualGraph(LuckPermsPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public Iterable<? extends PermissionHolder> successors(PermissionHolder holder) {
            Set<Group> successors = new TreeSet<>(holder.getInheritanceComparator());
            List<? extends Node> nodes = holder.getOwnGroupNodes();
            for (Node n : nodes) {
                Group g = this.plugin.getGroupManager().getIfLoaded(n.getGroupName());
                if (g != null) {
                    successors.add(g);
                }
            }
            return successors;
        }
    }

    private static final class ContextualGraph implements InheritanceGraph {
        private final LuckPermsPlugin plugin;

        /**
         * The contexts to resolve inheritance in.
         */
        private final Contexts context;

        ContextualGraph(LuckPermsPlugin plugin, Contexts context) {
            this.plugin = plugin;
            this.context = context;
        }

        @Override
        public Iterable<? extends PermissionHolder> successors(PermissionHolder holder) {
            Set<Group> successors = new TreeSet<>(holder.getInheritanceComparator());
            List<? extends Node> nodes = holder.getOwnGroupNodes(this.context.getContexts());
            for (Node n : nodes) {
                // effectively: if not (we're applying global groups or it's specific anyways)
                if (!((this.context.hasSetting(LookupSetting.APPLY_PARENTS_SET_WITHOUT_SERVER) || n.isServerSpecific()) && (this.context.hasSetting(LookupSetting.APPLY_PARENTS_SET_WITHOUT_WORLD) || n.isWorldSpecific()))) {
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
