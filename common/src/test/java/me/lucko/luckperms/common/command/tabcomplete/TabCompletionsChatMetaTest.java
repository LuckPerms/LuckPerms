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

package me.lucko.luckperms.common.command.tabcomplete;

import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.nodemap.RecordedNodeMap;
import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TabCompletionsChatMetaTest {

    private static PermissionHolder holderWithNodes(Node... nodes) {
        PermissionHolder holder = mock(PermissionHolder.class);
        RecordedNodeMap map = mock(RecordedNodeMap.class);
        when(map.asList()).thenReturn(Arrays.asList(nodes));
        when(holder.normalData()).thenReturn(map);
        return holder;
    }

    private static PrefixNode prefix(String value, int priority) {
        PrefixNode node = mock(PrefixNode.class);
        when(node.getMetaValue()).thenReturn(value);
        when(node.getPriority()).thenReturn(priority);
        return node;
    }

    private static SuffixNode suffix(String value, int priority) {
        SuffixNode node = mock(SuffixNode.class);
        when(node.getMetaValue()).thenReturn(value);
        when(node.getPriority()).thenReturn(priority);
        return node;
    }

    private static ArgumentList args(String... args) {
        return new ArgumentList(Arrays.asList(args));
    }

    @Test
    public void testCompletesExistingValueAtPriority() {
        PermissionHolder holder = holderWithNodes(
                prefix("&8[&#ECC97DMember&8]&#ECC97D", 1),
                prefix("&7Admin", 2)
        );

        CompletionSupplier supplier = TabCompletions.chatMeta(ChatMetaType.PREFIX, holder, args("1", ""));

        List<String> result = supplier.supplyCompletions("");
        assertEquals(Collections.singletonList("\"&8[&#ECC97DMember&8]&#ECC97D\" "), result);
    }

    @Test
    public void testOnlyCompletesValuesAtTheGivenPriority() {
        PermissionHolder holder = holderWithNodes(
                prefix("&8Member", 1),
                prefix("&7Admin", 2)
        );

        CompletionSupplier supplier = TabCompletions.chatMeta(ChatMetaType.PREFIX, holder, args("2", ""));

        assertEquals(Collections.singletonList("\"&7Admin\" "), supplier.supplyCompletions(""));
    }

    @Test
    public void testCompletesNoValuesWhenPriorityIsUnset() {
        PermissionHolder holder = holderWithNodes(
                prefix("&8Member", 1)
        );

        CompletionSupplier supplier = TabCompletions.chatMeta(ChatMetaType.PREFIX, holder, args("5", ""));

        assertEquals(Collections.emptyList(), supplier.supplyCompletions(""));
    }

    @Test
    public void testCompletesAllValuesWhenNoPriorityProvided() {
        PermissionHolder holder = holderWithNodes(
                prefix("&8Member", 1),
                prefix("&7Admin", 2)
        );

        CompletionSupplier supplier = TabCompletions.chatMeta(ChatMetaType.PREFIX, holder, args(""));

        assertEquals(Arrays.asList("\"&8Member\" ", "\"&7Admin\" "), supplier.supplyCompletions(""));
    }

    @Test
    public void testFiltersByPartial() {
        PermissionHolder holder = holderWithNodes(
                prefix("&8Member", 1),
                prefix("&7Admin", 2)
        );

        CompletionSupplier supplier = TabCompletions.chatMeta(ChatMetaType.PREFIX, holder, args(""));

        assertEquals(Collections.singletonList("\"&7Admin\" "), supplier.supplyCompletions("&7"));
    }

    @Test
    public void testFiltersByPartialWithLeadingQuote() {
        PermissionHolder holder = holderWithNodes(
                prefix("&8Member", 1)
        );

        CompletionSupplier supplier = TabCompletions.chatMeta(ChatMetaType.PREFIX, holder, args(""));

        assertEquals(Collections.singletonList("\"&8Member\" "), supplier.supplyCompletions("\"&8"));
    }

    @Test
    public void testDistinctValues() {
        PermissionHolder holder = holderWithNodes(
                prefix("&8Member", 1),
                prefix("&8Member", 2)
        );

        CompletionSupplier supplier = TabCompletions.chatMeta(ChatMetaType.PREFIX, holder, args(""));

        assertEquals(Collections.singletonList("\"&8Member\" "), supplier.supplyCompletions(""));
    }

    @Test
    public void testOnlyCompletesTheGivenType() {
        PermissionHolder holder = holderWithNodes(
                prefix("&8Member", 1),
                suffix("&7Admin", 1)
        );

        CompletionSupplier supplier = TabCompletions.chatMeta(ChatMetaType.PREFIX, holder, args("1", ""));

        assertEquals(Collections.singletonList("\"&8Member\" "), supplier.supplyCompletions(""));
    }

    @Test
    public void testEmptyWhenTargetIsNull() {
        CompletionSupplier supplier = TabCompletions.chatMeta(ChatMetaType.PREFIX, null, args("1", ""));

        assertEquals(Collections.emptyList(), supplier.supplyCompletions(""));
    }

    @Test
    public void testNumericPartialIsNotTreatedAsCompletedPriority() {
        PermissionHolder holder = holderWithNodes(prefix("123Member", 5));
        CompletionSupplier supplier = TabCompletions.chatMeta(ChatMetaType.PREFIX, holder, args("1"));

        assertEquals(Collections.singletonList("\"123Member\" "), supplier.supplyCompletions("1"));
    }

}
