/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.sponge.calculators;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.calculators.PermissionProcessor;

import java.util.Map;
import java.util.regex.Pattern;

public class SpongeWildcardProcessor implements PermissionProcessor {
    private static final Pattern SPLIT = Pattern.compile("\\.");
    private Map<String, Boolean> map = null;

    @Override
    public Tristate hasPermission(String s) {
        if (s.startsWith(".") || !s.contains(".")) {
            throw new IllegalArgumentException();
        }

        String[] parts = SPLIT.split(s);
        StringBuilder sb = new StringBuilder();

        for (int i = parts.length - 2; i >= 0; i--) {
            for (int i1 = 0; i1 <= i; i1++) {
                sb.append(parts[i1]).append(".");
            }

            Boolean b = map.get(sb.deleteCharAt(sb.length() - 1).toString());
            if (b != null) {
                return Tristate.fromBoolean(b);
            }

            sb.setLength(0);
        }

        return Tristate.UNDEFINED;
    }

    @Override
    public void updateBacking(Map<String, Boolean> map) {
        if (this.map == null) {
            this.map = map;
        }
    }
}
