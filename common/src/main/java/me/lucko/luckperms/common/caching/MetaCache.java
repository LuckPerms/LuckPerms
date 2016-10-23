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
import com.google.common.collect.ImmutableSortedMap;
import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.caching.MetaData;
import me.lucko.luckperms.api.context.MutableContextSet;

import java.util.*;

/**
 * Holds a user's cached meta for a given context
 */
@RequiredArgsConstructor
public class MetaCache implements MetaData {
    private final Contexts contexts;

    private final SortedMap<Integer, String> prefixes = new TreeMap<>(Comparator.reverseOrder());
    private final SortedMap<Integer, String> suffixes = new TreeMap<>(Comparator.reverseOrder());

    private final Map<String, String> meta = new HashMap<>();

    public void loadMeta(SortedSet<LocalizedNode> nodes) {
        invalidateCache();

        MutableContextSet contexts = MutableContextSet.fromSet(this.contexts.getContexts());
        String server = contexts.getValues("server").stream().findAny().orElse(null);
        String world = contexts.getValues("world").stream().findAny().orElse(null);
        contexts.removeAll("server");
        contexts.removeAll("world");

        for (LocalizedNode ln : nodes) {
            Node n = ln.getNode();

            if (!n.getValue()) {
                continue;
            }

            if (!n.isMeta() && !n.isPrefix() && !n.isSuffix()) {
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
                synchronized (this.prefixes) {
                    if (!this.prefixes.containsKey(value.getKey())) {
                        this.prefixes.put(value.getKey(), value.getValue());
                    }
                }
                continue;
            }

            if (n.isSuffix()) {
                Map.Entry<Integer, String> value = n.getSuffix();
                synchronized (this.suffixes) {
                    if (!this.suffixes.containsKey(value.getKey())) {
                        this.suffixes.put(value.getKey(), value.getValue());
                    }
                }
                continue;
            }

            if (n.isMeta()) {
                Map.Entry<String, String> meta = n.getMeta();
                synchronized (this.meta) {
                    if (!this.meta.containsKey(meta.getKey())) {
                        this.meta.put(meta.getKey(), meta.getValue());
                    }
                }
                this.meta.put(meta.getKey(), meta.getValue());
            }
        }
    }

    private void invalidateCache() {
        synchronized (meta) {
            meta.clear();
        }
        synchronized (prefixes) {
            prefixes.clear();
        }
        synchronized (suffixes) {
            suffixes.clear();
        }
    }

    @Override
    public Map<String, String> getMeta() {
        synchronized (meta) {
            return ImmutableMap.copyOf(meta);
        }
    }

    @Override
    public SortedMap<Integer, String> getPrefixes() {
        synchronized (prefixes) {
            return ImmutableSortedMap.copyOfSorted(prefixes);
        }
    }

    @Override
    public SortedMap<Integer, String> getSuffixes() {
        synchronized (suffixes) {
            return ImmutableSortedMap.copyOfSorted(suffixes);
        }
    }

    @Override
    public String getPrefix() {
        synchronized (prefixes) {
            if (prefixes.isEmpty()) {
                return null;
            }

            return prefixes.get(prefixes.firstKey());
        }
    }

    @Override
    public String getSuffix() {
        synchronized (suffixes) {
            if (suffixes.isEmpty()) {
                return null;
            }

            return suffixes.get(suffixes.firstKey());
        }
    }

}
