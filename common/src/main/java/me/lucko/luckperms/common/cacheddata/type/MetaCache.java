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

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;

import me.lucko.luckperms.common.cacheddata.UsageTracked;
import me.lucko.luckperms.common.cacheddata.result.StringResult;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;

import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.metastacking.MetaStackDefinition;
import net.luckperms.api.node.types.ChatMetaNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.query.meta.MetaValueSelector;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

/**
 * Holds cached meta for a given context
 */
public class MetaCache extends UsageTracked implements CachedMetaData {

    private final LuckPermsPlugin plugin;

    /** The query options this container is holding data for */
    private final QueryOptions queryOptions;

    /* The data */
    private final Map<String, List<StringResult<MetaNode>>> meta;
    private final Map<String, StringResult<MetaNode>> flattenedMeta;
    private final SortedMap<Integer, StringResult<ChatMetaNode<?, ?>>> prefixes;
    private final SortedMap<Integer, StringResult<ChatMetaNode<?, ?>>> suffixes;
    private final int weight;
    private final String primaryGroup;
    private final MetaStackDefinition prefixDefinition;
    private final MetaStackDefinition suffixDefinition;
    private final StringResult<ChatMetaNode<?, ?>> prefix;
    private final StringResult<ChatMetaNode<?, ?>> suffix;

    public MetaCache(LuckPermsPlugin plugin, QueryOptions queryOptions, MetaAccumulator sourceMeta) {
        this.plugin = plugin;
        this.queryOptions = queryOptions;

        Map<String, List<StringResult<MetaNode>>> meta = Multimaps.asMap(ImmutableListMultimap.copyOf(sourceMeta.getMeta()));

        MetaValueSelector metaValueSelector = this.queryOptions.option(MetaValueSelector.KEY)
                .orElseGet(() -> this.plugin.getConfiguration().get(ConfigKeys.META_VALUE_SELECTOR));

        ImmutableMap.Builder<String, StringResult<MetaNode>> builder = ImmutableMap.builder();
        for (Map.Entry<String, List<StringResult<MetaNode>>> e : meta.entrySet()) {
            if (e.getValue().isEmpty()) {
                continue;
            }

            MetaNode selected = metaValueSelector.selectValue(e.getKey(), Lists.transform(e.getValue(), StringResult::node));
            if (selected == null) {
                throw new NullPointerException(metaValueSelector + " returned null");
            }

            builder.put(e.getKey(), StringResult.of(selected));
        }
        this.flattenedMeta = builder.build();
        this.meta = new LowerCaseMetaMap(meta);

        this.prefixes = ImmutableSortedMap.copyOfSorted(sourceMeta.getPrefixes());
        this.suffixes = ImmutableSortedMap.copyOfSorted(sourceMeta.getSuffixes());
        this.weight = sourceMeta.getWeight();
        this.primaryGroup = sourceMeta.getPrimaryGroup();
        this.prefixDefinition = sourceMeta.getPrefixDefinition();
        this.suffixDefinition = sourceMeta.getSuffixDefinition();
        this.prefix = sourceMeta.getPrefix();
        this.suffix = sourceMeta.getSuffix();
    }

    public @NonNull StringResult<MetaNode> getMetaValue(String key, CheckOrigin origin) {
        Objects.requireNonNull(key, "key");
        return this.flattenedMeta.getOrDefault(key.toLowerCase(Locale.ROOT), StringResult.nullResult());
    }

    @Override
    public final @Nullable String getMetaValue(@NonNull String key) {
        return getMetaValue(key, CheckOrigin.LUCKPERMS_API).result();
    }

    public @NonNull StringResult<ChatMetaNode<?,?>> getPrefix(CheckOrigin origin) {
        return this.prefix;
    }

    @Override
    public final @Nullable String getPrefix() {
        return getPrefix(CheckOrigin.LUCKPERMS_API).result();
    }

    public @NonNull StringResult<ChatMetaNode<?,?>> getSuffix(CheckOrigin origin) {
        return this.suffix;
    }

    @Override
    public final @Nullable String getSuffix() {
        return getSuffix(CheckOrigin.LUCKPERMS_API).result();
    }

    protected Map<String, List<StringResult<MetaNode>>> getMetaResults(CheckOrigin origin) {
        return this.meta;
    }

    public final Map<String, List<String>> getMeta(CheckOrigin origin) {
        return Maps.transformValues(getMetaResults(origin), list -> Lists.transform(list, StringResult::result));
    }

    @Override
    public final @NonNull Map<String, List<String>> getMeta() {
        return getMeta(CheckOrigin.LUCKPERMS_API);
    }

    @Override
    public @NonNull SortedMap<Integer, String> getPrefixes() {
        return Maps.transformValues(this.prefixes, StringResult::result);
    }

    @Override
    public @NonNull SortedMap<Integer, String> getSuffixes() {
        return Maps.transformValues(this.suffixes, StringResult::result);
    }

    public int getWeight(CheckOrigin origin) {
        return this.weight;
    }

    //@Override - not actually exposed in the API atm
    public final int getWeight() {
        return getWeight(CheckOrigin.LUCKPERMS_API);
    }

    public @Nullable String getPrimaryGroup(CheckOrigin origin) {
        return this.primaryGroup;
    }

    @Override
    public final @Nullable String getPrimaryGroup() {
        return getPrimaryGroup(CheckOrigin.LUCKPERMS_API);
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

    private static final class LowerCaseMetaMap extends ForwardingMap<String, List<StringResult<MetaNode>>> {
        private final Map<String, List<StringResult<MetaNode>>> delegate;

        private LowerCaseMetaMap(Map<String, List<StringResult<MetaNode>>> delegate) {
            this.delegate = delegate;
        }

        @Override
        protected Map<String, List<StringResult<MetaNode>>> delegate() {
            return this.delegate;
        }

        @Override
        public List<StringResult<MetaNode>> get(Object k) {
            if (k == null) {
                return null;
            }

            String key = (String) k;
            return super.get(key.toLowerCase(Locale.ROOT));
        }
    }

}
