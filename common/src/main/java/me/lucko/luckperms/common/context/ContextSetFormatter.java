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

package me.lucko.luckperms.common.context;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.context.ContextSet;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class ContextSetFormatter {
    private ContextSetFormatter() {}

    public static Optional<String> toMinimalString(ContextSet contextSet) {
        Set<Map.Entry<String, String>> entries = contextSet.toSet();
        if (entries.isEmpty()) {
            return Optional.empty();
        }

        // effectively: if entries contains any non-server keys
        if (entries.stream().anyMatch(pair -> !pair.getKey().equals(Contexts.SERVER_KEY))) {
            // return all entries in 'key=value' form
            return Optional.of(entries.stream().map(pair -> pair.getKey() + "=" + pair.getValue()).collect(Collectors.joining(";")));
        } else {
            // just return the server ids, without the 'server='
            return Optional.of(entries.stream().map(Map.Entry::getValue).collect(Collectors.joining(";")));
        }
    }

}
