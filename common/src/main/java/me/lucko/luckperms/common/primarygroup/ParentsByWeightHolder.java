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

import lombok.NonNull;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.User;

import java.util.ArrayList;
import java.util.List;

public class ParentsByWeightHolder extends StoredHolder {

    private String cachedValue = null;
    private boolean useCached = false;

    public ParentsByWeightHolder(@NonNull User user) {
        super(user);
        user.getStateListeners().add(() -> useCached = false);
    }

    @Override
    public String getValue() {
        if (useCached) {
            return cachedValue;
        }

        Contexts contexts = user.getPlugin().getContextForUser(user);
        ContextSet contextSet = contexts != null ? contexts.getContexts() : user.getPlugin().getContextManager().getStaticContexts();

        List<Group> groups = new ArrayList<>();
        for (Node node : user.filterNodes(contextSet)) {
            if (!node.getValue() || !node.isGroupNode()) {
                continue;
            }

            Group group = user.getPlugin().getGroupManager().getIfLoaded(node.getGroupName());
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

        cachedValue = bestGroup == null ? null : bestGroup.getName();
        useCached = true;
        return cachedValue;
    }
}
