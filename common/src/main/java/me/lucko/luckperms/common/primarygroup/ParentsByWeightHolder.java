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

import me.lucko.luckperms.api.node.types.InheritanceNode;
import me.lucko.luckperms.api.query.QueryOptions;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.User;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class ParentsByWeightHolder extends ContextualHolder {
    public ParentsByWeightHolder(User user) {
        super(user);
    }

    @Override
    protected @NonNull Optional<String> calculateValue(QueryOptions queryOptions) {
        Set<Group> groups = new LinkedHashSet<>();
        for (InheritanceNode node : this.user.getOwnGroupNodes(queryOptions)) {
            Group group = this.user.getPlugin().getGroupManager().getIfLoaded(node.getGroupName());
            if (group != null) {
                groups.add(group);
            }
        }

        Group bestGroup = null;

        if (!groups.isEmpty()) {
            int best = 0;
            for (Group g : groups) {
                int weight = g.getWeight().orElse(0);
                if (bestGroup == null || g.getWeight().orElse(0) > best) {
                    bestGroup = g;
                    best = weight;
                }
            }
        }

        return bestGroup == null ? Optional.empty() : Optional.of(bestGroup.getName());
    }
}
