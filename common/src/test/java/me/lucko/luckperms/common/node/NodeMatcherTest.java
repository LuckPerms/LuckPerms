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

import me.lucko.luckperms.common.filter.Comparison;
import me.lucko.luckperms.common.filter.Constraint;
import me.lucko.luckperms.common.node.matcher.ConstraintNodeMatcher;
import me.lucko.luckperms.common.node.matcher.StandardNodeMatchers;
import me.lucko.luckperms.common.node.types.Permission;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NodeMatcherTest {

    @Test
    public void testKey() {
        ConstraintNodeMatcher<Node> matcher = StandardNodeMatchers.key("foo");
        Constraint<String> constraint = matcher.getConstraint();

        assertEquals(Comparison.EQUAL, constraint.comparison());
        assertEquals("foo", constraint.value());

        ConstraintNodeMatcher<Permission> typedMatcher = StandardNodeMatchers.key(Permission.builder().permission("foo").build());
        Constraint<String> typedConstraint = typedMatcher.getConstraint();

        assertEquals(Comparison.EQUAL, typedConstraint.comparison());
        assertEquals("foo", typedConstraint.value());
    }

    @ParameterizedTest
    @CsvSource({
            "foo, foo%",
    })
    public void testKeyStartsWith(String value, String expectedConstraint) {
        ConstraintNodeMatcher<Node> matcher = StandardNodeMatchers.keyStartsWith(value);
        Constraint<String> constraint = matcher.getConstraint();

        assertEquals(Comparison.SIMILAR, constraint.comparison());
        assertEquals(expectedConstraint, constraint.value());
    }

    @ParameterizedTest
    @CsvSource({
            "foo, meta.foo.%",
    })
    public void testMetaKey(String value, String expectedConstraint) {
        ConstraintNodeMatcher<MetaNode> matcher = StandardNodeMatchers.metaKey(value);
        Constraint<String> constraint = matcher.getConstraint();

        assertEquals(Comparison.SIMILAR, constraint.comparison());
        assertEquals(expectedConstraint, constraint.value());
    }


    private static Stream<Arguments> testType() {
        return Stream.of(
                Arguments.of(NodeType.REGEX_PERMISSION, "r=%"),
                Arguments.of(NodeType.INHERITANCE, "group.%"),
                Arguments.of(NodeType.PREFIX, "prefix.%.%"),
                Arguments.of(NodeType.SUFFIX, "suffix.%.%"),
                Arguments.of(NodeType.META, "meta.%.%"),
                Arguments.of(NodeType.WEIGHT, "weight.%"),
                Arguments.of(NodeType.DISPLAY_NAME, "displayname.%")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testType(NodeType<?> type, String expectedValue) {
        ConstraintNodeMatcher<?> matcher = StandardNodeMatchers.type(type);
        Constraint<String> constraint = matcher.getConstraint();

        assertEquals(Comparison.SIMILAR, constraint.comparison());
        assertEquals(expectedValue, constraint.value());

    }

}
