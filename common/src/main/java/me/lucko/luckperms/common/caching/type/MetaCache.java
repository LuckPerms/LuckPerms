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

package me.lucko.luckperms.common.caching.type;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ListMultimap;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.caching.MetaContexts;
import me.lucko.luckperms.api.caching.MetaData;
import me.lucko.luckperms.api.metastacking.MetaStackDefinition;
import me.lucko.luckperms.common.metastacking.MetaStack;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * Holds cached meta for a given context
 */
public class MetaCache implements MetaData {

    /**
     * The contexts this container is holding data for
     */
    private final MetaContexts metaContexts;

    private ListMultimap<String, String> metaMultimap = ImmutableListMultimap.of();
    private Map<String, String> meta = ImmutableMap.of();
    private SortedMap<Integer, String> prefixes = ImmutableSortedMap.of();
    private SortedMap<Integer, String> suffixes = ImmutableSortedMap.of();
    private MetaStack prefixStack = null;
    private MetaStack suffixStack = null;

    public MetaCache(MetaContexts metaContexts) {
        this.metaContexts = metaContexts;
    }

    public void loadMeta(MetaAccumulator meta) {
        meta.complete();

        this.metaMultimap = ImmutableListMultimap.copyOf(meta.getMeta());

        //noinspection unchecked
        Map<String, List<String>> metaMap = (Map) this.metaMultimap.asMap();
        ImmutableMap.Builder<String, String> metaMapBuilder = ImmutableMap.builder();

        for (Map.Entry<String, List<String>> e : metaMap.entrySet()) {
            if (e.getValue().isEmpty()) {
                continue;
            }

            // take the value which was accumulated first
            metaMapBuilder.put(e.getKey(), e.getValue().get(0));
        }
        this.meta = metaMapBuilder.build();

        this.prefixes = ImmutableSortedMap.copyOfSorted(meta.getPrefixes());
        this.suffixes = ImmutableSortedMap.copyOfSorted(meta.getSuffixes());
        this.prefixStack = meta.getPrefixStack();
        this.suffixStack = meta.getSuffixStack();
    }

    @Override
    public String getPrefix() {
        MetaStack prefixStack = this.prefixStack;
        return prefixStack == null ? null : prefixStack.toFormattedString();
    }

    @Override
    public String getSuffix() {
        MetaStack suffixStack = this.suffixStack;
        return suffixStack == null ? null : suffixStack.toFormattedString();
    }

    @Override
    public @NonNull MetaStackDefinition getPrefixStackDefinition() {
        return this.prefixStack.getDefinition();
    }

    @Override
    public @NonNull MetaStackDefinition getSuffixStackDefinition() {
        return this.suffixStack.getDefinition();
    }

    @Override
    public @NonNull Contexts getContexts() {
        return this.metaContexts.getContexts();
    }

    @Override
    public @NonNull MetaContexts getMetaContexts() {
        return this.metaContexts;
    }

    @Override
    public @NonNull ListMultimap<String, String> getMetaMultimap() {
        return this.metaMultimap;
    }

    @Override
    public @NonNull Map<String, String> getMeta() {
        return this.meta;
    }

    @Override
    public @NonNull SortedMap<Integer, String> getPrefixes() {
        return this.prefixes;
    }

    @Override
    public @NonNull SortedMap<Integer, String> getSuffixes() {
        return this.suffixes;
    }

}
