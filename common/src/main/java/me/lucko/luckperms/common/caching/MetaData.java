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

package me.lucko.luckperms.common.caching;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class MetaData {
    private final Contexts contexts;

    @Getter
    private String prefix = null;

    @Getter
    private String suffix = null;
    private Map<String, String> meta = new ConcurrentHashMap<>();

    public void loadMeta(SortedSet<LocalizedNode> nodes) {
        invalidateCache();

        Map<String, String> contexts = new HashMap<>(this.contexts.getContext());
        String server = contexts.remove("server");
        String world = contexts.remove("world");

        int prefixPriority = Integer.MIN_VALUE;
        int suffixPriority = Integer.MIN_VALUE;

        for (LocalizedNode ln : nodes) {
            Node n = ln.getNode();

            if (!n.getValue()) {
                continue;
            }

            if (!n.isMeta() && !n.isPrefix() && n.isSuffix()) {
                continue;
            }

            if (!n.shouldApplyOnServer(server, this.contexts.isIncludeGlobal(), false)) {
                continue;
            }

            if (!n.shouldApplyOnWorld(world, this.contexts.isIncludeGlobalWorld(), false)) {
                continue;
            }

            if (!n.shouldApplyWithContext(contexts, false)) {
                continue;
            }

            if (n.isPrefix()) {
                Map.Entry<Integer, String> value = n.getPrefix();
                if (value.getKey() > prefixPriority) {
                    this.prefix = value.getValue();
                    prefixPriority = value.getKey();
                }
                continue;
            }

            if (n.isSuffix()) {
                Map.Entry<Integer, String> value = n.getSuffix();
                if (value.getKey() > suffixPriority) {
                    this.suffix = value.getValue();
                    suffixPriority = value.getKey();
                }
                continue;
            }

            if (n.isMeta()) {
                Map.Entry<String, String> meta = n.getMeta();
                this.meta.put(meta.getKey(), meta.getValue());
            }
        }
    }

    public void invalidateCache() {
        meta.clear();
        prefix = null;
        suffix = null;
    }

    public Map<String, String> getMeta() {
        return ImmutableMap.copyOf(meta);
    }

}
