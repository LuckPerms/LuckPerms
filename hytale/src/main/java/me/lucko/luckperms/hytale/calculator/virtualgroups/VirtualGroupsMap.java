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

package me.lucko.luckperms.hytale.calculator.virtualgroups;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import me.lucko.luckperms.common.cache.LoadingMap;
import me.lucko.luckperms.common.calculator.PermissionCalculator;
import me.lucko.luckperms.common.calculator.PermissionCalculatorBase;
import me.lucko.luckperms.common.calculator.processor.DirectProcessor;
import me.lucko.luckperms.common.calculator.processor.PermissionProcessor;
import me.lucko.luckperms.common.calculator.processor.WildcardProcessor;
import net.luckperms.api.node.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VirtualGroupsMap {
    private final Map<ImmutableSet<String>, PermissionCalculator> caches;
    private ImmutableMap<String, ImmutableMap<String, Node>> virtualGroupToNodesMap;

    public VirtualGroupsMap() {
        this.caches = LoadingMap.of(this::buildCalculator);
        refresh();
    }

    public void refresh() {
        this.virtualGroupToNodesMap = VirtualGroupsAccess.export();
        this.caches.clear();
    }

    public ImmutableSet<String> getAllVirtualGroups() {
        return this.virtualGroupToNodesMap.keySet();
    }

    public PermissionCalculator getCalculator(ImmutableSet<String> virtualGroups) {
        return this.caches.get(virtualGroups);
    }

    private PermissionCalculator buildCalculator(Set<String> virtualGroups) {
        Map<String, Node> sourceMap = new ConcurrentHashMap<>();
        for (String virtualGroup : virtualGroups) {
            sourceMap.putAll(this.virtualGroupToNodesMap.getOrDefault(virtualGroup, ImmutableMap.of()));
        }

        if (sourceMap.isEmpty()) {
            return PermissionCalculator.EMPTY;
        }

        List<PermissionProcessor> processors = new ArrayList<>(2);
        processors.add(new DirectProcessor(sourceMap));
        processors.add(new WildcardProcessor(sourceMap));

        return new PermissionCalculatorBase(processors);
    }

}
