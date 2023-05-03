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
import me.lucko.luckperms.common.cacheddata.CacheMetadata;
import me.lucko.luckperms.common.cacheddata.result.IntegerResult;
import me.lucko.luckperms.common.cacheddata.result.StringResult;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.Suffix;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.node.types.WeightNode;
import net.luckperms.api.query.QueryOptions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Holds cached meta for a given context
 */
public class MonitoredMetaCache extends MetaCache implements CachedMetaData {

    /** The plugin instance */
    private final LuckPermsPlugin plugin;

    /** The metadata for this cache */
    private final CacheMetadata metadata;

    public MonitoredMetaCache(LuckPermsPlugin plugin, QueryOptions queryOptions, CacheMetadata metadata, MetaAccumulator sourceMeta) {
        super(plugin, queryOptions, sourceMeta);
        this.plugin = plugin;
        this.metadata = metadata;
    }

    @Override
    public @NonNull StringResult<MetaNode> getMetaValue(String key, CheckOrigin origin) {
        StringResult<MetaNode> value = super.getMetaValue(key, origin);
        this.plugin.getVerboseHandler().offerMetaCheckEvent(origin, this.metadata.getVerboseCheckInfo(), this.metadata.getQueryOptions(), key, value);
        return value;
    }

    @Override
    public @NonNull StringResult<PrefixNode> getPrefix(CheckOrigin origin) {
        StringResult<PrefixNode> value = super.getPrefix(origin);
        this.plugin.getVerboseHandler().offerMetaCheckEvent(origin, this.metadata.getVerboseCheckInfo(), this.metadata.getQueryOptions(), Prefix.NODE_KEY, value);
        return value;
    }

    @Override
    public @NonNull StringResult<SuffixNode> getSuffix(CheckOrigin origin) {
        StringResult<SuffixNode> value = super.getSuffix(origin);
        this.plugin.getVerboseHandler().offerMetaCheckEvent(origin, this.metadata.getVerboseCheckInfo(), this.metadata.getQueryOptions(), Suffix.NODE_KEY, value);
        return value;
    }

    @Override
    public @NonNull Map<String, List<StringResult<MetaNode>>> getMetaResults(CheckOrigin origin) {
        return new MonitoredMetaMap(super.getMetaResults(origin), origin);
    }

    @Override
    public @NonNull IntegerResult<WeightNode> getWeight(CheckOrigin origin) {
        IntegerResult<WeightNode> value = super.getWeight(origin);
        this.plugin.getVerboseHandler().offerMetaCheckEvent(origin, this.metadata.getVerboseCheckInfo(), this.metadata.getQueryOptions(), "weight", value.asStringResult());
        return value;
    }

    @Override
    public @Nullable String getPrimaryGroup(CheckOrigin origin) {
        String value = super.getPrimaryGroup(origin);
        this.plugin.getVerboseHandler().offerMetaCheckEvent(origin, this.metadata.getVerboseCheckInfo(), this.metadata.getQueryOptions(), "primarygroup", StringResult.of(value));
        return value;
    }

    private final class MonitoredMetaMap extends ForwardingMap<String, List<StringResult<MetaNode>>> {
        private final Map<String, List<StringResult<MetaNode>>> delegate;
        private final CheckOrigin origin;

        private MonitoredMetaMap(Map<String, List<StringResult<MetaNode>>> delegate, CheckOrigin origin) {
            this.delegate = delegate;
            this.origin = origin;
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
            List<StringResult<MetaNode>> values = super.get(key);

            if (values == null || values.isEmpty()) {
                MonitoredMetaCache.this.plugin.getVerboseHandler().offerMetaCheckEvent(this.origin, MonitoredMetaCache.this.metadata.getVerboseCheckInfo(), MonitoredMetaCache.this.metadata.getQueryOptions(), key, StringResult.nullResult());
            } else {
                Iterator<StringResult<MetaNode>> it = values.iterator();
                StringResult<MetaNode> result = it.next().copy();

                StringResult<MetaNode> root = result;
                while (it.hasNext()) {
                    StringResult<MetaNode> nested = it.next().copy();
                    root.setOverriddenResult(nested);
                    root = nested;
                }

                MonitoredMetaCache.this.plugin.getVerboseHandler().offerMetaCheckEvent(this.origin, MonitoredMetaCache.this.metadata.getVerboseCheckInfo(), MonitoredMetaCache.this.metadata.getQueryOptions(), key, result);
            }

            return values;
        }
    }

}
