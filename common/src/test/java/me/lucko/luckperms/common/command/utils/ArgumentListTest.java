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
import me.lucko.luckperms.common.util.SignedDuration;
import net.luckperms.api.context.ContextSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    private static Stream<Arguments> testParseSignedDuration() {
        return Stream.of(
                Arguments.of("1h", SignedDuration.Sign.ABSOLUTE, Duration.ofHours(1)),
                Arguments.of("90m", SignedDuration.Sign.ABSOLUTE, Duration.ofMinutes(90)),
                Arguments.of("+1h", SignedDuration.Sign.ADD, Duration.ofHours(1)),
                Arguments.of("-1h", SignedDuration.Sign.SUBTRACT, Duration.ofHours(1)),
                Arguments.of("+1h30m", SignedDuration.Sign.ADD, Duration.ofMinutes(90)),
                Arguments.of("-10s", SignedDuration.Sign.SUBTRACT, Duration.ofSeconds(10))
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testParseSignedDuration(String argument, SignedDuration.Sign expectedSign, Duration expectedDuration) throws ArgumentException {
        ArgumentList list = new ArgumentList(ImmutableList.of(argument));
        SignedDuration parsed = list.getSignedDuration(0);

        assertEquals(expectedSign, parsed.sign());
        assertEquals(expectedDuration, parsed.duration());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "+4102444800", "-4102444800", // a sign only makes sense on a relative duration,
            "+1700000000", "-1700000000", // never on a unix timestamp
            "+wat", "-wat",               // nor on something that isn't a duration at all
            "+", "-"                      // nor on nothing, which would otherwise read as zero
    })
    public void testParseSignedDurationRejectsMalformed(String argument) {
        ArgumentList list = new ArgumentList(ImmutableList.of(argument));

        assertThrows(ArgumentException.InvalidDate.class, () -> list.getSignedDuration(0));

        // the optional form must reject these too - falling back to the default would let
        // a mistyped duration be silently swallowed by the context parser
        assertThrows(ArgumentException.InvalidDate.class, () -> list.getSignedDurationOrDefault(0, null));
    }

    @Test
    public void testParseSignedDurationDefaults() throws ArgumentException {
        ArgumentList list = new ArgumentList(ImmutableList.of("server=test"));

        // arguments that aren't durations at all fall back to the default, so that
        // callers can treat them as the start of the context instead
        assertNull(list.getSignedDurationOrDefault(0, null));
        assertNull(list.getSignedDurationOrDefault(1, null));
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
