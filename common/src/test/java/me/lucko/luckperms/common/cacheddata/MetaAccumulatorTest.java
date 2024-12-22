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

package me.lucko.luckperms.common.cacheddata;

import com.google.common.collect.ListMultimap;
import me.lucko.luckperms.common.cacheddata.metastack.SimpleMetaStackDefinition;
import me.lucko.luckperms.common.cacheddata.metastack.StandardStackElements;
import me.lucko.luckperms.common.cacheddata.result.IntegerResult;
import me.lucko.luckperms.common.cacheddata.result.StringResult;
import me.lucko.luckperms.common.cacheddata.type.MetaAccumulator;
import me.lucko.luckperms.common.node.types.DisplayName;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.Suffix;
import me.lucko.luckperms.common.node.types.Weight;
import net.luckperms.api.metastacking.DuplicateRemovalFunction;
import net.luckperms.api.metastacking.MetaStackDefinition;
import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.node.types.WeightNode;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;

import java.util.SortedMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetaAccumulatorTest {

    @Test
    public void testStates() {
        SimpleMetaStackDefinition definition = new SimpleMetaStackDefinition(ImmutableList.of(StandardStackElements.HIGHEST), DuplicateRemovalFunction.RETAIN_ALL, "", "", "");
        MetaAccumulator accumulator = new MetaAccumulator(definition, definition);

        assertThrows(IllegalStateException.class, accumulator::getMeta);
        assertThrows(IllegalStateException.class, () -> accumulator.getChatMeta(ChatMetaType.PREFIX));
        assertThrows(IllegalStateException.class, accumulator::getPrefixes);
        assertThrows(IllegalStateException.class, accumulator::getSuffixes);
        assertThrows(IllegalStateException.class, accumulator::getWeight);
        assertThrows(IllegalStateException.class, accumulator::getPrimaryGroup);
        assertThrows(IllegalStateException.class, accumulator::getPrefixDefinition);
        assertThrows(IllegalStateException.class, accumulator::getSuffixDefinition);
        assertThrows(IllegalStateException.class, accumulator::getPrefix);
        assertThrows(IllegalStateException.class, accumulator::getSuffix);

        Prefix prefixNode = Prefix.builder("hello", 100).build();

        accumulator.accumulateNode(prefixNode);
        accumulator.complete();

        assertThrows(IllegalStateException.class, () -> accumulator.accumulateNode(prefixNode));

        StringResult<PrefixNode> prefixResult = accumulator.getPrefix();
        assertEquals(prefixNode, prefixResult.node());
    }

    @Test
    public void testEmpty() {
        SimpleMetaStackDefinition definition = new SimpleMetaStackDefinition(ImmutableList.of(StandardStackElements.HIGHEST), DuplicateRemovalFunction.RETAIN_ALL, "[", "|", "]");
        MetaAccumulator accumulator = new MetaAccumulator(definition, definition);
        accumulator.complete();

        ListMultimap<String, StringResult<MetaNode>> meta = accumulator.getMeta();
        assertEquals(0, meta.size());

        SortedMap<Integer, StringResult<PrefixNode>> prefixes = accumulator.getPrefixes();
        assertEquals(0, prefixes.size());

        SortedMap<Integer, StringResult<SuffixNode>> suffixes = accumulator.getSuffixes();
        assertEquals(0, suffixes.size());

        IntegerResult<WeightNode> weight = accumulator.getWeight();
        assertTrue(weight.isNull());
        assertEquals(0, weight.intResult());
        assertNull(weight.node());

        String primaryGroup = accumulator.getPrimaryGroup();
        assertNull(primaryGroup);

        MetaStackDefinition prefixDefinition = accumulator.getPrefixDefinition();
        assertSame(definition, prefixDefinition);

        MetaStackDefinition suffixDefinition = accumulator.getSuffixDefinition();
        assertSame(definition, suffixDefinition);

        StringResult<PrefixNode> prefix = accumulator.getPrefix();
        assertNull(prefix.result());
        assertNull(prefix.node());

        StringResult<SuffixNode> suffix = accumulator.getSuffix();
        assertNull(suffix.result());
        assertNull(suffix.node());
    }

    @Test
    public void testSimple() {
        SimpleMetaStackDefinition definition = new SimpleMetaStackDefinition(ImmutableList.of(StandardStackElements.HIGHEST), DuplicateRemovalFunction.RETAIN_ALL, "[", "|", "]");
        MetaAccumulator accumulator = new MetaAccumulator(definition, definition);

        accumulator.accumulateNode(Prefix.builder("b", 90).build());
        accumulator.accumulateNode(Prefix.builder("a", 100).build());
        accumulator.accumulateNode(Prefix.builder("c", 80).build());
        accumulator.accumulateNode(Suffix.builder("foo", 80).build());
        accumulator.accumulateNode(Meta.builder().key("foo").value("bar").build());
        accumulator.accumulateNode(Weight.builder(10).build()); // ignored
        accumulator.accumulateNode(DisplayName.builder("hello").build());
        accumulator.accumulateWeight(IntegerResult.of(Weight.builder(5).build()));
        accumulator.setPrimaryGroup("member");

        accumulator.complete();

        StringResult<PrefixNode> prefix = accumulator.getPrefix();
        assertEquals("[a]", prefix.result());

        SortedMap<Integer, StringResult<PrefixNode>> prefixes = accumulator.getPrefixes();
        assertEquals(ImmutableSet.of(100, 90, 80), prefixes.keySet());

        StringResult<SuffixNode> suffix = accumulator.getSuffix();
        assertEquals("[foo]", suffix.result());

        SortedMap<Integer, StringResult<SuffixNode>> suffixes = accumulator.getSuffixes();
        assertEquals(ImmutableSet.of(80), suffixes.keySet());

        ListMultimap<String, StringResult<MetaNode>> meta = accumulator.getMeta();
        assertEquals(3, meta.size());
        assertEquals(ImmutableSet.of("foo", "weight", "primarygroup"), meta.keySet());
        assertEquals("bar", meta.get("foo").get(0).result());
        assertEquals("5", meta.get("weight").get(0).result());
        assertEquals("member", meta.get("primarygroup").get(0).result());

        IntegerResult<WeightNode> weight = accumulator.getWeight();
        assertEquals(5, weight.intResult());

        String primaryGroup = accumulator.getPrimaryGroup();
        assertEquals("member", primaryGroup);
    }

}
