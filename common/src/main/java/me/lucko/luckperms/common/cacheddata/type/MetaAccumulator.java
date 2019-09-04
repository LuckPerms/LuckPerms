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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.metastacking.MetaStack;
import me.lucko.luckperms.common.metastacking.SimpleMetaStack;
import me.lucko.luckperms.common.node.types.Weight;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

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

    /**
     * Represents the current state of a {@link MetaAccumulator}.
     */
    private enum State {
        /** Marks that the accumulator is still gaining (accumulating) new data. */
        ACCUMULATING,
        /** Marks that this accumulator is being completed. */
        COMPLETING,
        /** Marks that the process of gaining (accumulating) new data is complete. */
        COMPLETE
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.ACCUMULATING);

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

    private void ensureState(State state) {
        if (this.state.get() != state) {
            throw new IllegalStateException("State must be " + state + ", but is actually " + this.state.get());
        }
    }

    /**
     * "Completes" the accumulator, preventing any further changes.
     *
     * Also performs some final processing on the accumulators state, before
     * data is read.
     */
    public void complete() {
        if (!this.state.compareAndSet(State.ACCUMULATING, State.COMPLETING)) {
            return;
        }

        // perform final changes
        if (!this.meta.containsKey(Weight.NODE_KEY) && this.weight != 0) {
            this.meta.put(Weight.NODE_KEY, String.valueOf(this.weight));
        }

        this.state.set(State.COMPLETE);
    }

    // accumulate methods

    public void accumulateNode(Node n) {
        ensureState(State.ACCUMULATING);
        
        if (n instanceof MetaNode) {
            MetaNode mn = (MetaNode) n;
            this.meta.put(mn.getMetaKey(), mn.getMetaValue());
        }

        if (n instanceof PrefixNode) {
            PrefixNode pn = (PrefixNode) n;
            this.prefixes.putIfAbsent(pn.getPriority(), pn.getMetaValue());
            this.prefixStack.accumulateToAll(pn);
        }
        
        if (n instanceof SuffixNode) {
            SuffixNode pn = (SuffixNode) n;
            this.suffixes.putIfAbsent(pn.getPriority(), pn.getMetaValue());
            this.suffixStack.accumulateToAll(pn);
        }
    }

    public void accumulateMeta(String key, String value) {
        ensureState(State.ACCUMULATING);
        this.meta.put(key, value);
    }

    public void accumulateWeight(int weight) {
        ensureState(State.ACCUMULATING);
        this.weight = Math.max(this.weight, weight);
    }

    // read methods

    public ListMultimap<String, String> getMeta() {
        ensureState(State.COMPLETE);
        return this.meta;
    }

    public Map<Integer, String> getChatMeta(ChatMetaType type) {
        ensureState(State.COMPLETE);
        return type == ChatMetaType.PREFIX ? this.prefixes : this.suffixes;
    }

    public SortedMap<Integer, String> getPrefixes() {
        ensureState(State.COMPLETE);
        return this.prefixes;
    }

    public SortedMap<Integer, String> getSuffixes() {
        ensureState(State.COMPLETE);
        return this.suffixes;
    }

    public int getWeight() {
        ensureState(State.COMPLETE);
        return this.weight;
    }

    public MetaStack getPrefixStack() {
        ensureState(State.COMPLETE);
        return this.prefixStack;
    }

    public MetaStack getSuffixStack() {
        ensureState(State.COMPLETE);
        return this.suffixStack;
    }

    @Override
    public String toString() {
        return "MetaAccumulator(" +
                "meta=" + this.meta + ", " +
                "prefixes=" + this.prefixes + ", " +
                "suffixes=" + this.suffixes + ", " +
                "weight=" + this.weight + ", " +
                "prefixStack=" + this.prefixStack + ", " +
                "suffixStack=" + this.suffixStack + ")";
    }
}
