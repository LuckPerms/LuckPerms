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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.nodetype.types.MetaType;
import me.lucko.luckperms.api.nodetype.types.PrefixType;
import me.lucko.luckperms.api.nodetype.types.SuffixType;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.metastacking.MetaStack;
import me.lucko.luckperms.common.metastacking.SimpleMetaStack;
import me.lucko.luckperms.common.node.model.NodeTypes;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Holds temporary mutable meta whilst this object is passed up the
 * inheritance tree to accumulate meta from parents
 */
public class MetaAccumulator {
    public static MetaAccumulator makeFromConfig(LuckPermsPlugin plugin) {
        return new MetaAccumulator(
                new SimpleMetaStack(plugin.getConfiguration().get(ConfigKeys.PREFIX_FORMATTING_OPTIONS), ChatMetaType.PREFIX),
                new SimpleMetaStack(plugin.getConfiguration().get(ConfigKeys.SUFFIX_FORMATTING_OPTIONS), ChatMetaType.SUFFIX)
        );
    }

    private final ListMultimap<String, String> meta;
    private final SortedMap<Integer, String> prefixes;
    private final SortedMap<Integer, String> suffixes;
    private int weight = 0;

    private final MetaStack prefixStack;
    private final MetaStack suffixStack;

    public MetaAccumulator(MetaStack prefixStack, MetaStack suffixStack) {
        Objects.requireNonNull(prefixStack, "prefixStack");
        Objects.requireNonNull(suffixStack, "suffixStack");
        this.meta = ArrayListMultimap.create();
        this.prefixes = new TreeMap<>(Comparator.reverseOrder());
        this.suffixes = new TreeMap<>(Comparator.reverseOrder());
        this.prefixStack = prefixStack;
        this.suffixStack = suffixStack;
    }

    public void accumulateNode(LocalizedNode n) {
        n.getTypeData(MetaType.KEY).ifPresent(metaType ->
                this.meta.put(metaType.getKey(), metaType.getValue())
        );

        n.getTypeData(PrefixType.KEY).ifPresent(prefix -> {
            this.prefixes.putIfAbsent(prefix.getPriority(), prefix.getPrefix());
            this.prefixStack.accumulateToAll(n);
        });

        n.getTypeData(SuffixType.KEY).ifPresent(suffix -> {
            this.suffixes.putIfAbsent(suffix.getPriority(), suffix.getSuffix());
            this.suffixStack.accumulateToAll(n);
        });
    }

    public void accumulateMeta(String key, String value) {
        this.meta.put(key, value);
    }

    public void accumulateWeight(int weight) {
        this.weight = Math.max(this.weight, weight);
    }

    // We can assume that if this method is being called, this holder is effectively finalized.
    // (it's not going to accumulate more nodes)
    // Therefore, it should be ok to set the weight meta key, if not already present.
    public ListMultimap<String, String> getMeta() {
        if (!this.meta.containsKey(NodeTypes.WEIGHT_KEY) && this.weight != 0) {
            this.meta.put(NodeTypes.WEIGHT_KEY, String.valueOf(this.weight));
        }

        return this.meta;
    }

    public Map<Integer, String> getChatMeta(ChatMetaType type) {
        return type == ChatMetaType.PREFIX ? this.prefixes : this.suffixes;
    }

    public MetaStack getStack(ChatMetaType type) {
        return type == ChatMetaType.PREFIX ? this.prefixStack : this.suffixStack;
    }

    public SortedMap<Integer, String> getPrefixes() {
        return this.prefixes;
    }

    public SortedMap<Integer, String> getSuffixes() {
        return this.suffixes;
    }

    public int getWeight() {
        return this.weight;
    }

    public MetaStack getPrefixStack() {
        return this.prefixStack;
    }

    public MetaStack getSuffixStack() {
        return this.suffixStack;
    }

    @Override
    public String toString() {
        return "MetaAccumulator(" +
                "meta=" + this.getMeta() + ", " +
                "prefixes=" + this.getPrefixes() + ", " +
                "suffixes=" + this.getSuffixes() + ", " +
                "weight=" + this.getWeight() + ", " +
                "prefixStack=" + this.getPrefixStack() + ", " +
                "suffixStack=" + this.getSuffixStack() + ")";
    }
}
