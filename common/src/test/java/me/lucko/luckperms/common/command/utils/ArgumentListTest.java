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

package me.lucko.luckperms.common.command.utils;

import com.google.common.collect.ImmutableList;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import net.luckperms.api.context.ContextSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ArgumentListTest {

    @Test
    public void testGetString() {
        ArgumentList list = new ArgumentList(ImmutableList.of("hello", "world{SPACE}"));

        assertEquals("hello", list.getOrDefault(0, "def"));
        assertEquals("world ", list.getOrDefault(1, "def"));
        assertEquals("def", list.getOrDefault(2, "def"));
        assertEquals("def", list.getOrDefault(-1, "def"));
        assertNull(list.getOrDefault(2, null));
        assertNull(list.getOrDefault(-1, null));
    }

    @Test
    public void testGetInt() {
        ArgumentList list = new ArgumentList(ImmutableList.of("5", "-50"));

        assertEquals(5, list.getIntOrDefault(0, -1));
        assertEquals(-50, list.getIntOrDefault(1, -1));
        assertEquals(-1, list.getIntOrDefault(2, -1));
        assertEquals(-1, list.getIntOrDefault(-1, -1));
    }

    private static Stream<Arguments> testParseContext() {
        return Stream.of(
                Arguments.of(new String[]{}, ImmutableContextSetImpl.EMPTY),
                Arguments.of(new String[]{"test"}, ImmutableContextSetImpl.of("server", "test")),
                Arguments.of(
                        new String[]{"a", "b", "c"},
                        new ImmutableContextSetImpl.BuilderImpl()
                                .add("server", "a")
                                .add("world", "b")
                                .add("server", "c")
                                .build()
                ),
                Arguments.of(
                        new String[]{"a", "thing=b", "c"},
                        new ImmutableContextSetImpl.BuilderImpl()
                                .add("server", "a")
                                .add("thing", "b")
                                .add("server", "c")
                                .build()
                ),
                Arguments.of(
                        new String[]{"thing=a", "thing=b", "c"},
                        new ImmutableContextSetImpl.BuilderImpl()
                                .add("thing", "a")
                                .add("thing", "b")
                                .add("server", "c")
                                .build()
                ),
                Arguments.of(new String[]{"="}, ImmutableContextSetImpl.EMPTY)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testParseContext(String[] arguments, ContextSet expected) {
        ArgumentList list = new ArgumentList(ImmutableList.copyOf(arguments));
        assertEquals(expected, list.getContextOrEmpty(0));
    }


}
