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

package me.lucko.luckperms.common.filter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ComparisonTest {

    @ParameterizedTest(name = "[{index}] {0} {1}")
    @CsvSource({
            "foo, foo, true",
            "foo, Foo, true",
            "Foo, foo, true",
            "foo, bar, false",
            "foo, '', false",
            "'', foo, false",
    })
    public void testEquals(String expression, String test, boolean expected) {
        assertEquals(expected, ConstraintFactory.STRINGS.build(Comparison.EQUAL, expression).evaluate(test));
        assertEquals(expected, ConstraintFactory.STRINGS.build(Comparison.EQUAL, test).evaluate(expression));
        assertEquals(!expected, ConstraintFactory.STRINGS.build(Comparison.NOT_EQUAL, expression).evaluate(test));
        assertEquals(!expected, ConstraintFactory.STRINGS.build(Comparison.NOT_EQUAL, test).evaluate(expression));
    }

    @ParameterizedTest(name = "[{index}] {0} {1}")
    @CsvSource({
            "foo, foo, true",
            "foo, Foo, true",
            "Foo, foo, true",
            "foo, bar, false",
            "foo, '', false",
            "'', foo, false",

            "foo%, foobar, true",
            "foo%, Foobar, true",
            "foo%, foo, true",
            "%bar%, bar, true",
            "%bar%, foobar, true",
            "%bar%, barbaz, true",
            "%bar%, foobarbaz, true",

            "_ar, bar, true",
            "_ar, far, true",
            "_ar, BAR, true",
            "_ar, FAR, true",
            "_ar, ar, false",
            "_ar, bbar, false",
    })
    public void testSimilar(String expression, String test, boolean expected) {
        assertEquals(expected, ConstraintFactory.STRINGS.build(Comparison.SIMILAR, expression).evaluate(test));
        assertEquals(!expected, ConstraintFactory.STRINGS.build(Comparison.NOT_SIMILAR, expression).evaluate(test));
    }

}

