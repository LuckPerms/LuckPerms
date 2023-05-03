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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import me.lucko.luckperms.common.cacheddata.result.TristateResult;
import me.lucko.luckperms.common.node.types.RegexPermission;
import net.luckperms.api.node.Node;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class RegexProcessor extends AbstractSourceBasedProcessor implements PermissionProcessor {
    private static final TristateResult.Factory RESULT_FACTORY = new TristateResult.Factory(RegexProcessor.class);

    private List<Map.Entry<Pattern, TristateResult>> regexPermissions = Collections.emptyList();

    @Override
    public TristateResult hasPermission(String permission) {
        for (Map.Entry<Pattern, TristateResult> e : this.regexPermissions) {
            if (e.getKey().matcher(permission).matches()) {
                return e.getValue();
            }
        }
        return TristateResult.UNDEFINED;
    }

    @Override
    public void refresh() {
        ImmutableList.Builder<Map.Entry<Pattern, TristateResult>> builder = ImmutableList.builder();
        for (Map.Entry<String, Node> e : this.sourceMap.entrySet()) {
            RegexPermission.Builder regexPerm = RegexPermission.parse(e.getKey());
            if (regexPerm == null) {
                continue;
            }

            Pattern pattern = regexPerm.build().getPattern().orElse(null);
            if (pattern == null) {
                continue;
            }

            TristateResult value = RESULT_FACTORY.result(e.getValue());
            builder.add(Maps.immutableEntry(pattern, value));
        }
        this.regexPermissions = builder.build();
    }
}
