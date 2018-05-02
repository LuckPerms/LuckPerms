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

import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.node.model.ImmutableNode;

import java.util.Collections;
import java.util.Map;

public class WildcardProcessor extends AbstractPermissionProcessor implements PermissionProcessor {
    public static final String WILDCARD_SUFFIX = ".*";
    private static final String GLOBAL_WILDCARD = "*";
    private static final String GLOBAL_WILDCARD_WITH_QUOTES = "'*'";

    private Map<String, Boolean> wildcardPermissions = Collections.emptyMap();
    private Tristate globalWildcardState = Tristate.UNDEFINED;

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
                Tristate t = Tristate.fromNullableBoolean(this.wildcardPermissions.get(node));
                if (t != Tristate.UNDEFINED) {
                    return t;
                }
            }
        }

        return this.globalWildcardState;
    }

    @Override
    public void refresh() {
        ImmutableMap.Builder<String, Boolean> builder = ImmutableMap.builder();
        for (Map.Entry<String, Boolean> e : this.sourceMap.entrySet()) {
            String key = e.getKey();
            if (!key.endsWith(WILDCARD_SUFFIX) || key.length() <= 2) {
                continue;
            }

            builder.put(key.substring(0, key.length() - 2), e.getValue());
        }
        this.wildcardPermissions = builder.build();

        Tristate state = Tristate.fromNullableBoolean(this.sourceMap.get(GLOBAL_WILDCARD));
        if (state == Tristate.UNDEFINED) {
            state = Tristate.fromNullableBoolean(this.sourceMap.get(GLOBAL_WILDCARD_WITH_QUOTES));
        }

        this.globalWildcardState = state;
    }
}
