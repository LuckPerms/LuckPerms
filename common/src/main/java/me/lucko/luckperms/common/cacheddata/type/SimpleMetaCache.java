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

package me.lucko.luckperms.common.cacheddata.type;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multimaps;

import me.lucko.luckperms.common.metastacking.MetaStack;
import me.lucko.luckperms.common.verbose.event.MetaCheckEvent;

import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.metastacking.MetaStackDefinition;
import net.luckperms.api.query.QueryOptions;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

/**
 * Holds cached meta for a given context
 */
public class SimpleMetaCache implements CachedMetaData {

    /** The query options this container is holding data for */
    private final QueryOptions queryOptions;

    /* The data */
    protected Map<String, List<String>> meta = ImmutableMap.of();
    protected Map<String, String> flattenedMeta = ImmutableMap.of();
    protected SortedMap<Integer, String> prefixes = ImmutableSortedMap.of();
    protected SortedMap<Integer, String> suffixes = ImmutableSortedMap.of();
    protected String primaryGroup = null;
    protected MetaStack prefixStack = null;
    protected MetaStack suffixStack = null;

    public SimpleMetaCache(QueryOptions queryOptions) {
        this.queryOptions = queryOptions;
    }

    public void loadMeta(MetaAccumulator meta) {
        this.meta = Multimaps.asMap(ImmutableListMultimap.copyOf(meta.getMeta()));

        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (Map.Entry<String, List<String>> e : this.meta.entrySet()) {
            if (e.getValue().isEmpty()) {
                continue;
            }

            // take the value which was accumulated first
            builder.put(e.getKey(), e.getValue().get(0));
        }
        this.flattenedMeta = builder.build();

        this.prefixes = ImmutableSortedMap.copyOfSorted(meta.getPrefixes());
        this.suffixes = ImmutableSortedMap.copyOfSorted(meta.getSuffixes());
        this.primaryGroup = meta.getPrimaryGroup();
        this.prefixStack = meta.getPrefixStack();
        this.suffixStack = meta.getSuffixStack();
    }

    public String getMetaValue(String key, MetaCheckEvent.Origin origin) {
        Objects.requireNonNull(key, "key");
        return this.flattenedMeta.get(key);
    }

    @Override
    public final String getMetaValue(String key) {
        return getMetaValue(key, MetaCheckEvent.Origin.LUCKPERMS_API);
    }

    public String getPrefix(MetaCheckEvent.Origin origin) {
        MetaStack prefixStack = this.prefixStack;
        return prefixStack == null ? null : prefixStack.toFormattedString();
    }

    @Override
    public final String getPrefix() {
        return getPrefix(MetaCheckEvent.Origin.LUCKPERMS_API);
    }

    public String getSuffix(MetaCheckEvent.Origin origin) {
        MetaStack suffixStack = this.suffixStack;
        return suffixStack == null ? null : suffixStack.toFormattedString();
    }

    @Override
    public final String getSuffix() {
        return getSuffix(MetaCheckEvent.Origin.LUCKPERMS_API);
    }

    public Map<String, List<String>> getMeta(MetaCheckEvent.Origin origin) {
        return this.meta;
    }

    @Override
    public final @NonNull Map<String, List<String>> getMeta() {
        return getMeta(MetaCheckEvent.Origin.LUCKPERMS_API);
    }

    @Override
    public @NonNull SortedMap<Integer, String> getPrefixes() {
        return this.prefixes;
    }

    @Override
    public @NonNull SortedMap<Integer, String> getSuffixes() {
        return this.suffixes;
    }

    public @Nullable String getPrimaryGroup(MetaCheckEvent.Origin origin) {
        return this.primaryGroup;
    }

    @Override
    public final @Nullable String getPrimaryGroup() {
        return this.primaryGroup;
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
    public @NonNull QueryOptions getQueryOptions() {
        return this.queryOptions;
    }

}
