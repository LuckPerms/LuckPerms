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

package me.lucko.luckperms.common.calculator.processor;

import com.google.common.collect.ImmutableMap;
import me.lucko.luckperms.common.cacheddata.result.TristateResult;
import me.lucko.luckperms.common.node.AbstractNode;
import net.luckperms.api.node.Node;
import net.luckperms.api.util.Tristate;

import java.util.Collections;
import java.util.Map;

public class WildcardProcessor extends AbstractSourceBasedProcessor implements PermissionProcessor {
    private static final TristateResult.Factory RESULT_FACTORY = new TristateResult.Factory(WildcardProcessor.class);

    public static final String WILDCARD_SUFFIX = ".*";
    private static final String ROOT_WILDCARD = "*";
    private static final String ROOT_WILDCARD_WITH_QUOTES = "'*'";

    public static boolean isRootWildcard(String permission) {
        return ROOT_WILDCARD.equals(permission) || ROOT_WILDCARD_WITH_QUOTES.equals(permission);
    }

    public static boolean isWildcardPermission(String permission) {
        return isRootWildcard(permission) || permission.endsWith(WILDCARD_SUFFIX) && permission.length() > 2;
    }

    private Map<String, TristateResult> wildcardPermissions = Collections.emptyMap();
    private TristateResult rootWildcardState = TristateResult.UNDEFINED;

    @Override
    public TristateResult hasPermission(String permission) {
        String node = permission;

        while (true) {
            int endIndex = node.lastIndexOf(AbstractNode.NODE_SEPARATOR);
            if (endIndex == -1) {
                break;
            }

            node = node.substring(0, endIndex);
            if (!node.isEmpty()) {
                TristateResult match = this.wildcardPermissions.get(node);
                if (match != null && match.result() != Tristate.UNDEFINED) {
                    return match;
                }
            }
        }

        return this.rootWildcardState;
    }

    @Override
    public void refresh() {
        ImmutableMap.Builder<String, TristateResult> builder = ImmutableMap.builder();
        for (Map.Entry<String, Node> e : this.sourceMap.entrySet()) {
            String key = e.getKey();
            if (!key.endsWith(WILDCARD_SUFFIX) || key.length() <= 2) {
                continue;
            }
            key = key.substring(0, key.length() - 2);

            TristateResult value = RESULT_FACTORY.result(e.getValue());
            builder.put(key, value);
        }
        this.wildcardPermissions = builder.build();

        Node rootWildcard = this.sourceMap.get(ROOT_WILDCARD);
        if (rootWildcard == null) {
            rootWildcard = this.sourceMap.get(ROOT_WILDCARD_WITH_QUOTES);
        }
        this.rootWildcardState = rootWildcard == null ? TristateResult.UNDEFINED : RESULT_FACTORY.result(rootWildcard);
    }
}
