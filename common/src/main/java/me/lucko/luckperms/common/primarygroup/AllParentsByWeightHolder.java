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

package me.lucko.luckperms.common.primarygroup;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.inheritance.InheritanceGraph;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;

import java.util.List;

public class AllParentsByWeightHolder extends CachedPrimaryGroupHolder {
    public AllParentsByWeightHolder(User user) {
        super(user);
    }

    @Override
    protected String calculateValue() {
        Contexts contexts = this.user.getPlugin().getContextForUser(this.user).orElse(null);
        if (contexts == null) {
            contexts = this.user.getPlugin().getContextManager().getStaticContexts();
        }

        InheritanceGraph graph = this.user.getPlugin().getInheritanceHandler().getGraph(contexts);

        // fully traverse the graph, obtain a list of permission holders the user inherits from
        List<PermissionHolder> traversal = ImmutableList.copyOf(graph.traverse(this.user.getPlugin().getConfiguration().get(ConfigKeys.INHERITANCE_TRAVERSAL_ALGORITHM), this.user));

        Group bestGroup = null;

        if (!traversal.isEmpty()) {
            int best = 0;
            for (PermissionHolder holder : traversal) {
                if (!(holder instanceof Group)) {
                    continue;
                }
                Group g = ((Group) holder);

                int weight = g.getWeight().orElse(0);
                if (bestGroup == null || g.getWeight().orElse(0) > best) {
                    bestGroup = g;
                    best = weight;
                }
            }
        }

        return bestGroup == null ? null : bestGroup.getName();
    }
}
