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
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.Suffix;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.verbose.event.MetaCheckEvent;

import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.query.QueryOptions;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Holds cached meta for a given context
 */
public class MetaCache extends SimpleMetaCache implements CachedMetaData {

    /** The plugin instance */
    private final LuckPermsPlugin plugin;

    /** The metadata for this cache */
    private final CacheMetadata metadata;

    /** The object name passed to the verbose handler when checks are made */
    private final String verboseCheckTarget;

    public MetaCache(LuckPermsPlugin plugin, QueryOptions queryOptions, CacheMetadata metadata, MetaAccumulator sourceMeta) {
        super(plugin, queryOptions, sourceMeta);
        this.plugin = plugin;
        this.metadata = metadata;

        if (this.metadata.getHolderType() == HolderType.GROUP) {
            this.verboseCheckTarget = "group/" + this.metadata.getObjectName();
        } else {
            this.verboseCheckTarget = this.metadata.getObjectName();
        }
    }

    @Override
    public String getMetaValue(String key, MetaCheckEvent.Origin origin) {
        String value = super.getMetaValue(key, origin);
        this.plugin.getVerboseHandler().offerMetaCheckEvent(origin, this.verboseCheckTarget, this.metadata.getQueryOptions(), key, String.valueOf(value));
        return value;
    }

    @Override
    public String getPrefix(MetaCheckEvent.Origin origin) {
        String value = super.getPrefix(origin);
        this.plugin.getVerboseHandler().offerMetaCheckEvent(origin, this.verboseCheckTarget, this.metadata.getQueryOptions(), Prefix.NODE_KEY, String.valueOf(value));
        return value;
    }

    @Override
    public String getSuffix(MetaCheckEvent.Origin origin) {
        String value = super.getSuffix(origin);
        this.plugin.getVerboseHandler().offerMetaCheckEvent(origin, this.verboseCheckTarget, this.metadata.getQueryOptions(), Suffix.NODE_KEY, String.valueOf(value));
        return value;
    }

    @Override
    public Map<String, List<String>> getMeta(MetaCheckEvent.Origin origin) {
        return new MonitoredMetaMap(super.getMeta(origin), origin);
    }

    @Override
    public int getWeight(MetaCheckEvent.Origin origin) {
        int value = super.getWeight(origin);
        this.plugin.getVerboseHandler().offerMetaCheckEvent(origin, this.verboseCheckTarget, this.metadata.getQueryOptions(), "weight", String.valueOf(value));
        return value;
    }

    @Override
    public @Nullable String getPrimaryGroup(MetaCheckEvent.Origin origin) {
        String value = super.getPrimaryGroup(origin);
        this.plugin.getVerboseHandler().offerMetaCheckEvent(origin, this.verboseCheckTarget, this.metadata.getQueryOptions(), "primarygroup", String.valueOf(value));
        return value;
    }

    private final class MonitoredMetaMap extends ForwardingMap<String, List<String>> {
        private final Map<String, List<String>> delegate;
        private final MetaCheckEvent.Origin origin;

        private MonitoredMetaMap(Map<String, List<String>> delegate, MetaCheckEvent.Origin origin) {
            this.delegate = delegate;
            this.origin = origin;
        }

        @Override
        protected Map<String, List<String>> delegate() {
            return this.delegate;
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
