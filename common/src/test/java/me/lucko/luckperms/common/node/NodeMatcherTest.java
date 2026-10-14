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
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.node.types.Permission;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.Weight;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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

    @ParameterizedTest
    @CsvSource({
            "foo, true",
            "bar, false",
            "FOO, true", // case-insensitive
    })
    public void testKeyMatch(String permission, boolean expectedMatch) {
        ConstraintNodeMatcher<Node> matcher = StandardNodeMatchers.key("foo");
        Node node = Permission.builder().permission(permission).build();

        assertEquals(expectedMatch, matcher.test(node));
        if (expectedMatch) {
            assertSame(node, matcher.match(node));
        } else {
            assertNull(matcher.match(node));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "foo.bar, true",
            "foo.baz, true",
            "foo, false",
            "bar.foo, false",
    })
    public void testKeyStartsWithMatch(String permission, boolean expectedMatch) {
        ConstraintNodeMatcher<Node> matcher = StandardNodeMatchers.keyStartsWith("foo.");
        Node node = Permission.builder().permission(permission).build();
        assertEquals(expectedMatch, matcher.test(node));
    }

    private static Stream<Arguments> testNodeEqualsMatch() {
        return Stream.of(
                // same key & value -> matches
                Arguments.of(
                        Permission.builder().permission("foo").value(true).build(),
                        Permission.builder().permission("foo").value(true).build(),
                        NodeEqualityPredicate.EXACT,
                        true
                ),
                // different value, EXACT equality -> doesn't match
                Arguments.of(
                        Permission.builder().permission("foo").value(true).build(),
                        Permission.builder().permission("foo").value(false).build(),
                        NodeEqualityPredicate.EXACT,
                        false
                ),
                // different value, but IGNORE_VALUE equality -> matches
                Arguments.of(
                        Permission.builder().permission("foo").value(true).build(),
                        Permission.builder().permission("foo").value(false).build(),
                        NodeEqualityPredicate.IGNORE_VALUE,
                        true
                ),
                // different key -> never matches, regardless of equality predicate
                Arguments.of(
                        Permission.builder().permission("foo").build(),
                        Permission.builder().permission("bar").build(),
                        NodeEqualityPredicate.IGNORE_VALUE,
                        false
                ),
                // ONLY_KEY equality -> value/context/expiry differences are ignored
                Arguments.of(
                        Permission.builder().permission("foo").value(true).withContext("server", "foo").build(),
                        Permission.builder().permission("foo").value(false).build(),
                        NodeEqualityPredicate.ONLY_KEY,
                        true
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testNodeEqualsMatch(Node matcherNode, Node candidateNode, NodeEqualityPredicate equalityPredicate, boolean expectedMatch) {
        ConstraintNodeMatcher<Node> matcher = StandardNodeMatchers.equals(matcherNode, equalityPredicate);

        assertEquals(expectedMatch, matcher.test(candidateNode));
        assertEquals(expectedMatch, matcher.match(candidateNode) != null);
    }

    @Test
    public void testNodeEqualsMatchFailsConstraintBeforeReachingFilter() {
        // the key comparison constraint is checked before filterConstraintMatch is invoked
        // if the keys don't match, filterConstraintMatch should never even be considered
        ConstraintNodeMatcher<Node> matcher = StandardNodeMatchers.equals(
                Permission.builder().permission("foo").build(),
                (o1, o2) -> {
                    throw new RuntimeException("should never be called");
                }
        );

        Node differentKeyNode = Permission.builder().permission("bar").build();
        assertNull(matcher.match(differentKeyNode));
        assertFalse(matcher.test(differentKeyNode));
    }

    @ParameterizedTest
    @CsvSource({
            "foo, true",
            "bar, false",
    })
    public void testMetaKeyMatch(String metaKey, boolean expectedMatch) {
        ConstraintNodeMatcher<MetaNode> matcher = StandardNodeMatchers.metaKey("foo");
        Node node = Meta.builder(metaKey, "100").build();

        assertEquals(expectedMatch, matcher.test(node));

        MetaNode result = matcher.match(node);
        if (expectedMatch) {
            assertNotNull(result);
            assertSame(node, result);
            assertEquals(metaKey, result.getMetaKey());
        } else {
            assertNull(result);
        }
    }

    private static Stream<Arguments> testTypeMatch() {
        return Stream.of(
                Arguments.of(NodeType.INHERITANCE, Inheritance.builder("admin").build(), true),
                Arguments.of(NodeType.INHERITANCE, Permission.builder().permission("foo").build(), false),
                Arguments.of(NodeType.META, Meta.builder("foo", "bar").build(), true),
                Arguments.of(NodeType.META, Prefix.builder("foo", 100).build(), false),
                Arguments.of(NodeType.PREFIX, Prefix.builder("foo", 100).build(), true),
                Arguments.of(NodeType.PREFIX, Meta.builder("foo", "bar").build(), false),
                Arguments.of(NodeType.WEIGHT, Weight.builder(100).build(), true),
                Arguments.of(NodeType.WEIGHT, Permission.builder().permission("some.permission").build(), false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testTypeMatch(NodeType<?> type, Node node, boolean expectedMatch) {
        ConstraintNodeMatcher<?> matcher = StandardNodeMatchers.type(type);

        assertEquals(expectedMatch, matcher.test(node));
        assertEquals(expectedMatch, matcher.match(node) != null);
    }

    @Test
    public void testTypeMatchCastsToRequestedType() {
        ConstraintNodeMatcher<InheritanceNode> matcher = StandardNodeMatchers.type(NodeType.INHERITANCE);
        Node node = Inheritance.builder("admin").build();

        InheritanceNode result = matcher.match(node);
        assertNotNull(result);
        assertSame(node, result);
    }
}
