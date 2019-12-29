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
import com.google.common.collect.Multimaps;

import me.lucko.luckperms.common.cacheddata.CacheMetadata;
import me.lucko.luckperms.common.metastacking.MetaStack;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.Suffix;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.verbose.event.MetaCheckEvent;

import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.metastacking.MetaStackDefinition;
import net.luckperms.api.query.QueryOptions;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

/**
 * Holds cached meta for a given context
 */
public class MetaCache implements CachedMetaData {

    /** The plugin instance */
    private final LuckPermsPlugin plugin;

    /** The query options this container is holding data for */
    private final QueryOptions queryOptions;

    /** The metadata for this cache */
    private final CacheMetadata metadata;

    /** The object name passed to the verbose handler when checks are made */
    private final String verboseCheckTarget;

    /* The data */
    private Map<String, List<String>> meta = ImmutableMap.of();
    private Map<String, String> flattenedMeta = ImmutableMap.of();
    private SortedMap<Integer, String> prefixes = ImmutableSortedMap.of();
    private SortedMap<Integer, String> suffixes = ImmutableSortedMap.of();
    private MetaStack prefixStack = null;
    private MetaStack suffixStack = null;

    public MetaCache(LuckPermsPlugin plugin, QueryOptions queryOptions, CacheMetadata metadata) {
        this.plugin = plugin;
        this.queryOptions = queryOptions;
        this.metadata = metadata;

        if (this.metadata.getHolderType() == HolderType.GROUP) {
            this.verboseCheckTarget = "group/" + this.metadata.getObjectName();
        } else {
            this.verboseCheckTarget = this.metadata.getObjectName();
        }
    }

    public void loadMeta(MetaAccumulator meta) {
        meta.complete();

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
        this.prefixStack = meta.getPrefixStack();
        this.suffixStack = meta.getSuffixStack();
    }

    public String getMetaValue(String key, MetaCheckEvent.Origin origin) {
        Objects.requireNonNull(key, "key");
        String value = this.flattenedMeta.get(key);
        this.plugin.getVerboseHandler().offerMetaCheckEvent(origin, this.verboseCheckTarget, this.metadata.getQueryOptions(), key, String.valueOf(value));
        return value;
    }

    @Override
    public String getMetaValue(String key) {
        return getMetaValue(key, MetaCheckEvent.Origin.LUCKPERMS_API);
    }

    public String getPrefix(MetaCheckEvent.Origin origin) {
        MetaStack prefixStack = this.prefixStack;
        String value = prefixStack == null ? null : prefixStack.toFormattedString();
        this.plugin.getVerboseHandler().offerMetaCheckEvent(origin, this.verboseCheckTarget, this.metadata.getQueryOptions(), Prefix.NODE_KEY, String.valueOf(value));
        return value;
    }

    @Override
    public String getPrefix() {
        return getPrefix(MetaCheckEvent.Origin.LUCKPERMS_API);
    }

    public String getSuffix(MetaCheckEvent.Origin origin) {
        MetaStack suffixStack = this.suffixStack;
        String value = suffixStack == null ? null : suffixStack.toFormattedString();
        this.plugin.getVerboseHandler().offerMetaCheckEvent(origin, this.verboseCheckTarget, this.metadata.getQueryOptions(), Suffix.NODE_KEY, String.valueOf(value));
        return value;
    }

    @Override
    public String getSuffix() {
        return getSuffix(MetaCheckEvent.Origin.LUCKPERMS_API);
    }

    public Map<String, List<String>> getMeta(MetaCheckEvent.Origin origin) {
        return new MonitoredMetaMap(origin);
    }

    @Override
    public @NonNull Map<String, List<String>> getMeta() {
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

    private final class MonitoredMetaMap extends ForwardingMap<String, List<String>> {
        private final MetaCheckEvent.Origin origin;

        MonitoredMetaMap(MetaCheckEvent.Origin origin) {
            this.origin = origin;
        }

        @Override
        protected Map<String, List<String>> delegate() {
            return MetaCache.this.meta;
        }

        @Override
        public List<String> get(Object k) {
            if (k == null) {
                return null;
            }

            String key = (String) k;
            List<String> values = super.get(key);
            MetaCache.this.plugin.getVerboseHandler().offerMetaCheckEvent(this.origin, MetaCache.this.verboseCheckTarget, MetaCache.this.metadata.getQueryOptions(), key, String.valueOf(values));
            return values;
        }
    }

}
