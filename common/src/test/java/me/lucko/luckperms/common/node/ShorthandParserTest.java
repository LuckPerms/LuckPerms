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

package me.lucko.luckperms.common.node;

import com.google.common.collect.ImmutableSet;
import me.lucko.luckperms.common.node.utils.ShorthandParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShorthandParserTest {

    private static Stream<Arguments> testParse() {
        return Stream.of(
                // numeric range
                Arguments.of("{2-4}", new String[]{"2", "3", "4"}),
                Arguments.of("{4-2}", new String[]{"2", "3", "4"}),

                // character range
                Arguments.of("{a-d}", new String[]{"a", "b", "c", "d"}),
                Arguments.of("{A-D}", new String[]{"A", "B", "C", "D"}),
                Arguments.of("{D-A}", new String[]{"A", "B", "C", "D"}),

                // list
                Arguments.of("{aa,bb,cc}", new String[]{"aa", "bb", "cc"}),
                Arguments.of("{aa|bb|cc}", new String[]{"aa", "bb", "cc"}),
                Arguments.of("{aa,bb|cc}", new String[]{"aa", "bb", "cc"}),

                // groups
                Arguments.of("he{y|llo} {1-2}", new String[]{"hey 1", "hey 2", "hello 1", "hello 2"}),
                Arguments.of("my.permission.{test,hi}", new String[]{"my.permission.test", "my.permission.hi"}),
                Arguments.of("my.permission.{a-c}", new String[]{"my.permission.a", "my.permission.b", "my.permission.c"}),

                // groups - using () instead
                Arguments.of("he(y|llo) (1-2)", new String[]{"hey 1", "hey 2", "hello 1", "hello 2"}),
                Arguments.of("my.permission.(test,hi)", new String[]{"my.permission.test", "my.permission.hi"}),
                Arguments.of("my.permission.(a-c)", new String[]{"my.permission.a", "my.permission.b", "my.permission.c"})
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testParse(String shorthand, String[] expected) {
        Assertions.assertEquals(ImmutableSet.copyOf(expected), ShorthandParser.expandShorthand(shorthand));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{1-1000}",
            "{1000-1}",
            "{!-က}",
            "{က-!}",
    })
    public void testTooManyElements(String shorthand) {
        assertTrue(ShorthandParser.expandShorthand(shorthand).size() <= 1);
    }

}
