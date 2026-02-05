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
import me.lucko.luckperms.common.cacheddata.result.TristateResult;
import me.lucko.luckperms.common.node.types.RegexPermission;
import net.luckperms.api.node.Node;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class RegexProcessor extends AbstractPermissionProcessor implements PermissionProcessor {
    private static final TristateResult.Factory RESULT_FACTORY = new TristateResult.Factory(RegexProcessor.class);

    private final List<RegexEntry> regexPermissions;

    public RegexProcessor(Map<String, Node> sourceMap) {
        this.regexPermissions = process(sourceMap);
    }

    @Override
    public TristateResult hasPermission(String permission) {
        for (RegexEntry e : this.regexPermissions) {
            if (e.pattern().matcher(permission).matches()) {
                return e.result();
            }
        }
        return TristateResult.UNDEFINED;
    }

    private static List<RegexEntry> process(Map<String, Node> sourceMap) {
        ImmutableList.Builder<RegexEntry> builder = ImmutableList.builder();
        for (Map.Entry<String, Node> e : sourceMap.entrySet()) {
            RegexPermission.Builder regexPerm = RegexPermission.parse(e.getKey());
            if (regexPerm == null) {
                continue;
            }

            Pattern pattern = regexPerm.build().getPattern().orElse(null);
            if (pattern == null) {
                continue;
            }

            TristateResult value = RESULT_FACTORY.result(e.getValue());
            builder.add(new RegexEntry(pattern, value));
        }
        return builder.build();
    }

    private static final class RegexEntry {
        private final Pattern pattern;
        private final TristateResult result;

        RegexEntry(Pattern pattern, TristateResult result) {
            this.pattern = pattern;
            this.result = result;
        }

        public Pattern pattern() {
            return this.pattern;
        }

        public TristateResult result() {
            return this.result;
        }
    }
}
