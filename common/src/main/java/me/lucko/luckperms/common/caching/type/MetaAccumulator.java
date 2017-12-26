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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.metastacking.MetaStack;
import me.lucko.luckperms.common.metastacking.SimpleMetaStack;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Holds temporary mutable meta whilst this object is passed up the
 * inheritance tree to accumulate meta from parents
 */
@Getter
@ToString
public class MetaAccumulator {
    public static MetaAccumulator makeFromConfig(LuckPermsPlugin plugin) {
        return new MetaAccumulator(
                new SimpleMetaStack(plugin.getConfiguration().get(ConfigKeys.PREFIX_FORMATTING_OPTIONS), ChatMetaType.PREFIX),
                new SimpleMetaStack(plugin.getConfiguration().get(ConfigKeys.SUFFIX_FORMATTING_OPTIONS), ChatMetaType.SUFFIX)
        );
    }

    @Getter(AccessLevel.NONE)
    private final ListMultimap<String, String> meta;
    private final SortedMap<Integer, String> prefixes;
    private final SortedMap<Integer, String> suffixes;
    private int weight = 0;

    private final MetaStack prefixStack;
    private final MetaStack suffixStack;

    public MetaAccumulator(@NonNull MetaStack prefixStack, @NonNull MetaStack suffixStack) {
        this.meta = ArrayListMultimap.create();
        this.prefixes = new TreeMap<>(Comparator.reverseOrder());
        this.suffixes = new TreeMap<>(Comparator.reverseOrder());
        this.prefixStack = prefixStack;
        this.suffixStack = suffixStack;
    }

    public void accumulateNode(LocalizedNode n) {
        if (n.isMeta()) {
            Map.Entry<String, String> entry = n.getMeta();
            meta.put(entry.getKey(), entry.getValue());
        }

        if (n.isPrefix()) {
            Map.Entry<Integer, String> value = n.getPrefix();
            prefixes.putIfAbsent(value.getKey(), value.getValue());
            prefixStack.accumulateToAll(n);
        }

        if (n.isSuffix()) {
            Map.Entry<Integer, String> value = n.getSuffix();
            suffixes.putIfAbsent(value.getKey(), value.getValue());
            suffixStack.accumulateToAll(n);
        }
    }

    public void accumulateWeight(int weight) {
        this.weight = Math.max(this.weight, weight);
    }

    // We can assume that if this method is being called, this holder is effectively finalized.
    // (it's not going to accumulate more nodes)
    // Therefore, it should be ok to set the weight meta key, if not already present.
    public ListMultimap<String, String> getMeta() {
        if (!this.meta.containsKey(NodeFactory.WEIGHT_KEY) && this.weight != 0) {
            this.meta.put(NodeFactory.WEIGHT_KEY, String.valueOf(this.weight));
        }

        return this.meta;
    }

    public Map<Integer, String> getChatMeta(ChatMetaType type) {
        return type == ChatMetaType.PREFIX ? prefixes : suffixes;
    }

    public MetaStack getStack(ChatMetaType type) {
        return type == ChatMetaType.PREFIX ? prefixStack : suffixStack;
    }

}
