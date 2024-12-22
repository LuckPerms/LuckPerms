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

import me.lucko.luckperms.common.cacheddata.metastack.SimpleMetaStackDefinition;
import me.lucko.luckperms.common.cacheddata.metastack.StandardStackElements;
import me.lucko.luckperms.common.cacheddata.result.StringResult;
import me.lucko.luckperms.common.cacheddata.type.MetaStackAccumulator;
import me.lucko.luckperms.common.node.types.Prefix;
import net.luckperms.api.metastacking.DuplicateRemovalFunction;
import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.node.types.PrefixNode;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MetaStackAccumulatorTest {

    @Test
    public void testEmpty() {
        SimpleMetaStackDefinition definition = new SimpleMetaStackDefinition(ImmutableList.of(StandardStackElements.HIGHEST), DuplicateRemovalFunction.RETAIN_ALL, "", "", "");
        MetaStackAccumulator<PrefixNode> accumulator = new MetaStackAccumulator<>(definition, ChatMetaType.PREFIX);

        StringResult<PrefixNode> result = accumulator.toResult();
        assertNotNull(result);
        assertNull(result.result());
        assertNull(result.node());
        assertNull(result.overriddenResult());

        String formattedString = accumulator.toFormattedString();
        assertNull(formattedString);
    }

    @Test
    public void testSingle() {
        SimpleMetaStackDefinition definition = new SimpleMetaStackDefinition(ImmutableList.of(StandardStackElements.HIGHEST), DuplicateRemovalFunction.RETAIN_ALL, "[", "|", "]");
        MetaStackAccumulator<PrefixNode> accumulator = new MetaStackAccumulator<>(definition, ChatMetaType.PREFIX);

        PrefixNode a = Prefix.builder("a", 100).build();
        PrefixNode b = Prefix.builder("b", 90).build();
        PrefixNode c = Prefix.builder("c", 80).build();

        accumulator.offer(b);
        accumulator.offer(a);
        accumulator.offer(c);

        StringResult<PrefixNode> result = accumulator.toResult();
        assertNotNull(result);
        assertEquals("[a]", result.result());
        assertEquals(a, result.node());
        assertNull(result.overriddenResult());
    }

    @Test
    public void testMultiple() {
        SimpleMetaStackDefinition definition = new SimpleMetaStackDefinition(ImmutableList.of(StandardStackElements.LOWEST, StandardStackElements.HIGHEST), DuplicateRemovalFunction.RETAIN_ALL, "[", "|", "]");
        MetaStackAccumulator<PrefixNode> accumulator = new MetaStackAccumulator<>(definition, ChatMetaType.PREFIX);

        PrefixNode a = Prefix.builder("a", 100).build();
        PrefixNode b = Prefix.builder("b", 90).build();
        PrefixNode c = Prefix.builder("c", 80).build();

        accumulator.offer(b);
        accumulator.offer(a);
        accumulator.offer(c);

        StringResult<PrefixNode> result = accumulator.toResult();
        assertNotNull(result);
        assertEquals("[c|a]", result.result());
        assertEquals(c, result.node());

        StringResult<PrefixNode> overriddenResult = result.overriddenResult();
        assertNotNull(overriddenResult);
        assertEquals(a, overriddenResult.node());
        assertNull(overriddenResult.overriddenResult());
    }

}
