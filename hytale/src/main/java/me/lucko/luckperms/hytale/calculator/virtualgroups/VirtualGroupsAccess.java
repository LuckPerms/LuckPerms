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
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import me.lucko.luckperms.common.model.InheritanceOrigin;
import me.lucko.luckperms.common.model.PermissionHolderIdentifier;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.util.ImmutableCollectors;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class VirtualGroupsAccess {

    /** Reflective access to PermissionsModule.virtualGroups field */
    private static final Field VIRTUAL_GROUPS_FIELD;
    static {
        try {
            VIRTUAL_GROUPS_FIELD = PermissionsModule.class.getDeclaredField("virtualGroups");
            VIRTUAL_GROUPS_FIELD.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Exports the virtual groups mapping from Hytale's PermissionsModule into a map of maps of {@link Node}.
     *
     * @return the exported mapping
     */
    public static ImmutableMap<String, ImmutableMap<String, Node>> export() {
        Map<String, Set<String>> virtualGroups = getVirtualGroups();
        return virtualGroups.entrySet().stream().collect(ImmutableCollectors.toMap(
                e -> e.getKey().toLowerCase(Locale.ROOT),
                e -> transformSet(e.getValue(), e.getKey())
        ));
    }

    /**
     * Reads the virtual groups map from the PermissionsModule.
     *
     * @return the virtual groups map
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, Set<String>> getVirtualGroups() {
        PermissionsModule permissionsModule = PermissionsModule.get();
        try {
            return (Map) VIRTUAL_GROUPS_FIELD.get(permissionsModule);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Transforms a set of Hytale permission strings into a map of {@link Node} ready for lookup.
     *
     * @param permissions the input
     * @return the transformed map
     */
    private static ImmutableMap<String, Node> transformSet(Set<String> permissions, String originGroup) {
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
