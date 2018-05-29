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
import me.lucko.luckperms.api.nodetype.types.RegexType;
import me.lucko.luckperms.common.node.model.NodeTypes;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

public class RegexProcessor extends AbstractPermissionProcessor implements PermissionProcessor {
    private Map<Pattern, Boolean> regexPermissions = Collections.emptyMap();

    @Override
    public Tristate hasPermission(String permission) {
        for (Map.Entry<Pattern, Boolean> e : this.regexPermissions.entrySet()) {
            if (e.getKey().matcher(permission).matches()) {
                return Tristate.fromBoolean(e.getValue());
            }
        }

        return Tristate.UNDEFINED;
    }

    @Override
    public void refresh() {
        ImmutableMap.Builder<Pattern, Boolean> builder = ImmutableMap.builder();
        for (Map.Entry<String, Boolean> e : this.sourceMap.entrySet()) {
            RegexType regexType = NodeTypes.parseRegexType(e.getKey());
            if (regexType == null) {
                continue;
            }

            Pattern pattern = regexType.getPattern().orElse(null);
            if (pattern == null) {
                continue;
            }

            builder.put(pattern, e.getValue());
        }
        this.regexPermissions = builder.build();
    }
}
