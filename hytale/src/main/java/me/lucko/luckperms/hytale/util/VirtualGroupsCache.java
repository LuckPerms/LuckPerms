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

package me.lucko.luckperms.hytale.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import me.lucko.luckperms.common.cache.LoadingMap;
import me.lucko.luckperms.common.calculator.PermissionCalculator;
import me.lucko.luckperms.common.calculator.PermissionCalculatorBase;
import me.lucko.luckperms.common.calculator.processor.DirectProcessor;
import me.lucko.luckperms.common.calculator.processor.PermissionProcessor;
import me.lucko.luckperms.common.calculator.processor.WildcardProcessor;
import me.lucko.luckperms.common.model.InheritanceOrigin;
import me.lucko.luckperms.common.model.PermissionHolderIdentifier;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.util.ImmutableCollectors;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VirtualGroupsCache {

    /**
     * A loading map (cache) of virtual group names to a {@link PermissionCalculator}
     * that can resolve the collective permissions of the groups.
     */
    private final Map<ImmutableSet<String>, PermissionCalculator> caches;

    /**
     * A set of known virtual group names
     */
    private ImmutableSet<String> knownVirtualGroups;

    /**
     * A lookup of virtual group name (lowercase) to the permissions it grants, as nodes.
     */
    private ImmutableMap<String, ImmutableMap<String, Node>> virtualGroupToNodesLookup;

    public VirtualGroupsCache() {
        this.caches = LoadingMap.of(this::buildCalculator);
        refresh();
    }

    public void refresh() {
        Map<String, Set<String>> virtualGroups = PermissionsModule.get().getVirtualGroups();
        this.knownVirtualGroups = ImmutableSet.copyOf(virtualGroups.keySet());
        this.virtualGroupToNodesLookup = virtualGroups.entrySet().stream().collect(ImmutableCollectors.toMap(
                e -> e.getKey().toLowerCase(Locale.ROOT),
                e -> hytalePermissionStringsToNodes(e.getValue(), e.getKey())
        ));
        this.caches.clear();
    }

    public ImmutableSet<String> getAllVirtualGroups() {
        return this.knownVirtualGroups;
    }

    public PermissionCalculator getCalculator(ImmutableSet<String> virtualGroups) {
        return this.caches.get(virtualGroups);
    }

    private PermissionCalculator buildCalculator(Set<String> virtualGroups) {
        Map<String, Node> sourceMap = new ConcurrentHashMap<>();
        for (String virtualGroup : virtualGroups) {
            sourceMap.putAll(this.virtualGroupToNodesLookup.getOrDefault(virtualGroup.toLowerCase(Locale.ROOT), ImmutableMap.of()));
        }

        if (sourceMap.isEmpty()) {
            return PermissionCalculator.EMPTY;
        }

        List<PermissionProcessor> processors = new ArrayList<>(2);
        processors.add(new DirectProcessor(sourceMap));
        processors.add(new WildcardProcessor(sourceMap));

        return new PermissionCalculatorBase(processors);
    }

    /**
     * Transforms a set of Hytale permission strings into a map of {@link Node} ready for lookup.
     *
     * @param permissions the input
     * @return the transformed map
     */
    private static ImmutableMap<String, Node> hytalePermissionStringsToNodes(Set<String> permissions, String originGroup) {
        ImmutableMap.Builder<String, Node> builder = ImmutableMap.builder();

        InheritanceOrigin origin = new InheritanceOrigin(
                new PermissionHolderIdentifier("virtual_group", originGroup),
                DataType.TRANSIENT
        );

        for (String permission : permissions) {
            boolean value = true;
            if (!permission.isEmpty() && permission.charAt(0) == '-') {
                value = false;
                permission = permission.substring(1);
            }

            Node node = NodeBuilders.determineMostApplicable(permission)
                    .value(value)
                    .withMetadata(InheritanceOrigin.KEY, origin)
                    .build();

            builder.put(permission.toLowerCase(Locale.ROOT), node);
        }

        return builder.build();
    }

}
