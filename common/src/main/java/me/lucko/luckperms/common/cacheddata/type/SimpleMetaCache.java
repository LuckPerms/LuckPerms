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

import me.lucko.luckperms.common.cacheddata.UsageTracked;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.verbose.event.MetaCheckEvent;

import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.metastacking.MetaStackDefinition;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.query.meta.MetaValueSelector;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

/**
 * Holds cached meta for a given context
 */
public class SimpleMetaCache extends UsageTracked implements CachedMetaData {

    private final LuckPermsPlugin plugin;

    /** The query options this container is holding data for */
    private final QueryOptions queryOptions;

    /* The data */
    protected final Map<String, List<String>> meta;
    protected final Map<String, String> flattenedMeta;
    protected final SortedMap<Integer, String> prefixes;
    protected final SortedMap<Integer, String> suffixes;
    protected final int weight;
    protected final String primaryGroup;
    private final MetaStackDefinition prefixDefinition;
    private final MetaStackDefinition suffixDefinition;
    private final String prefix;
    private final String suffix;

    public SimpleMetaCache(LuckPermsPlugin plugin, QueryOptions queryOptions, MetaAccumulator sourceMeta) {
        this.plugin = plugin;
        this.queryOptions = queryOptions;

        this.meta = Multimaps.asMap(ImmutableListMultimap.copyOf(sourceMeta.getMeta()));

        MetaValueSelector metaValueSelector = this.queryOptions.option(MetaValueSelector.KEY)
                .orElseGet(() -> this.plugin.getConfiguration().get(ConfigKeys.META_VALUE_SELECTOR));

        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (Map.Entry<String, List<String>> e : this.meta.entrySet()) {
            if (e.getValue().isEmpty()) {
                continue;
            }

            String selected = metaValueSelector.selectValue(e.getKey(), e.getValue());
            if (selected == null) {
                throw new NullPointerException(metaValueSelector + " returned null");
            }

            builder.put(e.getKey(), selected);
        }
        this.flattenedMeta = builder.build();

        this.prefixes = ImmutableSortedMap.copyOfSorted(sourceMeta.getPrefixes());
        this.suffixes = ImmutableSortedMap.copyOfSorted(sourceMeta.getSuffixes());
        this.weight = sourceMeta.getWeight();
        this.primaryGroup = sourceMeta.getPrimaryGroup();
        this.prefixDefinition = sourceMeta.getPrefixDefinition();
        this.suffixDefinition = sourceMeta.getSuffixDefinition();
        this.prefix = sourceMeta.getPrefix();
        this.suffix = sourceMeta.getSuffix();
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
        return this.prefix;
    }

    @Override
    public final String getPrefix() {
        return getPrefix(MetaCheckEvent.Origin.LUCKPERMS_API);
    }

    public String getSuffix(MetaCheckEvent.Origin origin) {
        return this.suffix;
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

    public int getWeight(MetaCheckEvent.Origin origin) {
        return this.weight;
    }

    //@Override - not actually exposed in the API atm
    public final int getWeight() {
        return getWeight(MetaCheckEvent.Origin.LUCKPERMS_API);
    }

    public @Nullable String getPrimaryGroup(MetaCheckEvent.Origin origin) {
        return this.primaryGroup;
    }

    @Override
    public final @Nullable String getPrimaryGroup() {
        return getPrimaryGroup(MetaCheckEvent.Origin.LUCKPERMS_API);
    }

    @Override
    public @NonNull MetaStackDefinition getPrefixStackDefinition() {
        return this.prefixDefinition;
    }

    @Override
    public @NonNull MetaStackDefinition getSuffixStackDefinition() {
        return this.suffixDefinition;
    }

    @Override
    public @NonNull QueryOptions getQueryOptions() {
        return this.queryOptions;
    }

}
