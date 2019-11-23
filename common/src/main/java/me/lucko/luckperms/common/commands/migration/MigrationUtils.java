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

package me.lucko.luckperms.common.commands.migration;

import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.node.types.Weight;

import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.NodeBuilder;
import net.luckperms.api.node.NodeType;

public final class MigrationUtils {
    private MigrationUtils() {}

    public static NodeBuilder parseNode(String permission, boolean value) {
        if (permission.startsWith("-") || permission.startsWith("!")) {
            if (permission.length() == 1) {
                return NodeBuilders.determineMostApplicable(permission).value(value);
            }

            permission = permission.substring(1);
            value = false;
        } else if (permission.startsWith("+")) {
            if (permission.length() == 1) {
                return NodeBuilders.determineMostApplicable(permission).value(value);
            }

            permission = permission.substring(1);
            value = true;
        }

        return NodeBuilders.determineMostApplicable(permission).value(value);
    }

    public static void setGroupWeight(Group group, int weight) {
        group.removeIf(DataType.NORMAL, null, NodeType.WEIGHT::matches, false);
        group.setNode(DataType.NORMAL, Weight.builder(weight).build(), true);
    }

    public static String standardizeName(String string) {
        return string.trim().replace(':', '-').replace(' ', '-').replace('.', '-').toLowerCase();
    }

}
