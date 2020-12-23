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

package me.lucko.luckperms.common.node.utils;

import com.google.common.collect.ImmutableSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShorthandParserTest {

    private static void test(String shorthand, String... expected) {
        assertEquals(ImmutableSet.copyOf(expected), ShorthandParser.expandShorthand(shorthand));
    }

    @Test
    void testNumericRange() {
        test("{2-4}", "2", "3", "4");
    }

    @Test
    void testCharacterRange() {
        test("{a-d}", "a", "b", "c", "d");
        test("{A-D}", "A", "B", "C", "D");
    }

    @Test
    void testList() {
        test("{aa,bb,cc}", "aa", "bb", "cc");
        test("{aa|bb|cc}", "aa", "bb", "cc");
        test("{aa,bb|cc}", "aa", "bb", "cc");
    }

    @Test
    void testGroups() {
        test("he{y|llo} {1-2}", "hey 1", "hey 2", "hello 1", "hello 2");
        test("my.permission.{test,hi}", "my.permission.test", "my.permission.hi");
        test("my.permission.{a-c}", "my.permission.a", "my.permission.b", "my.permission.c");
        
        // use ( ) instead
        test("he(y|llo) (1-2)", "hey 1", "hey 2", "hello 1", "hello 2");
        test("my.permission.(test,hi)", "my.permission.test", "my.permission.hi");
        test("my.permission.(a-c)", "my.permission.a", "my.permission.b", "my.permission.c");
    }

}
