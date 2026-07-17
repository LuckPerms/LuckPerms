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

import me.lucko.luckperms.common.node.types.Permission;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.types.PermissionNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NodeEqualityTest {

    private static Node node(String permission, boolean value, Instant expiry, String contextKey, String contextValue) {
        PermissionNode.Builder builder = Permission.builder().permission(permission).value(value);
        if (expiry != null) {
            builder.expiry(expiry);
        }
        if (contextKey != null) {
            builder.withContext(contextKey, contextValue);
        }
        return builder.build();
    }

    @Test
    public void testExact() {
        Instant expiry = Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.SECONDS);

        Node base = node("hello.world", true, expiry, "server", "one");
        Node same = node("hello.world", true, expiry, "server", "one");
        Node differentKey = node("hello.other", true, expiry, "server", "one");
        Node differentValue = node("hello.world", false, expiry, "server", "one");
        Node differentExpiry = node("hello.world", true, expiry.plusSeconds(60), "server", "one");
        Node differentContext = node("hello.world", true, expiry, "server", "two");

        NodeEqualityPredicate predicate = NodeEqualityPredicate.EXACT;
        assertTrue(predicate.areEqual(base, same));
        assertFalse(predicate.areEqual(base, differentKey));
        assertFalse(predicate.areEqual(base, differentValue));
        assertFalse(predicate.areEqual(base, differentExpiry));
        assertFalse(predicate.areEqual(base, differentContext));
    }

    @Test
    public void testIgnoreValue() {
        Instant expiry = Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.SECONDS);

        Node base = node("hello.world", true, expiry, "server", "one");
        Node differentValue = node("hello.world", false, expiry, "server", "one");
        Node differentKey = node("hello.other", true, expiry, "server", "one");
        Node differentExpiry = node("hello.world", true, expiry.plusSeconds(60), "server", "one");
        Node differentContext = node("hello.world", true, expiry, "server", "two");

        NodeEqualityPredicate predicate = NodeEqualityPredicate.IGNORE_VALUE;
        // value is ignored
        assertTrue(predicate.areEqual(base, differentValue)); 
        assertFalse(predicate.areEqual(base, differentKey));
        assertFalse(predicate.areEqual(base, differentExpiry));
        assertFalse(predicate.areEqual(base, differentContext));
    }

    @Test
    public void testIgnoreExpiryTime() {
        Instant expiryA = Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.SECONDS);
        Instant expiryB = Instant.now().plusSeconds(120).truncatedTo(ChronoUnit.SECONDS);

        Node base = node("hello.world", true, expiryA, "server", "one");
        Node differentExpiryTime = node("hello.world", true, expiryB, "server", "one");
        Node permanent = node("hello.world", true, null, "server", "one");
        Node differentValue = node("hello.world", false, expiryA, "server", "one");
        Node differentContext = node("hello.world", true, expiryA, "server", "two");

        NodeEqualityPredicate predicate = NodeEqualityPredicate.IGNORE_EXPIRY_TIME;
        // exact expiry time is ignored, but whether it has an expiry still matters
        assertTrue(predicate.areEqual(base, differentExpiryTime)); 
        assertFalse(predicate.areEqual(base, permanent));
        assertFalse(predicate.areEqual(base, differentValue));
        assertFalse(predicate.areEqual(base, differentContext));
    }

    @Test
    public void testIgnoreExpiryTimeAndValue() {
        Instant expiryA = Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.SECONDS);
        Instant expiryB = Instant.now().plusSeconds(120).truncatedTo(ChronoUnit.SECONDS);

        Node base = node("hello.world", true, expiryA, "server", "one");
        Node differentValue = node("hello.world", false, expiryA, "server", "one");
        Node differentExpiryTime = node("hello.world", true, expiryB, "server", "one");
        Node permanent = node("hello.world", true, null, "server", "one");
        Node differentContext = node("hello.world", true, expiryA, "server", "two");

        NodeEqualityPredicate predicate = NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE;
        // value and exact expiry time are ignored, but whether it has an expiry still matters
        assertTrue(predicate.areEqual(base, differentValue));
        assertTrue(predicate.areEqual(base, differentExpiryTime));
        assertFalse(predicate.areEqual(base, permanent));
        assertFalse(predicate.areEqual(base, differentContext));
    }

    @Test
    public void testIgnoreValueOrIfTemporary() {
        Instant expiry = Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.SECONDS);

        Node base = node("hello.world", true, expiry, "server", "one");
        Node differentValue = node("hello.world", false, expiry, "server", "one");
        Node permanent = node("hello.world", true, null, "server", "one");
        Node differentContext = node("hello.world", true, expiry, "server", "two");
        Node differentKey = node("hello.other", true, expiry, "server", "one");

        NodeEqualityPredicate predicate = NodeEqualityPredicate.IGNORE_VALUE_OR_IF_TEMPORARY;
        // value and expiry (whether temporary or not) are entirely ignored
        assertTrue(predicate.areEqual(base, differentValue));
        assertTrue(predicate.areEqual(base, permanent));
        assertFalse(predicate.areEqual(base, differentContext));
        assertFalse(predicate.areEqual(base, differentKey));
    }

    @Test
    public void testOnlyKey() {
        Node base = node("hello.world", true, null, "server", "one");
        Node differentValue = node("hello.world", false, null, "server", "two");
        Node differentKey = node("hello.other", true, null, "server", "one");

        NodeEqualityPredicate predicate = NodeEqualityPredicate.ONLY_KEY;
        assertTrue(predicate.areEqual(base, differentValue));
        assertFalse(predicate.areEqual(base, differentKey));
    }

    private static Stream<Arguments> testSameInstanceAlwaysEqual() {
        return Stream.of(
                Arguments.of(NodeEqualityPredicate.EXACT),
                Arguments.of(NodeEqualityPredicate.ONLY_KEY),
                Arguments.of(NodeEqualityPredicate.IGNORE_VALUE),
                Arguments.of(NodeEqualityPredicate.IGNORE_EXPIRY_TIME),
                Arguments.of(NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE),
                Arguments.of(NodeEqualityPredicate.IGNORE_VALUE_OR_IF_TEMPORARY)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testSameInstanceAlwaysEqual(NodeEqualityPredicate predicate) {
        Instant expiry = Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.SECONDS);
        Node node = node("hello.world", true, expiry, "server", "one");
        
        assertTrue(predicate.areEqual(node, node));
    }

    @ParameterizedTest
    @EnumSource
    public void testComparesContexts(NodeEquality nodeEquality) {
        if (nodeEquality.name().contains("CONTEXTS")) {
            assertTrue(nodeEquality.comparesContexts());
        } else {
            assertFalse(nodeEquality.comparesContexts());
        }
    }

    private static Stream<Arguments> testMappingToNodeEquality() {
        return Stream.of(
                Arguments.of(NodeEqualityPredicate.EXACT, NodeEquality.KEY_VALUE_EXPIRY_CONTEXTS),
                Arguments.of(NodeEqualityPredicate.IGNORE_VALUE, NodeEquality.KEY_EXPIRY_CONTEXTS),
                Arguments.of(NodeEqualityPredicate.IGNORE_EXPIRY_TIME, NodeEquality.KEY_VALUE_HASEXPIRY_CONTEXTS),
                Arguments.of(NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE, NodeEquality.KEY_HASEXPIRY_CONTEXTS),
                Arguments.of(NodeEqualityPredicate.IGNORE_VALUE_OR_IF_TEMPORARY, NodeEquality.KEY_CONTEXTS),
                Arguments.of(NodeEqualityPredicate.ONLY_KEY, NodeEquality.KEY)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testMappingToNodeEquality(NodeEqualityPredicate predicate, NodeEquality expected) {
        assertEquals(expected, NodeEquality.of(predicate));
    }

    @Test
    public void testMappingToNodeEqualityNull() {
        assertNull(NodeEquality.of((o1, o2) -> true));
    }

    @Test
    public void testComparesContextsHelper() {
        assertTrue(NodeEquality.comparesContexts(NodeEqualityPredicate.EXACT));
        assertFalse(NodeEquality.comparesContexts(NodeEqualityPredicate.ONLY_KEY));
        // an unrecognised predicate maps to null, and should be reported as not comparing contexts
        assertFalse(NodeEquality.comparesContexts((o1, o2) -> true));
    }

}
