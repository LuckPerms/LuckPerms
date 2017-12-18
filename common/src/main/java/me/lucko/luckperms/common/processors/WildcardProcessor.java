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

package me.lucko.luckperms.common.processors;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.node.ImmutableNode;

import java.util.Map;

public class WildcardProcessor implements PermissionProcessor {
    public static final String WILDCARD_SUFFIX = ".*";
    private static final String GLOBAL_WILDCARD_1 = "*";
    private static final String GLOBAL_WILDCARD_2 = "'*'";

    private Map<String, Boolean> map = null;

    @Override
    public Tristate hasPermission(String permission) {
        String node = permission;

        while (true) {
            int endIndex = node.lastIndexOf(ImmutableNode.NODE_SEPARATOR);
            if (endIndex == -1) {
                break;
            }

            node = node.substring(0, endIndex);
            if (!node.isEmpty()) {
                Tristate t = Tristate.fromNullableBoolean(map.get(node + WILDCARD_SUFFIX));
                if (t != Tristate.UNDEFINED) {
                    return t;
                }
            }
        }

        Tristate t = Tristate.fromNullableBoolean(map.get(GLOBAL_WILDCARD_1));
        if (t != Tristate.UNDEFINED) {
            return t;
        }

        return Tristate.fromNullableBoolean(map.get(GLOBAL_WILDCARD_2));
    }

    @Override
    public void updateBacking(Map<String, Boolean> map) {
        if (this.map == null) {
            this.map = map;
        }
    }
}
