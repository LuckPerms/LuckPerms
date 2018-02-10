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

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.buffers.Cache;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.node.NodeFactory;

import java.util.Map;
import java.util.OptionalInt;

/**
 * Cache instance to supply the weight of a {@link PermissionHolder}.
 */
public class WeightCache extends Cache<OptionalInt> {
    private static final Cache<OptionalInt> NULL = new Cache<OptionalInt>() {
        @Override
        protected OptionalInt supply() {
            return OptionalInt.empty();
        }
    };

    public static Cache<OptionalInt> getFor(PermissionHolder holder) {
        if (holder.getType().isUser()) {
            return NULL;
        }

        return new WeightCache(((Group) holder));
    }

    private final Group group;

    private WeightCache(Group group) {
        this.group = group;
    }

    @Override
    protected OptionalInt supply() {
        boolean seen = false;
        int best = 0;
        for (Node n : this.group.getOwnNodes(ImmutableContextSet.empty())) {
            Integer weight = NodeFactory.parseWeightNode(n.getPermission());
            if (weight == null) {
                continue;
            }

            if (!seen || weight > best) {
                seen = true;
                best = weight;
            }
        }
        OptionalInt weight = seen ? OptionalInt.of(best) : OptionalInt.empty();

        if (!weight.isPresent()) {
            Map<String, Integer> configWeights = this.group.getPlugin().getConfiguration().get(ConfigKeys.GROUP_WEIGHTS);
            Integer w = configWeights.get(this.group.getObjectName().toLowerCase());
            if (w != null) {
                weight = OptionalInt.of(w);
            }
        }

        return weight;
    }
}
